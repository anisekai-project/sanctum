package fr.anisekai.sanctum.enums;

import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.Library;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;

/**
 * Enum allowing to declare a {@link FileStore} management policy into a {@link Library} when merging an
 * {@link IsolationSession}.
 */
public enum StorePolicy {

    /**
     * The {@link FileStore} can only be used within {@link Library}. Any attempt to use such {@link FileStore} within a
     * {@link IsolationSession} will be denied.
     */
    PRIVATE,

    /**
     * The {@link FileStore} can be used within a {@link IsolationSession} and its content will be copied over the main storage
     * upon commit, replacing existing files only.
     */
    OVERWRITE,

    /**
     * The {@link FileStore} can be used within a {@link IsolationSession} and its content will completely replace the one present
     * in the main storage, removing every unused files.
     */
    FULL_SWAP,

    /**
     * The {@link FileStore} is unique to each instance of {@link IsolationSession} and its content will be discarded once the
     * {@link IsolationSession} is closed.
     */
    DISCARD;

    /**
     * Check if this {@link StorePolicy} will allow modification on the {@link Library} content.
     *
     * @return True if the {@link Library} content modification is allowed, false otherwise.
     */
    public boolean willModifyFilesystem() {

        return switch (this) {
            case OVERWRITE, FULL_SWAP -> true;
            case DISCARD, PRIVATE -> false;
        };
    }

}
