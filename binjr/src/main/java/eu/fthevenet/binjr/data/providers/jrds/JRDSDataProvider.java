package eu.fthevenet.binjr.data.providers.jrds;

import com.google.gson.Gson;
import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.providers.DataProvider;
import eu.fthevenet.binjr.data.providers.DataProviderException;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 14/10/2016.
 */
public class JRDSDataProvider implements DataProvider {
    private static final Logger logger = LogManager.getLogger(JRDSDataProvider.class);

    private final String jrdsHost;
    private final int jrdsPort;
    private final String jrdsPath;

    public JRDSDataProvider(String hostname, int port, String path) {
        this.jrdsHost = hostname;
        this.jrdsPort = port;
        this.jrdsPath = path;
    }

    public TreeItem<JRDSTreeNode> getJRDSTree() throws DataProviderException {

        Gson gson = new Gson();
        JsonTree t = gson.fromJson(getJsonTree("hoststab"), JsonTree.class);

       Map<String, List<JsonItem>> m= Arrays.stream(t.items).collect(Collectors.toMap(o -> o.type, (o-> o));


//        JSONTokener tokener = new JSONTokener(getJsonTree("hoststab"));
//        JSONObject root = new JSONObject(tokener);

        TreeItem<JRDSTreeNode> tree = new TreeItem<>(new JRDSTreeNode(jrdsHost + ":" + jrdsPort));
//        JSONArray items =  (JSONArray)root.get("items");

//        for (Object n :items.toList()){
//            switch(((JSONObject)n).get("type").toString()){
//                case "tree":
//                    break;
//                case "node":
//                    break;
//                case"graph":
//                    break;
//                    default:
//                        throw new Exception("unexpected node type");
//            }
//
//        }


        return tree;
    }

    public String getJsonTree(String tabname) throws DataProviderException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme("http")
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
    public long getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out) throws DataProviderException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme("http")
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

    private<T> T doHttpGet(URIBuilder requestUrl, ResponseHandler<T> responseHandler) throws DataProviderException{
        try (Profiler p = Profiler.start("Executing HTTP request: [" + requestUrl.toString()+"]", logger::trace)) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                logger.debug(() -> "requestUrl = " + requestUrl);
                HttpGet httpget = new HttpGet(requestUrl.build());
                return httpClient.execute(httpget,responseHandler);
            }
        } catch (IOException e) {
            throw new DataProviderException("Error executing HTTP request [" + requestUrl.toString() + "]", e);
        } catch (URISyntaxException e) {
            throw new DataProviderException("Error building URI for request");
        }
    }
}
