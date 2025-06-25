package fr.anisekai.sanctum.exceptions;

import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.Library;

/**
 * Exception thrown when a {@link FileStore} could not be added to a {@link Library}.
 */
public class StoreRegistrationException extends LibraryException {

    /**
     * Create a new {@link StoreRegistrationException}.
     *
     * @param message
     *         The message explaining the error
     */
    public StoreRegistrationException(String message) {

        super(message);
    }

    /**
     * Create a new {@link StoreRegistrationException}.
     *
     * @param message
     *         The message explaining the error
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public StoreRegistrationException(String message, Throwable cause) {

        super(message, cause);
    }

}
