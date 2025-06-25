package fr.anisekai.sanctum.enums;

import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;

/**
 * Enum allowing to declare the nature of a {@link FileStore}.
 */
public enum StoreType {

    /**
     * The {@link FileStore} is a reference to a store containing one directory for each {@link ScopedEntity}.
     */
    DIRECTORY_SCOPED,

    /**
     * The {@link FileStore} is a reference to a store containing one file for each {@link ScopedEntity}.
     */
    FILE_SCOPED,

    /**
     * The {@link FileStore} is a reference to a store containing files without clear structure.
     */
    UNSCOPED;

    /**
     * Check if the {@link FileStore} using this {@link StoreType} is a structured store using a {@link ScopedEntity}.
     *
     * @return True if the {@link StoreType} indicate a scoped store, false otherwise.
     */
    public boolean isScoped() {

        return switch (this) {
            case DIRECTORY_SCOPED, FILE_SCOPED -> true;
            case UNSCOPED -> false;
        };
    }

}
