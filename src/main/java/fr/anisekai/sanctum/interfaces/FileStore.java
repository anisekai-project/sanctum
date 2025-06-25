package fr.anisekai.sanctum.interfaces;

import fr.anisekai.sanctum.enums.StoreType;

/**
 * Interface describing the nature of a directory within the library.
 */
public interface FileStore {

    /**
     * Retrieve this {@link FileStore}'s {@link StoreType}.
     *
     * @return A {@link StoreType}.
     */
    StoreType type();

    /**
     * Retrieve the {@link ScopedEntity} type to use with this {@link FileStore}.
     *
     * @return A {@link ScopedEntity} class.
     */
    default Class<? extends ScopedEntity> scope() {

        throw new UnsupportedOperationException("This store does not support scoped entities");
    }

    /**
     * Retrieve the extension to use for each {@link ScopedEntity} when resolving a file.
     *
     * @return The file extension
     */
    default String extension() {

        throw new UnsupportedOperationException("This store does not support enforced extensions");
    }

    /**
     * Retrieve this {@link FileStore}'s name. The name is the directory name on the disk.
     *
     * @return The store name.
     */
    String name();

}
