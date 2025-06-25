package fr.anisekai.sanctum.exceptions.scope;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.LibraryException;

/**
 * Exception thrown when a {@link AccessScope} couldn't be granted.
 */
public class ScopeGrantException extends LibraryException {

    /**
     * Create a new {@link ScopeGrantException}.
     *
     * @param message
     *         The message explaining the error
     */
    public ScopeGrantException(String message) {

        super(message);
    }

}
