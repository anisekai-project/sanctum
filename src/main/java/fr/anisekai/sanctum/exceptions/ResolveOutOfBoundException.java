package fr.anisekai.sanctum.exceptions;

import fr.anisekai.sanctum.interfaces.resolvers.StorageWalker;

/**
 * Exception thrown when a {@link StorageWalker} goes up the file tree instead of going further in.
 */
public class ResolveOutOfBoundException extends LibraryException {

    /**
     * Create a new {@link LibraryException}.
     *
     * @param message
     *         The message explaining the error
     */
    public ResolveOutOfBoundException(String message) {

        super(message);
    }

}
