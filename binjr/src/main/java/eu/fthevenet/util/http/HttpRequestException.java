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

    public HttpRequestException(HttpResponse response) {
        this(response, null);
    }

    public HttpRequestException(String message, HttpResponse response) {
        this(message, response, null);
    }

    public HttpRequestException(HttpResponse response, Throwable cause) {
        this("An error occurred while sending HTTP request: Status [ " + (response != null ? response.getResponseCode() : "undefined") + "] - " + safeReadBody(response), response, cause);
    }

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

    public int getResponseCode() {
        return response.getResponseCode();
    }

    public String getErrorBody() {
        return errorBody;
    }
}
