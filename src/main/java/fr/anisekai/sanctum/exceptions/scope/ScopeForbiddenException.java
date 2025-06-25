package fr.anisekai.sanctum.exceptions.scope;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.LibraryException;

/**
 * Exception thrown when a {@link AccessScope} required couldn't be found.
 */
public class ScopeForbiddenException extends LibraryException {

    /**
     * Create a new {@link ScopeForbiddenException}.
     *
     * @param message
     *         The message explaining the error
     */
    public ScopeForbiddenException(String message) {

        super(message);
    }

}
