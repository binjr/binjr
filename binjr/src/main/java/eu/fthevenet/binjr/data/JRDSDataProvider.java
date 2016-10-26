package eu.fthevenet.binjr.data;

import eu.fthevenet.binjr.commons.logging.Profiler;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import java.io.OutputStream;

/**
 * Created by FTT2 on 14/10/2016.
 */
public class JRDSDataProvider implements DataProvider {
    private static final Logger logger = LogManager.getLogger(JRDSDataProvider.class);

    private final String jrdsHost;

    public JRDSDataProvider(String jrdsHost) {
        this.jrdsHost = jrdsHost;
    }

    @Override
    public boolean getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out) {
        String qid = "targetHost=" + targetHost + " probe=" + probe + " begin=" + begin.toString() + "end=" + end.toString();
        try (Profiler p = Profiler.start("Getting data for " + qid, logger::trace)) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                URI requestUrl = new URIBuilder()
                        .setScheme("http")
                        .setHost(jrdsHost)
                        .setPath("/perf-ui/download/probe/" + targetHost + "/" + probe)
                        .addParameter("begin", Long.toString(begin.toEpochMilli()))
                        .addParameter("end", Long.toString(end.toEpochMilli())).build();
                logger.debug(() -> "requestUrl = " + requestUrl);
                HttpGet httpget = new HttpGet(requestUrl);
                return httpClient.execute(httpget, response -> {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            entity.writeTo(out);
                            return true;
                        }
                        return false;
                    }
                    else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Error executing request " + qid, e);

        } catch (URISyntaxException e) {
            logger.error("Error building URI for request " + qid, e);

        }
        return false;
    }
}
