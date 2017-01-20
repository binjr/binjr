package eu.fthevenet.binjr.data.providers;

/**
 * Signals that an error happened while while using the DataProvider.
 * @author Frederic Thevenet
 */
public class DataProviderException extends Exception {
    /**
     * Creates a new instance of the {@link DataProviderException} class.
     */
    public DataProviderException() {
        super();
    }

    /**
     * Creates a new instance of the {@link DataProviderException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public DataProviderException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link DataProviderException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public DataProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link DataProviderException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public DataProviderException(Throwable cause) {
        super(cause);
    }
}
