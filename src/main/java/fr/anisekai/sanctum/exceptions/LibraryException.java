package fr.anisekai.sanctum.exceptions;

/**
 * Exception used to encapsulate every other exceptions in the library.
 */
public class LibraryException extends RuntimeException {

    /**
     * Create a new {@link LibraryException}.
     *
     * @param message
     *         The message explaining the error
     */
    public LibraryException(String message) {

        super(message);
    }

    /**
     * Create a new {@link LibraryException}.
     *
     * @param message
     *         The message explaining the error
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public LibraryException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Create a new {@link LibraryException}.
     *
     * @param cause
     *         The {@link Throwable} causing this error.
     */
    public LibraryException(Throwable cause) {

        super(cause);
    }

}
