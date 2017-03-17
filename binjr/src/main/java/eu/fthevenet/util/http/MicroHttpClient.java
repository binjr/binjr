package eu.fthevenet.util.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A *very* limited http client
 *
 * @author Frederic Thevenet
 */
public class MicroHttpClient {
    private static final Logger logger = LogManager.getLogger(MicroHttpClient.class);

    public static HttpResponse doHttpGet(URL requestUrl) throws IOException, URISyntaxException {
       return doHttpGet(requestUrl, true);
    }

    public static HttpResponse doHttpGet(URL requestUrl, boolean throwOnErrorStatus) throws IOException, URISyntaxException {
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestProperty("Accept-Charset", "utf-8");
        int status = connection.getResponseCode();
        logger.trace("HTTP GET on URL: " + requestUrl.toString());

        try {
            if (status >= 200 && status < 300) {
                return new HttpResponse(status, connection.getInputStream());
            }
            else if (throwOnErrorStatus) {
                throw new HttpRequestException("Unexpected response status: " + status, onError(connection));
            }
            else {
                return onError(connection);
            }
        } catch (IOException e) {
            throw new HttpRequestException(onError(connection), e);
        }
    }

    private static HttpResponse onError(HttpURLConnection conn) throws IOException {
        int respCode = conn.getResponseCode();
        try (InputStream es = conn.getErrorStream()) {
            return new HttpResponse(respCode, es);
        }
    }

}

