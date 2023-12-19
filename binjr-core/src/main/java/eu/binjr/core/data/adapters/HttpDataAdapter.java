/*
 *    Copyright 2017-2021 Frederic Thevenet
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

package eu.binjr.core.data.adapters;

import eu.binjr.common.io.ProxyConfiguration;
import eu.binjr.common.io.SSLContextUtils;
import eu.binjr.common.io.SSLCustomContextException;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.exceptions.*;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodySubscribers;

/**
 * This class provides a base on which to implement {@link DataAdapter} instances that communicate with sources via the HTTP protocol.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class HttpDataAdapter<T> extends SimpleCachingDataAdapter<T> {
    protected static final String BASE_ADDRESS_PARAM_NAME = "baseUri";
    private final static Pattern uriSchemePattern = Pattern.compile("^[a-zA-Z]*://");
    private static final Logger logger = Logger.create(HttpDataAdapter.class);
    public static final String APPLICATION_JSON = "application/json";
    public static final String USER_AGENT_STRING = AppEnvironment.APP_NAME +
            "/" + AppEnvironment.getInstance().getVersion() +
            " (Authenticates like: Firefox/Safari/Internet Explorer)";
    private final HttpClient httpClient;
    private URL baseAddress;

    /**
     * Creates a new instance of the {@link HttpDataAdapter} class.
     *
     * @throws CannotInitializeDataAdapterException if the {@link DataAdapter} cannot be initialized.
     */
    public HttpDataAdapter() throws CannotInitializeDataAdapterException {
        super();
        httpClient = httpClientFactory();
    }

    /**
     * Creates a new instance of the {@link HttpDataAdapter} class for the specified base address.
     *
     * @param baseAddress the source's base address
     * @throws CannotInitializeDataAdapterException if the {@link DataAdapter} cannot be initialized.
     */
    public HttpDataAdapter(URL baseAddress) throws CannotInitializeDataAdapterException {
        super();
        this.baseAddress = baseAddress;
        httpClient = httpClientFactory();
    }

    @Override
    public byte[] onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException {
        return doHttpGet(craftFetchUri(path, begin, end), r -> BodySubscribers.ofByteArray());
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(BASE_ADDRESS_PARAM_NAME, baseAddress.toString());
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        baseAddress = validateParameter(params, BASE_ADDRESS_PARAM_NAME,
                s -> {
                    if (s == null) {
                        throw new InvalidAdapterParameterException("Parameter " + BASE_ADDRESS_PARAM_NAME +
                                " is missing in adapter " + getSourceName());
                    }
                    try {
                        return baseAddress = new URL(s);
                    } catch (MalformedURLException e) {
                        throw new InvalidAdapterParameterException("Value provided for parameter " +
                                BASE_ADDRESS_PARAM_NAME + " is not valid in adapter " + getSourceName(), e);
                    }
                });
    }

    @Override
    public void close() {
        try {
            this.httpClient.close();
        } catch (Exception e) {
            logger.error("Error closing HttpDataAdapter", e);
        }
        super.close();
    }

    protected String doHttpGetJson(URI requestUri) throws DataAdapterException {
        return doHttpGet(requestUri, responseInfo -> BodySubscribers.mapping(
                BodySubscribers.ofString(StandardCharsets.UTF_8),
                s -> {
                    String contentType = responseInfo.headers().firstValue("Content-Type")
                            .orElseThrow(() -> new RuntimeException("No content type specified"));

                    if (!APPLICATION_JSON.equalsIgnoreCase(contentType.split(";")[0].trim())) {
                        throw new RuntimeException("Invalid content type: received: '" +
                                s + "', expected: '" + APPLICATION_JSON + "')");
                    }
                    return s;
                }));
    }

    protected <R> R doHttpGet(URI url, HttpResponse.BodyHandler<R> bodyHandler) throws DataAdapterException {
        try (Profiler p = Profiler.start("Executing HTTP request: [" + url.toString() + "]", logger::perf)) {
            var httpGet = HttpRequest.newBuilder()
                    .GET()
                    .setHeader("User-Agent", USER_AGENT_STRING)
                    .uri(url).build();
            logger.debug(() -> "requestUri = " + url);
            HttpResponse<R> response = httpClient.send(httpGet, bodyHandler);
            if (response == null) {
                throw new FetchingDataFromAdapterException("Invalid response to \"" + url + "\"");
            } else if (response.statusCode() >= 300) {
                String msg = switch (response.statusCode()) {
                    case 401 -> "Authentication failed while trying to access \"" + url + "\"";
                    case 403 -> "Access to the resource at \"" + url + "\" is denied.";
                    case 404 -> "The resource at \"" + url + "\" could not be found.";
                    case 407 -> "Proxy authentication is required to access resource at \"" + url + "\"";
                    case 500 -> "A server-side error has occurred while accessing the resource at \"" + url + "\"";
                    case 501 -> "Request method is not supported by the server at \"" + url + "\"";
                    case 502 -> "Bad gateway at \"" + url + "\"";
                    case 503 -> "Service unavailable at \"" + url + "\"";
                    case 504 -> "Gateway timeout at \"" + url + "\"";
                    default -> "Error executing HTTP request \"" + url + "\" (Status=" + response.statusCode() + ")";
                };
                throw new SourceCommunicationException(msg);
            }
            return response.body();
        } catch (ConnectException e) {
            throw new SourceCommunicationException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            throw new SourceCommunicationException("Host \"" + baseAddress.getHost() + (baseAddress.getPort() > 0 ? ":"
                    + baseAddress.getPort() : "") + "\" could not be found.", e);
        } catch (SSLHandshakeException e) {
            throw new SourceCommunicationException("An error occurred while negotiating connection security: " +
                    e.getMessage(), e);
        } catch (IOException e) {
            throw new SourceCommunicationException("IO error while communicating with host \"" + baseAddress.getHost() +
                    (baseAddress.getPort() > 0 ? ":" + baseAddress.getPort() : "") + "\": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SourceCommunicationException("Unexpected error in HTTP GET: " + e.getMessage(), e);
        }
    }

    protected HttpClient httpClientFactory() throws CannotInitializeDataAdapterException {
        try {
            var userPrefs = UserPreferences.getInstance();
            var builder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
            try {
                builder.sslContext(SSLContextUtils.withPlatformKeystore());
            } catch (SSLCustomContextException e) {
                logger.error("Error creating SSL context for GitHub helper:" + e.getMessage());
                logger.debug("Stacktrace", e);
            }
            if (userPrefs.enableHttpProxy.get()) {
                try {
                    var proxyConfig = new ProxyConfiguration(
                            userPrefs.enableHttpProxy.get(),
                            userPrefs.httpProxyHost.get(),
                            userPrefs.httpProxyPort.get().intValue(),
                            userPrefs.useHttpProxyAuth.get(),
                            userPrefs.httpProxyLogin.get(),
                            userPrefs.httpProxyPassword.get().toPlainText().toCharArray());
                    var proxySelector = ProxySelector.of(InetSocketAddress.createUnresolved(proxyConfig.host(), proxyConfig.port()));
                    if (proxyConfig.useAuth()) {
                        builder.authenticator(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                if (getRequestorType().equals(RequestorType.PROXY)) {
                                    return new PasswordAuthentication(proxyConfig.login(), proxyConfig.pwd());
                                } else {
                                    return null;
                                }
                            }
                        });
                    }
                    builder.proxy(proxySelector);
                } catch (Exception e) {
                    logger.error("Failed to setup http proxy: " + e.getMessage());
                    logger.debug(() -> "Stack", e);
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new CannotInitializeDataAdapterException("Could not initialize adapter to source '" +
                    this.getSourceName() + "': " + e.getMessage(), e);
        }
    }

    protected URI craftRequestUri(String path, List<UriParameter> params) throws SourceCommunicationException {
        Objects.requireNonNull(path);
        try {
            List<String> res = new ArrayList<>(Arrays.asList(getBaseAddress().getPath().split("/")));
            res.addAll(Arrays.asList(path.split("/")));
            return getBaseAddress().toURI().resolve(res.stream().filter(s -> !s.isEmpty()).reduce("", (p, e) -> p + "/" + e) +
                    params.stream().map(UriParameter::encoded).collect(Collectors.joining("&", path.contains("?") ? "&" : "?", "")));
        } catch (URISyntaxException e) {
            throw new SourceCommunicationException("Error building URI for request", e);
        }
    }

    protected URI craftRequestUri(String path, UriParameter... params) throws SourceCommunicationException {
        return craftRequestUri(path, params != null ? Arrays.asList(params) : null);
    }

    protected abstract URI craftFetchUri(String path, Instant begin, Instant end) throws DataAdapterException;

    /**
     * Returns the source's base address
     *
     * @return the source's base address
     */
    public URL getBaseAddress() {
        return baseAddress;
    }

    /**
     * Sets the source's base address
     *
     * @param baseAddress the source's base address
     */
    public void setBaseAddress(URL baseAddress) {
        this.baseAddress = baseAddress;
    }

    /**
     * Pings the data source
     *
     * @return true if the data source responded to ping request, false otherwise.
     */
    public boolean ping() {
        try {
            return doHttpGet(craftRequestUri(""),
                    responseInfo -> BodySubscribers.mapping(
                            BodySubscribers.discarding(),
                            unused -> responseInfo.statusCode() < 300));
        } catch (Exception e) {
            logger.debug(() -> "Ping failed", e);
            return false;
        }
    }

    /**
     * Infers a URL from a user provided string (adds protocol if not present).
     *
     * @param address the spring to generate a URL from/
     * @return the URL
     * @throws CannotInitializeDataAdapterException if an error occurs while forming a URL.
     */
    public static URL urlFromString(String address) throws CannotInitializeDataAdapterException {
        try {
            // Detect if URL protocol is present. If not, assume http.
            if (!uriSchemePattern.matcher(address).find()) {
                address = "http://" + address;
            }
            URL url = new URL(address.trim());
            if (url.getHost().trim().isEmpty()) {
                throw new CannotInitializeDataAdapterException("Malformed URL: no host");
            }
            return url;
        } catch (MalformedURLException e) {
            throw new CannotInitializeDataAdapterException("Malformed URL: " + e.getMessage(), e);
        }
    }

}
