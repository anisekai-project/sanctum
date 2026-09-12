package fr.anisekai.sanctum;

import com.github.f4b6a3.uuid.UuidCreator;
import fr.anisekai.sanctum.enums.StorePolicy;
import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.exceptions.LibraryException;
import fr.anisekai.sanctum.exceptions.StorageException;
import fr.anisekai.sanctum.exceptions.StoreRegistrationException;
import fr.anisekai.sanctum.exceptions.context.ContextCommitException;
import fr.anisekai.sanctum.exceptions.context.ContextDiscardException;
import fr.anisekai.sanctum.exceptions.context.ContextUnavailableException;
import fr.anisekai.sanctum.exceptions.scope.ScopeGrantException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.Library;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;
import fr.anisekai.sanctum.interfaces.resolvers.ResolverPolicy;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.sanctum.interfaces.resolvers.StorageWalker;
import fr.anisekai.sanctum.resolvers.StandardResolver;
import fr.anisekai.sanctum.resolvers.StandardWalker;
import fr.anisekai.sanctum.resolvers.policies.IsolationResolverPolicy;
import fr.anisekai.sanctum.resolvers.policies.StoreResolverPolicy;
import fr.anisekai.sanctum.stores.RawStorage;
import fr.anisekai.sanctum.stores.ScopedDirectoryStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of {@link Library}.
 * <p>
 * Instances are safe to use from multiple threads. Operations on different isolation sessions may run concurrently,
 * while lifecycle operations on the same session are serialized. A scope can only be claimed by one active isolation
 * session. Paths returned by a resolver remain regular filesystem paths; callers are responsible for coordinating
 * direct filesystem access performed outside this library.
 */
public class Sanctum implements Library {

    private static final FileStore STORE_TEMPORARY = new RawStorage("tmp");
    private static final FileStore STORE_ISOLATION = new ScopedDirectoryStorage("isolation", IsolationSession.class);

    private final    Path                                            root;
    private final    StorageWalker                                   walker;
    private final    ConcurrentMap<UUID, IsolationSessionDescriptor> isolatedStorages = new ConcurrentHashMap<>();
    private final    ConcurrentMap<FileStore, StorePolicy>           stores           = new ConcurrentHashMap<>();
    private final    ConcurrentMap<Path, FileStore>                  storesByPath     = new ConcurrentHashMap<>();
    private final    ConcurrentMap<AccessScope, UUID>                scopeClaims      = new ConcurrentHashMap<>();
    private final    Lock                                            claimLock        = new ReentrantLock();
    private final    ReentrantReadWriteLock                          lifecycleLock    = new ReentrantReadWriteLock();
    private final    Lock                                            operationLock    = this.lifecycleLock.readLock();
    private final    Lock                                            closeLock        = this.lifecycleLock.writeLock();
    private volatile boolean                                         closed;

    /**
     * Create a new {@link Sanctum} instance
     *
     * @param root
     *         The root {@link Path} of the library.
     */
    public Sanctum(Path root) {

        this.root   = root.toAbsolutePath().normalize();
        this.walker = new StandardWalker(this.root);

        if (!Files.exists(this.root)) {
            SanctumUtils.Action.wrap(() -> Files.createDirectories(this.root), LibraryException::new);
        }

        this.registerStore(STORE_TEMPORARY, StorePolicy.DISCARD);
        this.registerStore(STORE_ISOLATION, StorePolicy.PRIVATE);
    }

    public UUID randomUUID() {

        return UuidCreator.getTimeOrderedEpoch();
    }

    private void checkScopes(Iterable<AccessScope> scopes) {

        for (AccessScope scope : scopes) {
            UUID claimedBy = this.scopeClaims.get(scope);
            if (claimedBy != null) {
                throw new ScopeGrantException(String.format(
                        "Cannot grant %s: The scope is already claimed by the isolated context '%s'",
                        scope,
                        claimedBy
                ));
            }

            if (!this.stores.containsKey(scope.store())) {
                throw new ScopeGrantException(String.format(
                        "Cannot grant %s: The store targeted is not registered in this library.",
                        scope
                ));
            }
        }
    }

    private void ensureOpen() {

        if (this.closed) {
            throw new LibraryException("This library has already been closed.");
        }
    }

    private void reserveScopes(UUID uuid, Collection<AccessScope> scopes) {

        this.checkScopes(scopes);
        scopes.forEach(scope -> this.scopeClaims.put(scope, uuid));
    }

    private void releaseScopes(IsolationSessionDescriptor storage) {

        storage.scopes().forEach(scope -> this.scopeClaims.remove(scope, storage.uuid()));
    }

    public IsolationSessionDescriptor getIsolatedStorage(UUID uuid, boolean allowCommitted) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            return this.findIsolatedStorage(uuid, allowCommitted);
        } finally {
            this.operationLock.unlock();
        }
    }

    private IsolationSessionDescriptor findIsolatedStorage(UUID uuid, boolean allowCommitted) {

        IsolationSessionDescriptor storage = this.isolatedStorages.get(uuid);
        if (storage == null) {
            throw new ContextUnavailableException(String.format(
                    "The '%s' isolated storage has probably already been discarded.",
                    uuid
            ));
        }

        if (storage.isCommitted() && !allowCommitted) {
            throw new ContextUnavailableException(String.format(
                    "The '%s' isolated storage has already been committed.",
                    storage.uuid()
            ));
        }

        return storage;

    }

    @Override
    public Path requestTemporaryFile(IsolationSession context, String extension) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            StorageResolver resolver = this.getResolver(context, STORE_TEMPORARY);
            return resolver.file(String.format("%s.%s", this.randomUUID(), extension));
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public void registerStore(FileStore store, StorePolicy policy) {

        this.operationLock.lock();
        try {
            this.ensureOpen();

            // Deny policies that can be committed with unscoped stores.
            if (!store.type().isScoped() && policy.willModifyFilesystem()) {
                throw new StoreRegistrationException(String.format(
                        "The '%s' unscoped store cannot be registered under the '%s' policy.",
                        store.name(),
                        policy.name()
                ));
            }

            Path path = null;
            try {
                Path resolvedPath = this.walker.directory(store.name());
                path = resolvedPath;
                FileStore existingStore = this.storesByPath.putIfAbsent(resolvedPath, store);
                if (existingStore != null) {
                    if (existingStore.equals(store)) {
                        throw new StoreRegistrationException(String.format("Store '%s' already exists", store.name()));
                    }
                    throw new StoreRegistrationException(String.format(
                            "Store '%s' conflicts with the already registered store '%s' at '%s'",
                            store.name(),
                            existingStore.name(),
                            resolvedPath
                    ));
                }

                if (!Files.exists(resolvedPath)) {
                    SanctumUtils.Action.wrap(() -> Files.createDirectories(resolvedPath), StorageException::new);
                }
            } catch (Exception e) {
                if (path != null) {
                    this.storesByPath.remove(path, store);
                }
                if (e instanceof StoreRegistrationException registrationException) {
                    throw registrationException;
                }
                throw new StoreRegistrationException(
                        String.format("Store '%s' root directory could not be obtained", store.name()),
                        e
                );
            }

            if (this.stores.putIfAbsent(store, policy) != null) {
                this.storesByPath.remove(path, store);
                throw new StoreRegistrationException(String.format("Store '%s' already exists", store.name()));
            }
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public boolean hasStore(FileStore store) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            return this.stores.containsKey(store);
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public IsolationSession createIsolation(Set<AccessScope> scopes) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            Set<AccessScope> requestedScopes = Set.copyOf(scopes);
            UUID             uuid            = this.randomUUID();

            this.claimLock.lock();
            try {
                this.reserveScopes(uuid, requestedScopes);
                try {
                    Path isolationRoot = this.walker.walk(STORE_ISOLATION.name()).directory(uuid.toString());
                    if (!Files.exists(isolationRoot)) {
                        SanctumUtils.Action.wrap(() -> Files.createDirectories(isolationRoot), StorageException::new);
                    }
                    IsolationSession           context = new IsolationSessionImpl(this, isolationRoot, uuid);
                    IsolationSessionDescriptor storage = new IsolationSessionDescriptorImpl(uuid, context);
                    requestedScopes.forEach(storage::grantScope);
                    this.isolatedStorages.put(uuid, storage);
                    return context;
                } catch (RuntimeException e) {
                    requestedScopes.forEach(scope -> this.scopeClaims.remove(scope, uuid));
                    throw e;
                }
            } finally {
                this.claimLock.unlock();
            }
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public StorageResolver getResolver(IsolationSession context, FileStore store) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            if (!this.stores.containsKey(store)) {
                throw new StorageException(String.format(
                        "Store '%s' is not registered in this library",
                        store.name()
                ));
            }

            StorePolicy policy = this.stores.get(store);

            if (policy == StorePolicy.PRIVATE) {
                throw new StorageException(String.format(
                        "Store '%s' cannot be used in a isolation context.",
                        store.name()
                ));
            }

            IsolationSessionDescriptor storage = this.findIsolatedStorage(context.uuid(), false);

            ResolverPolicy resolverPolicy = ResolverPolicy.chained(
                    new IsolationResolverPolicy(storage, store),
                    new StoreResolverPolicy(store)
            );

            Path root = this.walker
                    .walk(STORE_ISOLATION.name())
                    .walk(storage.uuid().toString())
                    .directory(store.name());

            return new StandardResolver(root, store, resolverPolicy);
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public void requestScope(IsolationSession context, Set<AccessScope> scopes) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            IsolationSessionDescriptor storage         = this.findIsolatedStorage(context.uuid(), false);
            Set<AccessScope>           requestedScopes = Set.copyOf(scopes);
            synchronized (storage) {
                this.findIsolatedStorage(context.uuid(), false);
                this.claimLock.lock();
                try {
                    this.reserveScopes(storage.uuid(), requestedScopes);
                    requestedScopes.forEach(storage::grantScope);
                } finally {
                    this.claimLock.unlock();
                }
            }
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public void commit(IsolationSession context) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            IsolationSessionDescriptor storage = this.findIsolatedStorage(context.uuid(), false);
            synchronized (storage) {
                this.findIsolatedStorage(context.uuid(), false);
                for (AccessScope scope : storage.scopes()) {
                    try {
                        this.commitScope(storage, scope);
                    } catch (IOException e) {
                        throw new ContextCommitException(String.format("Failed to commit scope '%s'.", scope), e);
                    }
                }
                storage.setCommitted(true);
            }
        } finally {
            this.operationLock.unlock();
        }
    }

    /**
     * Commits the contents of the given {@link IsolationSessionDescriptor} under the {@link AccessScope} to the
     * library, applying the corresponding {@link FileStore} policies defined in this {@link Sanctum}.
     *
     * @param storage
     *         The {@link IsolationSessionDescriptor} to commit.
     * @param scope
     *         The {@link AccessScope} to commit.
     */
    private void commitScope(IsolationSessionDescriptor storage, AccessScope scope) throws IOException {

        FileStore   store  = scope.store();
        StorePolicy policy = this.stores.get(store);

        if (policy == StorePolicy.DISCARD) return;
        if (!store.type().isScoped()) return;

        StorageWalker storeWalker = this.walker.walk(store.name());

        Path localPath = this.resolve(scope);
        //noinspection resource
        Path isolationPath = storage.context().resolve(scope);

        String safeName = "." + localPath.getFileName().toString();

        Path safeLocalPath = scope.store().type() == StoreType.FILE_SCOPED ?
                storeWalker.file(safeName) :
                storeWalker.directory(safeName);

        if (store.type() == StoreType.DIRECTORY_SCOPED && policy == StorePolicy.FULL_SWAP) {
            this.commitSwap(localPath, isolationPath, safeLocalPath);
        } else if (store.type() == StoreType.DIRECTORY_SCOPED && policy == StorePolicy.OVERWRITE) {
            this.commitDirectoryOverwrite(localPath, isolationPath, safeLocalPath);
        } else if (store.type() == StoreType.FILE_SCOPED) {
            this.commitFileScope(policy, localPath, isolationPath, safeLocalPath);
        }
    }

    private void commitFileScope(StorePolicy policy, Path localPath, Path isolationPath, Path safeLocalPath) throws IOException {

        if (!Files.isRegularFile(isolationPath)) {
            if (policy == StorePolicy.FULL_SWAP) {
                SanctumUtils.delete(localPath);
            }
            return;
        }

        this.commitSwap(localPath, isolationPath, safeLocalPath);
    }

    private void commitDirectoryOverwrite(Path localPath, Path isolationPath, Path safeLocalPath) throws IOException {

        boolean hasBackup = false;
        boolean committed = false;

        SanctumUtils.delete(safeLocalPath);

        if (Files.exists(localPath)) {
            SanctumUtils.copy(localPath, safeLocalPath, StandardCopyOption.COPY_ATTRIBUTES);
            hasBackup = true;
        }

        try {
            SanctumUtils.copy(
                    isolationPath,
                    localPath,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING
            );
            committed = true;
        } catch (Exception e) {
            this.rollbackCommit(localPath, safeLocalPath, hasBackup, e);
        } finally {
            if (committed && hasBackup) {
                SanctumUtils.delete(safeLocalPath);
            }
        }
    }

    private void commitSwap(Path localPath, Path isolationPath, Path safeLocalPath) throws IOException {

        boolean hasBackup = false;
        boolean committed = false;

        SanctumUtils.delete(safeLocalPath);

        try {
            if (Files.exists(localPath)) {
                SanctumUtils.move(localPath, safeLocalPath);
                hasBackup = true;
            }

            SanctumUtils.move(isolationPath, localPath);
            committed = true;
        } catch (Exception e) {
            this.rollbackCommit(localPath, safeLocalPath, hasBackup, e);
        } finally {
            if (committed && hasBackup) {
                SanctumUtils.delete(safeLocalPath);
            }
        }
    }

    private void rollbackCommit(Path localPath, Path safeLocalPath, boolean hasBackup, Exception cause) throws IOException {

        try {
            SanctumUtils.delete(localPath);
            if (hasBackup) {
                SanctumUtils.move(safeLocalPath, localPath);
            }
        } catch (Exception recoveryException) {
            cause.addSuppressed(recoveryException);
        }

        if (cause instanceof IOException ioException) throw ioException;
        throw new IOException("Failed to commit isolated content.", cause);
    }

    @Override
    public void discard(IsolationSession context) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            IsolationSessionDescriptor storage = this.findIsolatedStorage(context.uuid(), true);
            synchronized (storage) {
                this.findIsolatedStorage(context.uuid(), true);
                this.isolatedStorages.remove(storage.uuid(), storage);
                this.claimLock.lock();
                try {
                    this.releaseScopes(storage);
                } finally {
                    this.claimLock.unlock();
                }

                Path isolationRoot = this.walker.walk(STORE_ISOLATION.name()).directory(storage.uuid().toString());
                try {
                    SanctumUtils.delete(isolationRoot);
                } catch (IOException e) {
                    throw new ContextDiscardException(String.format("Failed to discard store '%s'.", isolationRoot), e);
                }
            }
        } finally {
            this.operationLock.unlock();
        }
    }

    @Override
    public void close() throws Exception {

        this.closeLock.lock();
        try {
            if (this.closed) return;
            this.closed = true;
            this.scopeClaims.clear();
            this.isolatedStorages.clear();
            Path isolationRoot = this.walker.directory(STORE_ISOLATION.name());
            SanctumUtils.delete(isolationRoot);
        } finally {
            this.closeLock.unlock();
        }
    }

    @Override
    public StorageResolver getResolver(FileStore store) {

        this.operationLock.lock();
        try {
            this.ensureOpen();
            if (!this.stores.containsKey(store)) {
                throw new StorageException(String.format(
                        "Store '%s' is not registered in this library",
                        store.name()
                ));
            }

            ResolverPolicy resolverPolicy = new StoreResolverPolicy(store);

            Path root = this.walker.directory(store.name());
            return new StandardResolver(root, store, resolverPolicy);
        } finally {
            this.operationLock.unlock();
        }
    }

}
