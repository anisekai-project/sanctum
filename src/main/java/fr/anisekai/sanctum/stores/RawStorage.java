package fr.anisekai.sanctum.stores;

import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.FileStore;

/**
 * Specific implementation of a {@link FileStore} allowing to declare a folder in a library without clear structure. This is ideal
 * for externally managed directories.
 *
 * @param name
 *         The name of this {@link FileStore}.
 */
public record RawStorage(String name) implements FileStore {

    @Override
    public StoreType type() {

        return StoreType.UNSCOPED;
    }

}
