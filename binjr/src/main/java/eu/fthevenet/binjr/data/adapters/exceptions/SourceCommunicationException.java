package eu.fthevenet.binjr.data.adapters.exceptions;

/**
 * Signals that an error happened while communicating with the source via the DataAdapter.
 *
 * @author Frederic Thevenet
 */
public class SourceCommunicationException extends DataAdapterException {
    /**
     * Creates a new instance of the {@link SourceCommunicationException} class.
     */
    public SourceCommunicationException() {
        super();
    }

    /**
     * Creates a new instance of the {@link SourceCommunicationException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public SourceCommunicationException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link SourceCommunicationException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public SourceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link SourceCommunicationException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public SourceCommunicationException(Throwable cause) {
        super(cause);
    }
}
