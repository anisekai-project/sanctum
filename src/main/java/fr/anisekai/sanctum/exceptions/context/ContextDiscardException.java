package fr.anisekai.sanctum.exceptions.context;

import fr.anisekai.sanctum.exceptions.LibraryException;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;

/**
 * Exception thrown when a {@link IsolationSession} couldn't be fully discarded.
 */
public class ContextDiscardException extends LibraryException {

    /**
     * Create a new {@link ContextDiscardException}.
     *
     * @param message
     *         The message explaining the error
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public ContextDiscardException(String message, Throwable cause) {

        super(message, cause);
    }

}
