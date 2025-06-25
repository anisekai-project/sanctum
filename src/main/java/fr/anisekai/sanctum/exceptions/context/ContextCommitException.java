package fr.anisekai.sanctum.exceptions.context;

import fr.anisekai.sanctum.exceptions.LibraryException;
import fr.anisekai.sanctum.interfaces.Library;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;

/**
 * Exception thrown when a {@link IsolationSession} couldn't be fully committed to a {@link Library}.
 */
public class ContextCommitException extends LibraryException {

    /**
     * Create a new {@link ContextCommitException}.
     *
     * @param message
     *         The message explaining the error
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public ContextCommitException(String message, Throwable cause) {

        super(message, cause);
    }

}
