package eu.fthevenet.binjr.sources.jrds.adapters;

import com.google.gson.Gson;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.SimpleCachingDataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.parsers.CsvParser;
import eu.fthevenet.binjr.data.parsers.DataParser;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeriesProcessor;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.util.http.HttpRequestException;
import eu.fthevenet.util.http.HttpResponse;
import eu.fthevenet.util.http.MicroHttpClient;
import eu.fthevenet.util.http.URLBuilder;
import eu.fthevenet.util.xml.XmlUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides an implementation of {@link DataAdapter} for JRDS.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JrdsDataAdapter extends SimpleCachingDataAdapter<Double> {
    private static final Logger logger = LogManager.getLogger(JrdsDataAdapter.class);
    private static final String SEPARATOR = ",";
    private String jrdsHost;
    private int jrdsPort;
    private String jrdsPath;
    private String jrdsProtocol;
    private ZoneId zoneId;
    private String encoding;
    private JrdsTreeFilter treeFilter;
    private final JrdsSeriesBindingFactory bindingFactory = new JrdsSeriesBindingFactory();

    /**
     * Builds a new instance of the {@link JrdsDataAdapter} class from the provided parameters.
     *
     * @param url    the URL to the JRDS webapp.
     * @param zoneId the id of the time zone used to record dates.
     * @return a new instance of the {@link JrdsDataAdapter} class.
     */
    public static JrdsDataAdapter fromUrl(String url, ZoneId zoneId, JrdsTreeFilter treeFilter) throws MalformedURLException {
        URL u = new URL(url.replaceAll("/$", ""));
        return new JrdsDataAdapter(u.getProtocol(), u.getHost(), u.getPort(), u.getPath(), zoneId, "utf-8", treeFilter);
    }

    /**
     * Default constructor
     */
    public JrdsDataAdapter() {
    }

    /**
     * Initializes a new instance of the {@link JrdsDataAdapter} class.
     *
     * @param jrdsProtocol the URL scheme if the JRDS webapp.
     * @param hostname     the host of the JRDS webapp.
     * @param port         the port of the JRDS webapp.
     * @param path         the url path of the JRDS webapp.
     * @param zoneId       the id of the time zone used to record dates.
     * @param encoding     the encoding used by the download servlet.
     * @param treeFilter   the filter to apply to the tree view
     */
    public JrdsDataAdapter(String jrdsProtocol, String hostname, int port, String path, ZoneId zoneId, String encoding, JrdsTreeFilter treeFilter) {
        super();
        this.jrdsHost = hostname;
        this.jrdsPort = port;
        this.jrdsPath = path;
        this.jrdsProtocol = jrdsProtocol;
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.treeFilter = treeFilter;
    }

    //region [DataAdapter Members]
    @Override
    public TreeItem<TimeSeriesBinding<Double>> getBindingTree() throws DataAdapterException {
        Gson gson = new Gson();
        JsonTree t = gson.fromJson(getJsonTree(treeFilter), JsonTree.class);
        Map<String, JsonTree.JsonItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
        TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(bindingFactory.of("", getSourceName(), "/", this));
        List<TreeItem<JsonTree.JsonItem>> l = new ArrayList<>();
        for (JsonTree.JsonItem branch : Arrays.stream(t.items).filter(jsonItem -> "tree".equals(jsonItem.type)).collect(Collectors.toList())) {
            attachNode(tree, branch.id, m);
        }
        return tree;
    }

    @Override
    public InputStream onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException {
        URLBuilder requestUrl = new URLBuilder()
                .setProtocol(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/download")
                .addParameter("id", path)
                .addParameter("begin", Long.toString(begin.toEpochMilli()))
                .addParameter("end", Long.toString(end.toEpochMilli()));
        try {
            HttpResponse response = MicroHttpClient.doHttpGet(requestUrl.build());
            return response.getContent();
        } catch (IOException e) {
            throw new DataAdapterException("Error executing HTTP request [" + requestUrl.toString() + "]", e);
        } catch (URISyntaxException e) {
            throw new DataAdapterException("Error building URI for request");
        }
    }

    @Override
    public String getSourceName() {
        return "[JRDS] " + jrdsHost + ":" + jrdsPort + " (" + zoneId.toString() + ")";
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("jrdsHost", jrdsHost);
        params.put("jrdsPort", Integer.toString(jrdsPort));
        params.put("jrdsProtocol", jrdsProtocol);
        params.put("jrdsPath", jrdsPath);
        params.put("zoneId", zoneId.toString());
        params.put("encoding", encoding);
        params.put("treeFilter", treeFilter.name());
        return params;
    }

    @Override
    public void setParams(Map<String, String> params) {
        jrdsProtocol = params.get("jrdsProtocol");
        jrdsHost = params.get("jrdsHost");
        jrdsPort = Integer.parseInt(params.get("jrdsPort"));
        jrdsPath = params.get("jrdsPath");
        zoneId = ZoneId.of(params.get("zoneId"));
        encoding = params.get("encoding");
        treeFilter = JrdsTreeFilter.valueOf(params.get("treeFilter"));
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
    public DataParser<Double> getParser() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(getTimeZoneId());
        return new CsvParser<>(getEncoding(), SEPARATOR,
                DoubleTimeSeriesProcessor::new,
                s -> {
                    Double val = Double.parseDouble(s);
                    return val.isNaN() ? 0 : val;
                },
                s -> ZonedDateTime.parse(s, formatter));
    }


    //endregion


    /**
     * Returns a representation of the JRDS graph descriptor fot he provided id
     *
     * @param id the id ot the graph
     * @return a representation of the JRDS graph descriptor fot he provided id
     * @throws DataAdapterException if an error occurs while retrieving the graphdesc
     */
    public Graphdesc getGraphDescriptor(String id) throws DataAdapterException {
        URLBuilder requestUrl = new URLBuilder()
                .setProtocol(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/graphdesc")
                .addParameter("id", id);
        try {
            HttpResponse response = MicroHttpClient.doHttpGet(requestUrl.build(), false);
            if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                // This is probably an older version of JRDS that doesn't provide the graphdesc service,
                // so we're falling back to recovering the datastore name from the csv file provided by
                // the download service.
                logger.warn("Cannot found graphdesc service; falling back to legacy mode.");
                try {
                    return getGraphDescriptorLegacy(id);
                } catch (Exception e) {
                    throw new HttpRequestException(e.getMessage(), response, e);
                }
            }
            else if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    Graphdesc g = JAXB.unmarshal(XmlUtils.toNonValidatingSAXSource(response.getContent()), Graphdesc.class);
                    logger.trace(() -> "Retrieved graphdesc for probe [" + id + "]: " + g.toString());
                    return g;
                } catch (Exception e) {
                    throw new HttpRequestException(e.getMessage(), response, e);
                }
            }
            else {
                throw new HttpRequestException(response);
            }

        } catch (IOException e) {
            throw new DataAdapterException("Error executing HTTP request [" + requestUrl.toString() + "]", e);
        } catch (URISyntaxException e) {
            throw new DataAdapterException("Error building URI for request");
        }
    }

    private TreeItem<TimeSeriesBinding<Double>> attachNode(TreeItem<TimeSeriesBinding<Double>> tree, String id, Map<String, JsonTree.JsonItem> nodes) throws DataAdapterException {
        JsonTree.JsonItem n = nodes.get(id);
        String currentPath = normalizeId(n.id);
        TreeItem<TimeSeriesBinding<Double>> newBranch = new TreeItem<>(bindingFactory.of(tree.getValue().getTreeHierarchy(), n.name, currentPath, this));
        if (n.children != null) {
            for (JsonTree.JsonItem.JsonTreeRef ref : n.children) {
                attachNode(newBranch, ref._reference, nodes);
            }
        }
        else {
            // add a dummy node so that the branch can be expanded
            newBranch.getChildren().add(new TreeItem<>(null));
            // add a listener so that bindings for individual datastore are added lazily to avoid
            // dozens of individual call to "graphdesc" servlet when the tree is built.
            newBranch.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        try {
                            Graphdesc graphdesc = getGraphDescriptor(currentPath);
                            newBranch.setValue(bindingFactory.of(tree.getValue().getTreeHierarchy(), newBranch.getValue().getLegend(), graphdesc, currentPath, JrdsDataAdapter.this));
                            for (int i = 0; i < graphdesc.seriesDescList.size(); i++) {
                                String graphType = graphdesc.seriesDescList.get(i).graphType;
                                if (!"none".equalsIgnoreCase(graphType) && !"comment".equalsIgnoreCase(graphType)) {
                                    newBranch.getChildren().add(new TreeItem<>(bindingFactory.of(tree.getValue().getTreeHierarchy(), graphdesc, i, currentPath, JrdsDataAdapter.this)));
                                }
                            }
                            //remove dummy node
                            newBranch.getChildren().remove(0);
                            // remove the listener so it isn't executed next time node is expanded
                            newBranch.expandedProperty().removeListener(this);
                        } catch (Exception e) {
                            Dialogs.displayException("Failed to retrieve graph description", e);
                        }
                    }
                }
            });
        }
        tree.getChildren().add(newBranch);
        return tree;
    }

    private String normalizeId(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("Argument id cannot be null or blank");
        }
        String[] data = id.split("\\.");
        return data[data.length - 1];
    }

    private String getJsonTree(JrdsTreeFilter filter) throws DataAdapterException {
        URLBuilder requestUrl = new URLBuilder()
                .setProtocol(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/jsontree")
                .addParameter("tab", filter.getCommand());

        try {
            HttpResponse response = MicroHttpClient.doHttpGet(requestUrl.build());
            return response.getResponseAsString();
        } catch (IOException e) {
            throw new DataAdapterException("Error executing HTTP request [" + requestUrl.toString() + "]", e);
        } catch (URISyntaxException e) {
            throw new DataAdapterException("Error building URI for request");
        }
    }

    private Graphdesc getGraphDescriptorLegacy(String id) throws DataAdapterException {
        Instant now = ZonedDateTime.now().toInstant();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (InputStream in = getData(id, now.minusSeconds(300), now)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
                    String header = br.readLine();
                    if (header == null || header.isEmpty()) {
                        throw new IOException("CSV File is empty!");
                    }
                    String[] headers = header.split(SEPARATOR);
                    if (headers.length < 1) {
                        throw new DataAdapterException("Could not to retrieve data store names for graph id=" + id + ": header line in csv is blank.");
                    }
                    Graphdesc desc = new Graphdesc();
                    desc.seriesDescList = new ArrayList<>();
                    for (int i = 1; i < headers.length; i++) {
                        Graphdesc.SeriesDesc d = new Graphdesc.SeriesDesc();
                        d.name = headers[i];
                        desc.seriesDescList.add(d);
                    }
                    return desc;
                }
            }
        } catch (IOException e) {
            throw new DataAdapterException(e);
        }
    }

    /**
     * POJO definition used to parse JSON message.
     */
    private static class JsonTree {
        String identifier;
        String label;
        JsonItem[] items;

        static class JsonItem {
            String name;
            String id;
            String type;
            JsonTreeRef[] children;

            static class JsonTreeRef {
                String _reference;
            }
        }
    }
}
