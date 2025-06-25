package fr.anisekai.sanctum.stores;

import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;

/**
 * Specific implementation of a {@link FileStore} allowing to declare a folder in a library containing a file for each
 * {@link ScopedEntity}, the extension being forced at the store level.
 *
 * @param name
 *         The name of this {@link FileStore}
 * @param scope
 *         The type of {@link ScopedEntity} that this {@link FileStore} uses.
 * @param extension
 *         The extension that this {@link FileStore} will enforce.
 */
public record ScopedFileStorage(String name, Class<? extends ScopedEntity> scope, String extension) implements FileStore {

    @Override
    public StoreType type() {

        return StoreType.FILE_SCOPED;
    }

}
