package eu.fthevenet.binjr.data.adapters.exceptions;

/**
 * Signals that an error happened while processing the response from a source via the DataAdapter.
 *
 * @author Frederic Thevenet
 */
public class ResponseProcessingException extends DataAdapterException {
    /**
     * Creates a new instance of the {@link ResponseProcessingException} class.
     */
    public ResponseProcessingException() {
        super();
    }

    /**
     * Creates a new instance of the {@link ResponseProcessingException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public ResponseProcessingException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link ResponseProcessingException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public ResponseProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link ResponseProcessingException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public ResponseProcessingException(Throwable cause) {
        super(cause);
    }
}
