/*
 *    Copyright 2016-2020 Frederic Thevenet
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
 */

package eu.binjr.sources.jrds.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.xml.XmlUtils;
import eu.binjr.core.data.adapters.HttpDataAdapter;
import eu.binjr.core.data.adapters.SerializedDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.codec.csv.CsvDecoder;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.exceptions.SourceCommunicationException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.jrds.adapters.json.JsonJrdsItem;
import eu.binjr.sources.jrds.adapters.json.JsonJrdsTree;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.io.IOException;
import java.io.InputStream;
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
 * This class provides an implementation of {@link SerializedDataAdapter} for JRDS.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JrdsDataAdapter extends HttpDataAdapter<Double> {
    public static final String JRDS_FILTER = "filter";
    public static final String JRDS_TREE = "tree";
    protected static final String ENCODING_PARAM_NAME = "encoding";
    protected static final String ZONE_ID_PARAM_NAME = "zoneId";
    protected static final String TREE_VIEW_TAB_PARAM_NAME = "treeViewTab";
    private static final Logger logger = Logger.create(JrdsDataAdapter.class);
    private static final char DELIMITER = ',';
    private final Gson gson;
    private CsvDecoder decoder;
    private String filter;
    private ZoneId zoneId;
    private String encoding;
    private JrdsTreeViewTab treeViewTab;

    /**
     * Initialises a new instance of the {@link JrdsDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public JrdsDataAdapter() throws DataAdapterException {
        this(null, ZoneId.systemDefault(), "utf-8", JrdsTreeViewTab.HOSTS_TAB, "");
    }

    /**
     * Initializes a new instance of the {@link JrdsDataAdapter} class.
     *
     * @param baseURL     the URL to the JRDS webapp.
     * @param zoneId      the id of the time zone used to record dates.
     * @param encoding    the encoding used by the download servlet.
     * @param treeViewTab the tab to apply.
     * @param filter      the filter to apply to the tree view.
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public JrdsDataAdapter(URL baseURL, ZoneId zoneId, String encoding, JrdsTreeViewTab treeViewTab, String filter) throws DataAdapterException {
        super(baseURL);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.treeViewTab = treeViewTab;
        this.filter = filter;
        this.decoder = decoderFactory(zoneId);
        gson = new Gson();
    }

    /**
     * Builds a new instance of the {@link JrdsDataAdapter} class from the provided parameters.
     *
     * @param address     the URL to the JRDS webapp.
     * @param zoneId      the id of the time zone used to record dates.
     * @param treeViewTab the tab to apply.
     * @param filter      the filter to apply to the tree view.
     * @return a new instance of the {@link JrdsDataAdapter} class.
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public static JrdsDataAdapter fromUrl(String address, ZoneId zoneId, JrdsTreeViewTab treeViewTab, String filter) throws DataAdapterException {
        return new JrdsDataAdapter(urlFromString(address), zoneId, "utf-8", treeViewTab, filter);
    }

    //region [DataAdapter Members]

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        try {
            JsonJrdsTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument(), filter), JsonJrdsTree.class);
            Map<String, JsonJrdsItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
            FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                    new JrdsBindingBuilder()
                            .withLabel(getSourceName())
                            .withPath("/")
                            .withAdapter(this)
                            .build());
            for (JsonJrdsItem branch : Arrays.stream(t.items)
                    .filter(jsonJrdsItem -> JRDS_TREE.equals(jsonJrdsItem.type) || JRDS_FILTER.equals(jsonJrdsItem.type))
                    .collect(Collectors.toList())) {
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
        return craftRequestUri("download",
                new BasicNameValuePair("id", path),
                new BasicNameValuePair("begin", Long.toString(begin.toEpochMilli())),
                new BasicNameValuePair("end", Long.toString(end.toEpochMilli()))
        );
    }

    @Override
    public String getSourceName() {
        return new StringBuilder("[JRDS] ")
                .append(getBaseAddress() != null ? getBaseAddress().getHost() : "???")
                .append((getBaseAddress() != null && getBaseAddress().getPort() > 0) ? ":" + getBaseAddress().getPort() : "")
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
        params.put(ZONE_ID_PARAM_NAME, zoneId.toString());
        params.put(ENCODING_PARAM_NAME, encoding);
        params.put(TREE_VIEW_TAB_PARAM_NAME, treeViewTab.name());
        params.put(JRDS_FILTER, this.filter);
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        super.loadParams(params);
        encoding = validateParameterNullity(params, ENCODING_PARAM_NAME);
        zoneId = validateParameter(params, ZONE_ID_PARAM_NAME,
                s -> {
                    if (s == null) {
                        throw new InvalidAdapterParameterException("Parameter " + ZONE_ID_PARAM_NAME + " is missing in adapter " + getSourceName());
                    }
                    return ZoneId.of(s);
                });
        treeViewTab = validateParameter(params, TREE_VIEW_TAB_PARAM_NAME, s -> s == null ? JrdsTreeViewTab.valueOf(params.get(TREE_VIEW_TAB_PARAM_NAME)) : JrdsTreeViewTab.HOSTS_TAB);
        this.filter = params.get(JRDS_FILTER);
        this.decoder = decoderFactory(zoneId);
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
    public CsvDecoder getDecoder() {
        return decoder;
    }

    @Override
    public void close() {
        super.close();
    }

    //endregion

    public Collection<String> discoverFilters() throws DataAdapterException, URISyntaxException {
        try {
            JsonJrdsTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument()), JsonJrdsTree.class);
            return Arrays.stream(t.items).filter(jsonJrdsItem -> JRDS_FILTER.equals(jsonJrdsItem.type)).map(i -> i.filter).collect(Collectors.toList());
        } catch (JsonParseException e) {
            throw new DataAdapterException("An error occurred while parsing the json response to getBindingTree request", e);
        }
    }

    private void attachNode(FilterableTreeItem<SourceBinding> tree, String id, Map<String, JsonJrdsItem> nodes) throws DataAdapterException {
        JsonJrdsItem n = nodes.get(id);
        String currentPath = normalizeId(n.id);
        FilterableTreeItem<SourceBinding> newBranch = new FilterableTreeItem<>(
                new JrdsBindingBuilder()
                        .withParent(tree.getValue())
                        .withLabel(n.name)
                        .withPath(currentPath)
                        .withAdapter(this)
                        .build());
        if (JRDS_FILTER.equals(n.type)) {
            // add a dummy node so that the branch can be expanded
            newBranch.getInternalChildren().add(new FilterableTreeItem<>(null));
            // add a listener that will get the treeview filtered according to the selected filter/tag
            newBranch.expandedProperty().addListener(new FilteredViewListener(n, newBranch));
        } else {
            if (n.children != null) {
                for (JsonJrdsItem.JsonTreeRef ref : n.children) {
                    attachNode(newBranch, ref._reference, nodes);
                }
            } else {
                // add a dummy node so that the branch can be expanded
                newBranch.getInternalChildren().add(new FilterableTreeItem<>(null));
                // add a listener so that bindings for individual datastore are added lazily to avoid
                // dozens of individual call to "graphdesc" when the tree is built.
                newBranch.expandedProperty().addListener(new GraphDescListener(currentPath, newBranch, tree));
            }
        }
        tree.getInternalChildren().add(newBranch);
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
        String entityString = doHttpGet(craftRequestUri("jsontree", params), response -> {
            var entity = response.getEntity();
            try {
                if ("application/json".equalsIgnoreCase(ContentType.getOrDefault(entity).getMimeType())) {
                    return EntityUtils.toString(entity);
                }
                logger.error("HTTP response content type is '" +
                        ContentType.getOrDefault(entity).getMimeType() +
                        " (expected 'application/json')");
                return null;
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
        });
        logger.trace(entityString);
        return entityString;
    }


    private Graphdesc getGraphDescriptor(String id) throws DataAdapterException {
        URI requestUri = craftRequestUri("graphdesc", new BasicNameValuePair("id", id));

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
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
    }

    private CsvDecoder decoderFactory(ZoneId zoneId) {
        return new CsvDecoder(getEncoding(), DELIMITER,
                DoubleTimeSeriesProcessor::new,
                s -> ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId)));
    }

    private class GraphDescListener implements ChangeListener<Boolean> {
        private final String currentPath;
        private final FilterableTreeItem<SourceBinding> newBranch;
        private final FilterableTreeItem<SourceBinding> tree;

        public GraphDescListener(String currentPath, FilterableTreeItem<SourceBinding> newBranch, FilterableTreeItem<SourceBinding> tree) {
            this.currentPath = currentPath;
            this.newBranch = newBranch;
            this.tree = tree;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                try {
                    Graphdesc graphdesc = getGraphDescriptor(currentPath);
                    newBranch.setValue(new JrdsBindingBuilder()
                            .withGraphDesc(graphdesc)
                            .withParent(tree.getValue())
                            .withLegend(newBranch.getValue().getLegend())
                            .withPath(currentPath)
                            .withAdapter(JrdsDataAdapter.this)
                            .build());
                    for (int i = 0; i < graphdesc.seriesDescList.size(); i++) {
                        String graphType = graphdesc.seriesDescList.get(i).graphType;
                        if (!"none".equalsIgnoreCase(graphType) && !"comment".equalsIgnoreCase(graphType)) {
                            newBranch.getInternalChildren().add(new FilterableTreeItem<>((new JrdsBindingBuilder()
                                    .withGraphDesc(graphdesc, i)
                                    .withParent(newBranch.getValue())
                                    .withPath(currentPath)
                                    .withAdapter(JrdsDataAdapter.this)
                                    .build())));
                        }
                    }
                    //remove dummy node
                    newBranch.getInternalChildren().remove(0);
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
        private final FilterableTreeItem<SourceBinding> newBranch;

        public FilteredViewListener(JsonJrdsItem n, FilterableTreeItem<SourceBinding> newBranch) {
            this.n = n;
            this.newBranch = newBranch;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                try {
                    JsonJrdsTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), JRDS_FILTER, n.name), JsonJrdsTree.class);
                    Map<String, JsonJrdsItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
                    for (JsonJrdsItem branch : Arrays.stream(t.items).filter(jsonJrdsItem -> JRDS_TREE.equals(jsonJrdsItem.type) || JRDS_FILTER.equals(jsonJrdsItem.type)).collect(Collectors.toList())) {
                        attachNode(newBranch, branch.id, m);
                    }
                    //remove dummy node
                    newBranch.getInternalChildren().remove(0);
                    // remove the listener so it isn't executed next time node is expanded
                    newBranch.expandedProperty().removeListener(this);
                } catch (Exception e) {
                    Dialogs.notifyException("Failed to retrieve graph description", e);
                }
            }
        }
    }
}
