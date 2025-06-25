package fr.anisekai.sanctum.exceptions.context;

import fr.anisekai.sanctum.exceptions.LibraryException;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;

/**
 * Exception thrown when a {@link IsolationSession} couldn't be retrieved
 */
public class ContextUnavailableException extends LibraryException {

    /**
     * Create a new {@link ContextUnavailableException}.
     *
     * @param message
     *         The message explaining the error
     */
    public ContextUnavailableException(String message) {

        super(message);
    }

}
