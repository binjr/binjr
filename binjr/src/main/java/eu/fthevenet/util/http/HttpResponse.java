package eu.fthevenet.util.http;

import eu.fthevenet.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * A minimal representation of an HTTP response
 *
 * @author Frederic Thevenet
 */
public class HttpResponse {
    private final int responseCode;
    private final InputStream content;

    /**
     * Initializes a instance of the {@link HttpResponse} class
     *
     * @param responseCode the response HTTP code
     * @param inputStream  the content of the response
     */
    HttpResponse(int responseCode, InputStream inputStream) {
        this.responseCode = responseCode;
        this.content = inputStream;
    }

    /**
     * Gets the response HTTP code
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Gets the content of the response as a string
     * @return the content of the response as a string
     * @throws IOException in an error reading the response's stream
     */
    public String getResponseAsString() throws IOException {
        return IOUtils.readToString(content);
    }

    /**
     * Gets the content of the response
     * @return the content of the response
     */
    public InputStream getContent() {
        return content;
    }
}
