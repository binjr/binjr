package eu.fthevenet.binjr.data.adapters.jrds;

import com.google.gson.Gson;
import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.adapters.DataAdapterInfo;
import eu.fthevenet.binjr.data.parsers.CsvParser;
import eu.fthevenet.binjr.data.parsers.DataParser;
import javafx.scene.control.TreeItem;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;

import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 14/10/2016.
 */
@DataAdapterInfo(name = "JRDS", description = "A binjr data adapter for JRDS.")
public class JRDSDataAdapter implements DataAdapter<Double> {
    private static final Logger logger = LogManager.getLogger(JRDSDataAdapter.class);
    private final String jrdsHost;
    private final int jrdsPort;
    private final String jrdsPath;
    private final String jrdsScheme;
    private final ZoneId zoneId;
    private final String encoding;


    public static JRDSDataAdapter createHttp(String hostname, int port, String path, ZoneId zoneId, String encoding){
        return new JRDSDataAdapter(hostname, port, path, zoneId, encoding, "http");
    }

    public static JRDSDataAdapter createHttps(String hostname, int port, String path, ZoneId zoneId, String encoding){
        return new JRDSDataAdapter(hostname, port, path, zoneId, encoding, "https");
    }

    private JRDSDataAdapter(String hostname, int port, String path, ZoneId zoneId, String encoding, String jrdsScheme) {
        this.jrdsHost = hostname;
        this.jrdsPort = port;
        this.jrdsPath = path;
        this.jrdsScheme = jrdsScheme;
        this.zoneId = zoneId;
        this.encoding = encoding;
    }

    public TreeItem<TimeSeriesBinding<Double>> getTree() throws DataAdapterException {
        Gson gson = new Gson();
        JsonTree t = gson.fromJson(getJsonTree("hoststab"), JsonTree.class);
        Map<String, JsonItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
        TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(new JRDSSeriesBinding("JRDS" + ":" + jrdsHost, "/", this));
        List<TreeItem<JsonItem>> l = new ArrayList<>();
        for (JsonItem branch : Arrays.stream(t.items).filter(jsonItem -> "tree".equals(jsonItem.type)).collect(Collectors.toList())) {
            attachNode(tree, branch.id, m);
        }
        return tree;
    }

    private  TreeItem<TimeSeriesBinding<Double>> attachNode(TreeItem<TimeSeriesBinding<Double>> tree, String id, Map<String, JsonItem> nodes){
        JsonItem n = nodes.get(id);
        TreeItem<TimeSeriesBinding<Double>> newBranch = new TreeItem<>(new JRDSSeriesBinding(n.name, n.id, this));
        if (n.children != null){
            for (JsonTreeRef ref : n.children){
                attachNode(newBranch, ref._reference, nodes);
            }
        }
        tree.getChildren().add(newBranch);
        return tree;
    }

    private String getJsonTree(String tabname) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsScheme)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/jsontree")
                .addParameter("tab", tabname);
        return doHttpGet(requestUrl, response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                 return EntityUtils.toString(entity);
                }
                return null;
            }
            else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        });
    }

    @Override
    public long getData(String path, Instant begin, Instant end, OutputStream out) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsScheme)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/download")
                .addParameter("id", path)
                .addParameter("begin", Long.toString(begin.toEpochMilli()))
                .addParameter("end", Long.toString(end.toEpochMilli()));

        return doHttpGet(requestUrl, response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    long length = entity.getContentLength();
                    entity.writeTo(out);
                    return length;
                }
                return 0L;
            }
            else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        });
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public  DataParser<Double> getParser() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(getTimeZoneId());
        return new CsvParser<>(getEncoding(), ";",
                s -> {
                    Double val = Double.parseDouble(s);
                    return val.isNaN() ? 0 : val;
                },
                s -> ZonedDateTime.parse(s, formatter));
    }

    @Override
    public long getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsScheme)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/download/probe/" + targetHost + "/" + probe)
                .addParameter("begin", Long.toString(begin.toEpochMilli()))
                .addParameter("end", Long.toString(end.toEpochMilli()));

        return doHttpGet(requestUrl, response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    long length = entity.getContentLength();
                    entity.writeTo(out);
                    return length;
                }
                return 0L;
            }
            else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        });
    }

    private<T> T doHttpGet(URIBuilder requestUrl, ResponseHandler<T> responseHandler) throws DataAdapterException {
        try (Profiler p = Profiler.start("Executing HTTP request: [" + requestUrl.toString()+"]", logger::trace)) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                logger.debug(() -> "requestUrl = " + requestUrl);
                HttpGet httpget = new HttpGet(requestUrl.build());
                return httpClient.execute(httpget,responseHandler);
            }
        } catch (IOException e) {
            throw new DataAdapterException("Error executing HTTP request [" + requestUrl.toString() + "]", e);
        } catch (URISyntaxException e) {
            throw new DataAdapterException("Error building URI for request");
        }
    }
}
