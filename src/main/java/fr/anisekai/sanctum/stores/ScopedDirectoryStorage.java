package fr.anisekai.sanctum.stores;

import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;

/**
 * Specific implementation of a {@link FileStore} allowing to declare a folder in a library containing a directory for each
 * {@link ScopedEntity}. The content of this directory can be defined freely (consider the content of each folder as a
 * {@link RawStorage}).
 *
 * @param name
 *         The name of this {@link FileStore}.
 * @param scope
 *         The type of {@link ScopedEntity} that this {@link FileStore} uses.
 */
public record ScopedDirectoryStorage(String name, Class<? extends ScopedEntity> scope) implements FileStore {

    @Override
    public StoreType type() {

        return StoreType.DIRECTORY_SCOPED;
    }

}
