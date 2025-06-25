package fr.anisekai.sanctum;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link Library}.
 */
public class Sanctum implements Library {

    private static final FileStore STORE_TEMPORARY = new RawStorage("tmp");
    private static final FileStore STORE_ISOLATION = new ScopedDirectoryStorage("isolation", IsolationSession.class);

    private final Path                                    root;
    private final StorageWalker                           walker;
    private final Map<String, IsolationSessionDescriptor> isolatedStorages = new HashMap<>();
    private final Map<FileStore, StorePolicy>             stores           = new HashMap<>();

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

    private String randomName() {

        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private void checkScopes(Iterable<AccessScope> scopes) {

        Map<AccessScope, String> scopeClaimMap = new HashMap<>();
        for (IsolationSessionDescriptor storage : this.isolatedStorages.values()) {
            for (AccessScope scope : storage.scopes()) {
                scopeClaimMap.put(scope, storage.name());
            }
        }

        for (AccessScope scope : scopes) {
            if (scopeClaimMap.containsKey(scope)) {
                String claimedBy = scopeClaimMap.get(scope);
                throw new ScopeGrantException(String.format(
                        "Cannot grant %s: The scope is already claimed by the isolated context '%s'",
                        scope,
                        claimedBy
                ));
            }

            if (!this.hasStore(scope.store())) {
                throw new ScopeGrantException(String.format(
                        "Cannot grant %s: The store targeted is not registered in this library.",
                        scope
                ));
            }
        }
    }


    private IsolationSessionDescriptor getIsolatedStorage(IsolationSession context, boolean allowCommitted) {

        String contextIdentifier = context.name();

        if (!this.isolatedStorages.containsKey(contextIdentifier)) {
            throw new ContextUnavailableException(String.format(
                    "The '%s' isolated storage has probably already been discarded.",
                    contextIdentifier
            ));
        }

        IsolationSessionDescriptor storage = this.isolatedStorages.get(contextIdentifier);

        if (storage.isCommitted() && !allowCommitted) {
            throw new ContextUnavailableException(String.format(
                    "The '%s' isolated storage has already been committed.",
                    storage.name()
            ));
        }

        return storage;
    }

    @Override
    public Path requestTemporaryFile(IsolationSession context, String extension) {

        StorageResolver resolver = this.getResolver(context, STORE_TEMPORARY);
        return resolver.file(String.format("%s.%s", this.randomName(), extension));
    }

    @Override
    public void registerStore(FileStore store, StorePolicy policy) {

        if (this.hasStore(store)) {
            throw new StoreRegistrationException(String.format("Store '%s' already exists", store.name()));
        }

        // Deny policies that can be committed with unscoped stores.
        if (!store.type().isScoped() && policy.willModifyFilesystem()) {
            throw new StoreRegistrationException(String.format(
                    "The '%s' unscoped store cannot be registered under the '%s' policy.",
                    store.name(),
                    policy.name()
            ));
        }

        try {
            Path path = this.walker.directory(store.name());
            if (!Files.exists(path)) {
                SanctumUtils.Action.wrap(() -> Files.createDirectories(path), StorageException::new);
            }
        } catch (Exception e) {
            throw new StoreRegistrationException(
                    String.format("Store '%s' root directory could not be obtained", store.name()),
                    e
            );
        }

        this.stores.put(store, policy);
    }

    @Override
    public boolean hasStore(FileStore store) {

        return this.stores.containsKey(store);
    }

    @Override
    public IsolationSession createIsolation(Set<AccessScope> scopes) {

        this.checkScopes(scopes);

        String                     name          = this.randomName();
        Path                       isolationRoot = this.walker.walk(STORE_ISOLATION.name()).directory(name);
        IsolationSession           context       = new IsolationSessionImpl(this, isolationRoot, name);
        IsolationSessionDescriptor storage       = new IsolationSessionDescriptorImpl(name, context);

        scopes.forEach(storage::grantScope);

        if (!Files.exists(isolationRoot)) {
            SanctumUtils.Action.wrap(() -> Files.createDirectories(isolationRoot), StorageException::new);
        }

        this.isolatedStorages.put(name, storage);
        return context;
    }

    @Override
    public StorageResolver getResolver(IsolationSession context, FileStore store) {

        if (!this.hasStore(store)) {
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

        IsolationSessionDescriptor storage = this.getIsolatedStorage(context, false);

        ResolverPolicy resolverPolicy = ResolverPolicy.chained(
                new IsolationResolverPolicy(storage, store),
                new StoreResolverPolicy(store)
        );

        Path root = this.walker
                .walk(STORE_ISOLATION.name())
                .walk(storage.name())
                .directory(store.name());

        return new StandardResolver(root, store, resolverPolicy);
    }

    @Override
    public void requestScope(IsolationSession context, Set<AccessScope> scopes) {

        IsolationSessionDescriptor storage = this.getIsolatedStorage(context, false);
        this.checkScopes(scopes);
        scopes.forEach(storage::grantScope);
    }

    @Override
    public void commit(IsolationSession context) {

        IsolationSessionDescriptor storage = this.getIsolatedStorage(context, false);

        for (AccessScope scope : storage.scopes()) {
            try {
                this.commitScope(storage, scope);
            } catch (IOException e) {
                throw new ContextCommitException(String.format("Failed to commit scope '%s'.", scope), e);
            }
        }

        storage.setCommitted(true);
    }

    /**
     * Commits the contents of the given {@link IsolationSessionDescriptor} under the {@link AccessScope} to the library, applying
     * the corresponding {@link FileStore} policies defined in this {@link Sanctum}.
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

        boolean hasBackup = false;

        SanctumUtils.delete(safeLocalPath);

        if (Files.exists(localPath)) {
            SanctumUtils.copy(localPath, safeLocalPath);
            hasBackup = true;
        }

        try {
            if (store.type() == StoreType.DIRECTORY_SCOPED && policy == StorePolicy.FULL_SWAP) {
                SanctumUtils.delete(localPath);
                SanctumUtils.copy(isolationPath, localPath, StandardCopyOption.COPY_ATTRIBUTES);
            } else if (store.type() == StoreType.DIRECTORY_SCOPED && policy == StorePolicy.OVERWRITE) {
                SanctumUtils.copy(
                        isolationPath,
                        localPath,
                        StandardCopyOption.COPY_ATTRIBUTES,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } else if (store.type() == StoreType.FILE_SCOPED) {
                if (policy == StorePolicy.FULL_SWAP) {
                    SanctumUtils.delete(localPath);
                }
                if (Files.isRegularFile(isolationPath)) {
                    SanctumUtils.copy(
                            isolationPath,
                            localPath,
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        } catch (Exception e) {
            // Avoid partial commit
            SanctumUtils.delete(localPath);
            if (hasBackup) Files.move(safeLocalPath, localPath);
        } finally {
            if (hasBackup) SanctumUtils.delete(safeLocalPath);
        }
    }

    @Override
    public void discard(IsolationSession context) {

        IsolationSessionDescriptor storage = this.getIsolatedStorage(context, true);
        this.isolatedStorages.remove(storage.name());

        Path isolationRoot = this.walker.walk(STORE_ISOLATION.name()).directory(storage.name());

        try {
            // Remove recursively the isolated context. At that point even if it fails, we already dropped
            // the scopes claims, making the isolation context unusable so it does not matter anymore.
            SanctumUtils.delete(isolationRoot);
        } catch (IOException e) {
            throw new ContextDiscardException(String.format("Failed to discard store '%s'.", isolationRoot), e);
        }
    }

    @Override
    public void close() throws Exception {

        this.isolatedStorages.clear();
        Path isolationRoot = this.walker.directory(STORE_ISOLATION.name());
        SanctumUtils.delete(isolationRoot);
    }

    @Override
    public StorageResolver getResolver(FileStore store) {

        if (!this.hasStore(store)) {
            throw new StorageException(String.format(
                    "Store '%s' is not registered in this library",
                    store.name()
            ));
        }

        ResolverPolicy resolverPolicy = new StoreResolverPolicy(store);

        Path root = this.walker.directory(store.name());
        return new StandardResolver(root, store, resolverPolicy);
    }

}
