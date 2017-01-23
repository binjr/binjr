package eu.fthevenet.binjr.data.adapters;

/**
 * Signals that an error happened while while using the DataAdapter.
 * @author Frederic Thevenet
 */
public class DataAdapterException extends Exception {
    /**
     * Creates a new instance of the {@link DataAdapterException} class.
     */
    public DataAdapterException() {
        super();
    }

    /**
     * Creates a new instance of the {@link DataAdapterException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public DataAdapterException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link DataAdapterException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public DataAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link DataAdapterException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public DataAdapterException(Throwable cause) {
        super(cause);
    }
}
