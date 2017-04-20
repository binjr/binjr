package eu.fthevenet.binjr.data.adapters.exceptions;

/**
 * Signals that no valid DataAdapter could be found for a given UUID.
 *
 * @author Frederic Thevenet
 */
public class NoAdapterFoundException extends DataAdapterException {
    /**
     * Creates a new instance of the {@link NoAdapterFoundException} class.
     */
    public NoAdapterFoundException() {
        super();
    }

    /**
     * Creates a new instance of the {@link NoAdapterFoundException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public NoAdapterFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link NoAdapterFoundException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public NoAdapterFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link NoAdapterFoundException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public NoAdapterFoundException(Throwable cause) {
        super(cause);
    }
}
