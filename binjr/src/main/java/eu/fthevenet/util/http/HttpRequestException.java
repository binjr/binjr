package eu.fthevenet.util.http;

import eu.fthevenet.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Thrown when an error occurs while sending an HTTP request
 *
 * @author Frederic Thevenet
 */
public class HttpRequestException extends IOException {
    private final HttpResponse response;
    private String errorBody = "";

    /**
     * Initializes a new instance of the {@link HttpRequestException} class
     *
     * @param response the {@link HttpResponse}
     */
    public HttpRequestException(HttpResponse response) {
        this(response, null);
    }

    /**
     * Initializes a new instance of the {@link HttpRequestException} class
     *
     * @param message  the error message
     * @param response the {@link HttpResponse}
     */
    public HttpRequestException(String message, HttpResponse response) {
        this(message, response, null);
    }

    /**
     * Initializes a new instance of the {@link HttpRequestException} class
     *
     * @param response the {@link HttpResponse}
     * @param cause    the {@link Throwable} that caused the {@link HttpRequestException} ot be thrown
     */
    public HttpRequestException(HttpResponse response, Throwable cause) {
        this("An error occurred while sending HTTP request: Status [ " + (response != null ? response.getResponseCode() : "undefined") + "] - " + safeReadBody(response), response, cause);
    }

    /**
     * Initializes a new instance of the {@link HttpRequestException} class
     *
     * @param message  the error message
     * @param response the {@link HttpResponse}
     * @param cause    the {@link Throwable} that caused the {@link HttpRequestException} ot be thrown
     */
    public HttpRequestException(String message, HttpResponse response, Throwable cause) {
        super(message, cause);
        this.response = response;
        this.errorBody = safeReadBody(response);
    }

    private static String safeReadBody(HttpResponse response) {
        if (response == null) {
            return "";
        }
        try {
            InputStream in = response.getContent();
            return in != null ? IOUtils.readToString(response.getContent()) : "";
        } catch (IOException e) {
            // swallow exception
            return "";
        }
    }

    /**
     * Returns the response HTTP code
     *
     * @return the response HTTP code
     */
    public int getResponseCode() {
        return response.getResponseCode();
    }

    /**
     * Returns the error body
     *
     * @return the error body
     */
    public String getErrorBody() {
        return errorBody;
    }
}
