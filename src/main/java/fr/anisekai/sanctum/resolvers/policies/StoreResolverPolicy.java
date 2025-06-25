package fr.anisekai.sanctum.resolvers.policies;

import fr.anisekai.sanctum.exceptions.StorageException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.resolvers.ResolverPolicy;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

/**
 * Implementation of {@link ResolverPolicy} allowing to define basic resolving rules for a {@link StorageResolver}.
 *
 * @param store
 *         The {@link FileStore} being accessed within a {@link StorageResolver}.
 */
public record StoreResolverPolicy(FileStore store) implements ResolverPolicy {

    @Override
    public void checkResolveDirectory(String name) {

        if (!this.store().type().isScoped()) return;
        throw new StorageException("Tried to resolve a directory on a scoped store.");
    }

    @Override
    public void checkResolveFile(String filename) {

        if (!this.store().type().isScoped()) return;
        throw new StorageException("Tried to resolve a file on a scoped store.");
    }

    @Override
    public void checkResolveDirectory(ScopedEntity entity) {

        if (this.store().type().isScoped()) return;
        throw new StorageException("Tried to resolve a scoped directory.");
    }

    @Override
    public void checkResolveFile(ScopedEntity entity) {

        switch (this.store().type()) {
            case DIRECTORY_SCOPED ->
                    throw new StorageException("Tried to resolve a file scoped entity on a directory scoped store.");
            case UNSCOPED -> throw new StorageException("Tried to resolve a file scoped entity on a unscoped store.");
        }
    }

    @Override
    public void checkResolveFile(ScopedEntity entity, String name) {

        switch (this.store().type()) {
            case FILE_SCOPED -> throw new StorageException("Tried to resolve a directory scoped file on a file scoped store.");
            case UNSCOPED -> throw new StorageException("Tried to resolve a directory scoped entity file on a unscoped store.");
        }
    }

}
