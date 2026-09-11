package fr.anisekai.sanctum.resolvers;

import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.resolvers.ResolverPolicy;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.sanctum.interfaces.resolvers.StorageWalker;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Implementation of {@link StorageResolver} allowing to simply resolve directories and files within a directory for a specific
 * {@link FileStore}.
 */
public final class StandardResolver implements StorageResolver {

    private final Path           root;
    private final FileStore      store;
    private final ResolverPolicy resolverPolicy;
    private final StorageWalker  walker;

    /**
     * Create a new {@link StandardResolver} instance.
     *
     * @param root
     *         The root {@link Path} from which the {@link StorageResolver} will resolve other {@link Path}.
     * @param store
     *         The {@link FileStore} associated to this {@link StorageResolver}.
     * @param resolverPolicy
     *         The {@link ResolverPolicy} to use before resolving any {@link Path}.
     */
    public StandardResolver(Path root, FileStore store, ResolverPolicy resolverPolicy) {

        this.root           = root;
        this.store          = store;
        this.resolverPolicy = resolverPolicy;
        this.walker         = new StandardWalker(this.root);
    }

    @Override
    public Path directory() {

        return this.root;
    }

    @Override
    public Path directory(String name) {

        this.resolverPolicy.checkResolveDirectory(name);
        return this.walker.directory(name);
    }

    @Override
    public Path file(String filename) {

        this.resolverPolicy.checkResolveFile(filename);
        String resolvedName = this.store.type() == StoreType.FILE_SCOPED ?
                String.format("%s.%s", filename, this.store.extension()) :
                filename;
        return this.walker.file(resolvedName);
    }

    @Override
    public Path file(String directory, String filename) {

        this.resolverPolicy.checkResolveFile(directory, filename);
        return this.walker.walk(directory).file(filename);
    }

    @Override
    public Path directory(ScopedEntity entity) {

        return this.directory(entity == null ? null : entity.getScopedName());
    }

    @Override
    public Path file(ScopedEntity entity) {

        return this.file(entity == null ? null : entity.getScopedName());
    }

    @Override
    public Path file(ScopedEntity entity, String filename) {

        return this.file(entity == null ? null : entity.getScopedName(), filename);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StandardResolver) obj;
        return Objects.equals(this.root, that.root) &&
                Objects.equals(this.resolverPolicy, that.resolverPolicy);
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.root, this.resolverPolicy);
    }

    @Override
    public String toString() {

        return "StandardResolver[root=" + this.root + ", resolverPolicy=" + this.resolverPolicy + ']';
    }


}
