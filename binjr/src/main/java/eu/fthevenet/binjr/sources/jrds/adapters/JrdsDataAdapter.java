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
import eu.fthevenet.binjr.data.adapters.SimpleCachingDataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.adapters.exceptions.CannotInitializeDataAdapterException;
import eu.fthevenet.binjr.data.adapters.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.exceptions.ResponseProcessingException;
import eu.fthevenet.binjr.data.adapters.exceptions.SourceCommunicationException;
import eu.fthevenet.binjr.data.parsers.CsvParser;
import eu.fthevenet.binjr.data.parsers.DataParser;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeriesProcessor;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.AppEnvironment;
import eu.fthevenet.util.logging.Profiler;
import eu.fthevenet.util.xml.XmlUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
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
    public static final String JRDS_FILTER = "filter";
    public static final String JRDS_TREE = "tree";
    private final JrdsSeriesBindingFactory bindingFactory = new JrdsSeriesBindingFactory();
    private final CloseableHttpClient httpClient;
    private String filter;
    private String jrdsHost;
    private int jrdsPort;
    private String jrdsPath;
    private String jrdsProtocol;
    private ZoneId zoneId;
    private String encoding;
    private JrdsTreeViewTab treeViewTab;

    /**
     * Default constructor
     */
    public JrdsDataAdapter() throws CannotInitializeDataAdapterException {
        super();
        httpClient = httpClientFactory();

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
     * @param treeViewTab  the filter to apply to the tree view
     */
    public JrdsDataAdapter(String jrdsProtocol, String hostname, int port, String path, ZoneId zoneId, String encoding, JrdsTreeViewTab treeViewTab, String filter) throws CannotInitializeDataAdapterException {
        super();
        this.jrdsHost = hostname;
        this.jrdsPort = port;
        this.jrdsPath = path;
        this.jrdsProtocol = jrdsProtocol;
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.treeViewTab = treeViewTab;
        this.filter = filter;
        httpClient = httpClientFactory();

    }

    /**
     * Builds a new instance of the {@link JrdsDataAdapter} class from the provided parameters.
     *
     * @param url    the URL to the JRDS webapp.
     * @param zoneId the id of the time zone used to record dates.
     * @return a new instance of the {@link JrdsDataAdapter} class.
     */
    public static JrdsDataAdapter fromUrl(String url, ZoneId zoneId, JrdsTreeViewTab treeViewTab, String filter) throws MalformedURLException, CannotInitializeDataAdapterException {
        URL u = new URL(url.replaceAll("/$", ""));
        return new JrdsDataAdapter(u.getProtocol(), u.getHost(), u.getPort(), u.getPath(), zoneId, "utf-8", treeViewTab, filter);
    }

    //region [DataAdapter Members]

    @Override
    public TreeItem<TimeSeriesBinding<Double>> getBindingTree() throws DataAdapterException {
        Gson gson = new Gson();
        try {
            JsonTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument(), filter), JsonTree.class);
            Map<String, JsonTree.JsonItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
            TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(bindingFactory.of("", getSourceName(), "/", this));
            for (JsonTree.JsonItem branch : Arrays.stream(t.items).filter(jsonItem -> JRDS_TREE.equals(jsonItem.type) || JRDS_FILTER.equals(jsonItem.type)).collect(Collectors.toList())) {
                attachNode(tree, branch.id, m);
            }
            return tree;
        } catch (JsonParseException e) {
            throw new DataAdapterException("An error occurred while parsing the json response to getBindingTree request", e);
        }
    }

    @Override
    public InputStream onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/download")
                .addParameter("id", path)
                .addParameter("begin", Long.toString(begin.toEpochMilli()))
                .addParameter("end", Long.toString(end.toEpochMilli()));

        return doHttpGet(requestUrl, new AbstractResponseHandler<InputStream>() {
            @Override
            public InputStream handleEntity(HttpEntity entity) throws IOException {
                return new ByteArrayInputStream(EntityUtils.toByteArray(entity));
            }
        });
    }

    @Override
    public String getSourceName() {
        return new StringBuilder("[JRDS] ")
                .append(jrdsHost != null ? jrdsHost : "???")
                .append(jrdsPort > 0 ? ":" + jrdsPort : "")
                .append(" - ")
                .append(treeViewTab != null ? treeViewTab : "???")
                .append(filter != null ? filter : "")
                .append(" (")
                .append(zoneId != null ? zoneId : "???")
                .append(")").toString();
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
        params.put("treeViewTab", treeViewTab.name());
        params.put(JRDS_FILTER, this.filter);
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
        treeViewTab = params.get("treeViewTab") != null ? JrdsTreeViewTab.valueOf(params.get("treeViewTab")) : JrdsTreeViewTab.HOSTS_TAB;
        this.filter = params.get(JRDS_FILTER);
    }

    @Override
    public boolean ping() {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath);
        try {
            return doHttpGet(requestUrl, new AbstractResponseHandler<Boolean>() {
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

    @Override
    public void close() {
        super.close();
        try {
            this.httpClient.close();
        } catch (IOException e) {
            logger.error("Error closing JrdsDataAdapter", e);
        }
    }

    public Collection<String> discoverFilters() throws DataAdapterException {
        Gson gson = new Gson();
        try {
            JsonTree t = gson.fromJson(getJsonTree(treeViewTab.getCommand(), treeViewTab.getArgument()), JsonTree.class);
            return Arrays.stream(t.items).filter(jsonItem -> JRDS_FILTER.equals(jsonItem.type)).map(i -> i.filter).collect(Collectors.toList());
        } catch (JsonParseException e) {
            throw new DataAdapterException("An error occured while parsing the json response to getBindingTree request", e);
        }
    }

    private <T> T doHttpGet(URIBuilder requestUrl, ResponseHandler<T> responseHandler) throws DataAdapterException {
        try (Profiler p = Profiler.start("Executing HTTP request: [" + requestUrl.toString() + "]", logger::trace)) {
            logger.debug(() -> "requestUrl = " + requestUrl);
            HttpGet httpget = new HttpGet(requestUrl.build());
            // Set user-agent pattern to workaround CAS server not proposing SPNEGO authentication unless it thinks agent can handle it.
            httpget.setHeader("User-Agent", "binjr/" + AppEnvironment.getInstance().getManifestVersion() + " (Authenticates like: Firefox/Safari/Internet Explorer)");
            T result = httpClient.execute(httpget, responseHandler);
            if (result == null) {
                throw new ResponseProcessingException("Response entity to \"" + requestUrl.toString() + "\" is null.");
            }
            return result;
        } catch (HttpResponseException e) {
            String msg;
            switch (e.getStatusCode()) {
                case 401:
                    msg = "Authentication failed while trying to access \"" + requestUrl.toString() + "\"";
                    break;
                case 403:
                    msg = "Access to the resource at \"" + requestUrl.toString() + "\" is denied.";
                    break;
                case 404:
                    msg = "The resource at \"" + requestUrl.toString() + "\" could not be found.";
                    break;
                case 500:
                    msg = "A server-side error has occurred while trying to access the resource at \"" + requestUrl.toString() + "\": " + e.getMessage();
                    break;
                default:
                    msg = "Error executing HTTP request \"" + requestUrl.toString() + "\": " + e.getMessage();
                    break;
            }
            throw new SourceCommunicationException(msg, e);
        } catch (ConnectException e) {
            throw new SourceCommunicationException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            throw new SourceCommunicationException("Host \"" + jrdsHost + (jrdsPort > 0 ? ":" + jrdsPort : "") + "\" could not be found.", e);
        } catch (IOException e) {
            throw new SourceCommunicationException("IO error while communicating with host \"" + jrdsHost + (jrdsPort > 0 ? ":" + jrdsPort : "") + "\"", e);
        } catch (URISyntaxException e) {
            throw new SourceCommunicationException("Error building URI for request", e);
        } catch (Exception e) {
            throw new SourceCommunicationException("Unexpected error in HTTP GET", e);
        }
    }

    private void attachNode(TreeItem<TimeSeriesBinding<Double>> tree, String id, Map<String, JsonTree.JsonItem> nodes) throws DataAdapterException {
        JsonTree.JsonItem n = nodes.get(id);
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
                for (JsonTree.JsonItem.JsonTreeRef ref : n.children) {
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

    private String getJsonTree(String tabName, String argName) throws DataAdapterException {
        return getJsonTree(tabName, argName, null);
    }

    private String getJsonTree(String tabName, String argName, String argValue) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/jsontree")
                .addParameter("tab", tabName);
        if (argName != null && argValue != null && argValue.trim().length() > 0) {
            requestUrl.addParameter(argName, argValue);
        }

        String entityString = doHttpGet(requestUrl, new BasicResponseHandler());
        logger.trace(entityString);
        return entityString;
    }

    private Graphdesc getGraphDescriptor(String id) throws DataAdapterException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(jrdsProtocol)
                .setHost(jrdsHost)
                .setPort(jrdsPort)
                .setPath(jrdsPath + "/graphdesc")
                .addParameter("id", id);

        return doHttpGet(requestUrl, response -> {
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
            try (InputStream in = getData(id, now.minusSeconds(300), now, false)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
                    String header = br.readLine();
                    if (header == null || header.isEmpty()) {
                        throw new IOException("CSV File is empty!");
                    }
                    String[] headers = header.split(SEPARATOR);
                    if (headers.length < 1) {
                        throw new ResponseProcessingException("Could not to retrieve data store names for graph id=" + id + ": header line in csv is blank.");
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
            throw new ResponseProcessingException(e);
        }
    }

    private static SSLContext createSslCustomContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, NoSuchProviderException {
        // Load platform specific Trusted CA keystore²
        logger.trace(() -> Arrays.toString(Security.getProviders()));
        KeyStore tks;
        switch (AppEnvironment.getInstance().getOsFamily()) {
            case WINDOWS:
                tks = KeyStore.getInstance("Windows-ROOT", "SunMSCAPI");
                tks.load(null, null);
                break;
            case OSX:
                tks = KeyStore.getInstance("KeychainStore", "Apple");
                tks.load(null, null);
                break;
            case LINUX:
            case UNSUPPORTED:
            default:
                tks = null;
                break;
        }
        return SSLContexts.custom()
                .loadTrustMaterial(tks, null)
                .build();
    }

    private CloseableHttpClient httpClientFactory() throws CannotInitializeDataAdapterException {
        try {
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                    createSslCustomContext(),
                    null,
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            RegistryBuilder<AuthSchemeProvider> schemeProviderBuilder = RegistryBuilder.create();
            schemeProviderBuilder.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory());
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(null, -1, null),
                    new Credentials() {
                        @Override
                        public Principal getUserPrincipal() {
                            return null;
                        }

                        @Override
                        public String getPassword() {
                            return null;
                        }
                    });

            return HttpClients.custom()
                    .setDefaultAuthSchemeRegistry(schemeProviderBuilder.build())
                    .setDefaultCredentialsProvider(credsProvider)
                    .setSSLSocketFactory(csf)
                    .build();
        } catch (IOException | UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException | NoSuchProviderException | KeyManagementException e) {
            throw new CannotInitializeDataAdapterException("Could not initialize adapter to source '" + this.getSourceName() + "': " + e.getMessage(), e);
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
            String filter;
            JsonTreeRef[] children;

            static class JsonTreeRef {
                String _reference;
            }
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
        private final JsonTree.JsonItem n;
        private final TreeItem<TimeSeriesBinding<Double>> newBranch;

        public FilteredViewListener(JsonTree.JsonItem n, TreeItem<TimeSeriesBinding<Double>> newBranch) {
            this.n = n;
            this.newBranch = newBranch;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                try {
                    JsonTree t = new Gson().fromJson(getJsonTree(treeViewTab.getCommand(), JRDS_FILTER, n.name), JsonTree.class);
                    Map<String, JsonTree.JsonItem> m = Arrays.stream(t.items).collect(Collectors.toMap(o -> o.id, (o -> o)));
                    for (JsonTree.JsonItem branch : Arrays.stream(t.items).filter(jsonItem -> JRDS_TREE.equals(jsonItem.type) || JRDS_FILTER.equals(jsonItem.type)).collect(Collectors.toList())) {
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
