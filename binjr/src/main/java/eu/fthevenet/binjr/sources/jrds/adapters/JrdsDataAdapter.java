/*
 *    Copyright 2017 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package eu.fthevenet.binjr.sources.jrds.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.HttpDataAdapterBase;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.codec.CsvDecoder;
import eu.fthevenet.binjr.data.exceptions.*;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeriesProcessor;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.sources.jrds.adapters.json.JsonJrdsItem;
import eu.fthevenet.binjr.sources.jrds.adapters.json.JsonJrdsTree;
import eu.fthevenet.util.xml.XmlUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
public class JrdsDataAdapter extends HttpDataAdapterBase<Double, CsvDecoder<Double>> {
    private static final Logger logger = LogManager.getLogger(JrdsDataAdapter.class);
    private static final char DELIMITER = ',';
    public static final String JRDS_FILTER = "filter";
    public static final String JRDS_TREE = "tree";
    private final JrdsSeriesBindingFactory bindingFactory = new JrdsSeriesBindingFactory();
    private String filter;

    private ZoneId zoneId;
    private String encoding;
    private JrdsTreeViewTab treeViewTab;

    /**
     * Default constructor
     */
    public JrdsDataAdapter() throws DataAdapterException {
        super();
    }

    /**
     * Initializes a new instance of the {@link JrdsDataAdapter} class.
     *
     * @param zoneId      the id of the time zone used to record dates.
     * @param encoding    the encoding used by the download servlet.
     * @param treeViewTab the filter to apply to the tree view
     */
    public JrdsDataAdapter(URL baseURL, ZoneId zoneId, String encoding, JrdsTreeViewTab treeViewTab, String filter) throws DataAdapterException {
        super(baseURL);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.treeViewTab = treeViewTab;
        this.filter = filter;
    }

    /**
     * Builds a new instance of the {@link JrdsDataAdapter} class from the provided parameters.
     *
     * @param urlString    the URL to the JRDS webapp.
     * @param zoneId the id of the time zone used to record dates.
     * @return a new instance of the {@link JrdsDataAdapter} class.
     */
    public static JrdsDataAdapter fromUrl(String urlString, ZoneId zoneId, JrdsTreeViewTab treeViewTab, String filter) throws DataAdapterException {
        try {
            URL url = new URL(urlString);
            logger.trace(() -> "URL=" + url);
            if (url.getHost().trim().isEmpty()) {
                throw new CannotInitializeDataAdapterException("Malformed URL: no host");
            }
            return new JrdsDataAdapter(url, zoneId, "utf-8", treeViewTab, filter);
        } catch (MalformedURLException e) {
            throw new CannotInitializeDataAdapterException("Malformed URL: " + e.getMessage(), e);
        }
    }

    //region [DataAdapter Members]

    @Override
    public TreeItem<TimeSeriesBinding<Double>> getBindingTree() throws DataAdapterException {
        Gson gson = new Gson();
        try {
            JsonJrdsTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument(), filter), JsonJrdsTree.class);
            Map<String, JsonJrdsItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
            TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(bindingFactory.of("", getSourceName(), "/", this));
            for (JsonJrdsItem branch : Arrays.stream(t.items).filter(jsonJrdsItem -> JRDS_TREE.equals(jsonJrdsItem.type) || JRDS_FILTER.equals(jsonJrdsItem.type)).collect(Collectors.toList())) {
                attachNode(tree, branch.id, m);
            }
            return tree;
        } catch (JsonParseException e) {
            throw new DataAdapterException("An error occurred while parsing the json response to getBindingTree request", e);
        } catch (URISyntaxException e) {
            throw new SourceCommunicationException("Error building URI for request", e);
        }
    }

    @Override
    protected URI craftFetchUri(String path, Instant begin, Instant end) throws DataAdapterException {
        try {
            return new URIBuilder(getBaseUrl().toURI())
                    .setPath(getBaseUrl().getPath() + "/download")
                    .addParameter("id", path)
                    .addParameter("begin", Long.toString(begin.toEpochMilli()))
                    .addParameter("end", Long.toString(end.toEpochMilli())).build();
        } catch (URISyntaxException e) {
            throw new SourceCommunicationException("Error building URI for request", e);
        }
    }

    @Override
    public String getSourceName() {
        return new StringBuilder("[JRDS] ")
                .append(getBaseUrl() != null ? getBaseUrl().getHost() : "???")
                .append((getBaseUrl() != null && getBaseUrl().getPort() > 0) ? ":" + getBaseUrl().getPort() : "")
                .append(" - ")
                .append(treeViewTab != null ? treeViewTab : "???")
                .append(filter != null ? filter : "")
                .append(" (")
                .append(zoneId != null ? zoneId : "???")
                .append(")").toString();
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>(super.getParams());
        params.put("zoneId", zoneId.toString());
        params.put("encoding", encoding);
        params.put("treeViewTab", treeViewTab.name());
        params.put(JRDS_FILTER, this.filter);
        return params;
    }


    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        super.loadParams(params);
        encoding = validateParameterNullity(params, "encoding");
        zoneId = validateParameter(params, "zoneId",
                s -> {
                    if (s == null) {
                        throw new InvalidAdapterParameterException("Parameter zoneId is missing in adapter " + getSourceName());
                    }
                    return ZoneId.of(s);
                });
        treeViewTab = validateParameter(params, "treeViewTab", s -> s == null ? JrdsTreeViewTab.valueOf(params.get("treeViewTab")) : JrdsTreeViewTab.HOSTS_TAB);
        this.filter = params.get(JRDS_FILTER);
    }


    @Override
    public boolean ping() {
        try {
            return doHttpGet(craftRequestUri(""), new AbstractResponseHandler<Boolean>() {
                @Override
                public Boolean handleEntity(HttpEntity entity) throws IOException {
                    String entityString = EntityUtils.toString(entity);
                    logger.trace(entityString);
                    return true;
                }
            });
        } catch (Exception e) {
            logger.debug(() -> "Ping failed", e);
            return false;
        }
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
    public CsvDecoder<Double> getDecoder() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(getTimeZoneId());
        return new CsvDecoder<>(getEncoding(), DELIMITER,
                DoubleTimeSeriesProcessor::new,
                s -> {
                    Double val = Double.parseDouble(s);
                    return val.isNaN() ? 0 : val;
                },
                s -> ZonedDateTime.parse(s, formatter));
    }

    @Override
    public void close() {
        super.close();
    }

    //endregion

    public Collection<String> discoverFilters() throws DataAdapterException, URISyntaxException {
        Gson gson = new Gson();
        try {
            JsonJrdsTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument()), JsonJrdsTree.class);
            return Arrays.stream(t.items).filter(jsonJrdsItem -> JRDS_FILTER.equals(jsonJrdsItem.type)).map(i -> i.filter).collect(Collectors.toList());
        } catch (JsonParseException e) {
            throw new DataAdapterException("An error occurred while parsing the json response to getBindingTree request", e);
        }
    }

    private void attachNode(TreeItem<TimeSeriesBinding<Double>> tree, String id, Map<String, JsonJrdsItem> nodes) throws DataAdapterException {
        JsonJrdsItem n = nodes.get(id);
        String currentPath = normalizeId(n.id);
        TreeItem<TimeSeriesBinding<Double>> newBranch = new TreeItem<>(bindingFactory.of(tree.getValue().getTreeHierarchy(), n.name, currentPath, this));

        if (JRDS_FILTER.equals(n.type)) {
            // add a dummy node so that the branch can be expanded
            newBranch.getChildren().add(new TreeItem<>(null));
            // add a listener that will get the treeview filtered according to the selected filter/tag
            newBranch.expandedProperty().addListener(new FilteredViewListener(n, newBranch));
        }
        else {
            if (n.children != null) {
                for (JsonJrdsItem.JsonTreeRef ref : n.children) {
                    attachNode(newBranch, ref._reference, nodes);
                }
            }
            else {
                // add a dummy node so that the branch can be expanded
                newBranch.getChildren().add(new TreeItem<>(null));
                // add a listener so that bindings for individual datastore are added lazily to avoid
                // dozens of individual call to "graphdesc" when the tree is built.
                newBranch.expandedProperty().addListener(new GraphDescListener(currentPath, newBranch, tree));
            }
        }
        tree.getChildren().add(newBranch);
    }

    private String normalizeId(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("Argument id cannot be null or blank");
        }
        String[] data = id.split("\\.");
        return data[data.length - 1];
    }

    private String getJsonTree(String tabName, String argName) throws DataAdapterException, URISyntaxException {
        return getJsonTree(tabName, argName, null);
    }

    private String getJsonTree(String tabName, String argName, String argValue) throws DataAdapterException, URISyntaxException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("tab", tabName));
        if (argName != null && argValue != null && argValue.trim().length() > 0) {
            params.add(new BasicNameValuePair(argName, argValue));
        }
        String entityString = doHttpGet(craftRequestUri("/jsontree", params), new BasicResponseHandler());
        logger.trace(entityString);
        return entityString;
    }


    private Graphdesc getGraphDescriptor(String id) throws DataAdapterException {
        URI requestUri = craftRequestUri("/graphdesc", new BasicNameValuePair("id", id));

        return doHttpGet(requestUri, response -> {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 404) {
                // This is probably an older version of JRDS that doesn't provide the graphdesc service,
                // so we're falling back to recovering the datastore name from the csv file provided by
                // the download service.
                logger.warn("Cannot found graphdesc service; falling back to legacy mode.");
                try {
                    return getGraphDescriptorLegacy(id);
                } catch (Exception e) {
                    throw new IOException("", e);
                }
            }
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            if (entity != null) {
                try {
                    return JAXB.unmarshal(XmlUtils.toNonValidatingSAXSource(entity.getContent()), Graphdesc.class);
                } catch (Exception e) {
                    throw new IOException("Failed to unmarshall graphdesc response", e);
                }
            }
            return null;
        });
    }

    private Graphdesc getGraphDescriptorLegacy(String id) throws DataAdapterException {
        Instant now = ZonedDateTime.now().toInstant();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (InputStream in = fetchRawData(id, now.minusSeconds(300), now, false)) {
                List<String> headers = getDecoder().getDataColumnHeaders(in);
                Graphdesc desc = new Graphdesc();
                desc.seriesDescList = new ArrayList<>();
                for (String header : headers) {
                    Graphdesc.SeriesDesc d = new Graphdesc.SeriesDesc();
                    d.name = header;
                    desc.seriesDescList.add(d);
                }
                return desc;
            }
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
    }

    private class GraphDescListener implements ChangeListener<Boolean> {
        private final String currentPath;
        private final TreeItem<TimeSeriesBinding<Double>> newBranch;
        private final TreeItem<TimeSeriesBinding<Double>> tree;

        public GraphDescListener(String currentPath, TreeItem<TimeSeriesBinding<Double>> newBranch, TreeItem<TimeSeriesBinding<Double>> tree) {
            this.currentPath = currentPath;
            this.newBranch = newBranch;
            this.tree = tree;
        }

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
                    Dialogs.notifyException("Failed to retrieve graph description", e);
                }
            }
        }
    }

    private class FilteredViewListener implements ChangeListener<Boolean> {
        private final JsonJrdsItem n;
        private final TreeItem<TimeSeriesBinding<Double>> newBranch;

        public FilteredViewListener(JsonJrdsItem n, TreeItem<TimeSeriesBinding<Double>> newBranch) {
            this.n = n;
            this.newBranch = newBranch;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                try {
                    JsonJrdsTree t = new Gson().fromJson(getJsonTree(treeViewTab.getCommand(), JRDS_FILTER, n.name), JsonJrdsTree.class);
                    Map<String, JsonJrdsItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
                    for (JsonJrdsItem branch : Arrays.stream(t.items).filter(jsonJrdsItem -> JRDS_TREE.equals(jsonJrdsItem.type) || JRDS_FILTER.equals(jsonJrdsItem.type)).collect(Collectors.toList())) {
                        attachNode(newBranch, branch.id, m);
                    }
                    //remove dummy node
                    newBranch.getChildren().remove(0);
                    // remove the listener so it isn't executed next time node is expanded
                    newBranch.expandedProperty().removeListener(this);
                } catch (Exception e) {
                    Dialogs.notifyException("Failed to retrieve graph description", e);
                }
            }
        }
    }
}
