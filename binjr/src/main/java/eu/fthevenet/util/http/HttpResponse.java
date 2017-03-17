package eu.fthevenet.util.http;

import eu.fthevenet.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an HTTP response
 * @author Frederic Thevenet
 */
public class HttpResponse {
    private final int responseCode;
    private final InputStream content;


    HttpResponse(int responseCode, InputStream inputStream) {
        this.responseCode = responseCode;
        this.content = inputStream;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseAsString() throws IOException {
        return IOUtils.readToString(content);
    }

    public InputStream getContent() {
        return content;
    }
}
