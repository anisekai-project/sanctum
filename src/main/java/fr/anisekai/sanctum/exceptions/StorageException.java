package fr.anisekai.sanctum.exceptions;

import fr.anisekai.sanctum.interfaces.FileStore;

/**
 * Exception thrown when an error occurs while accessing a {@link FileStore} content.
 */
public class StorageException extends LibraryException {

    /**
     * Create a new {@link StorageException}.
     *
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public StorageException(Throwable cause) {

        super(cause);
    }

    /**
     * Create a new {@link StorageException}.
     *
     * @param message
     *         The message explaining the error
     */
    public StorageException(String message) {

        super(message);
    }

}
