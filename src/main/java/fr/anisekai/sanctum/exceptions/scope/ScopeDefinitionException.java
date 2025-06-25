package fr.anisekai.sanctum.exceptions.scope;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.LibraryException;

/**
 * Exception thrown when a {@link AccessScope} couldn't be created.
 */
public class ScopeDefinitionException extends LibraryException {

    /**
     * Create a new {@link ScopeDefinitionException}.
     *
     * @param message
     *         The message explaining the error
     */
    public ScopeDefinitionException(String message) {

        super(message);
    }

}
