package fr.anisekai.sanctum.interfaces;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

import java.nio.file.Path;

/**
 * Represents a component capable of resolving storage paths.
 */
public interface StorageAware {

    /**
     * Retrieve a {@link StorageResolver} for the provided {@link FileStore} within this {@link StorageAware} context.
     *
     * @param store
     *         A {@link FileStore}.
     *
     * @return A {@link StorageResolver}.
     */
    StorageResolver getResolver(FileStore store);

    /**
     * Try to resolve the provided {@link AccessScope} within this {@link StorageAware} context.
     *
     * @param scope
     *         The {@link AccessScope} to resolve.
     *
     * @return A {@link Path} pointing to an existing directory, or a file, depending on the underlying {@link FileStore} type.
     */
    default Path resolve(AccessScope scope) {

        if (scope.store().type() == StoreType.FILE_SCOPED) {
            return this.getResolver(scope.store()).file(scope.claim());
        }
        return this.getResolver(scope.store()).directory(scope.claim());
    }

    /**
     * Try to resolve the provided {@link AccessScope} within this {@link StorageAware} context.
     *
     * @param scope
     *         The {@link AccessScope} to resolve.
     * @param filename
     *         The file name to resolve.
     *
     * @return A {@link Path} pointing to a file.
     */
    default Path resolve(AccessScope scope, String filename) {

        return this.getResolver(scope.store()).file(scope.claim(), filename);
    }

}
