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

import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.exceptions.*;
import eu.binjr.core.preferences.AppEnvironment;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.auth.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.*;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.Security;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

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
    private final CloseableHttpClient httpClient;
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

    protected static SSLContext createSslCustomContext() {
        // Load platform specific Trusted CA keystore
        String keystoreType;
        switch (AppEnvironment.getInstance().getOsFamily()) {
            case WINDOWS:
                keystoreType = "Windows-ROOT";
                break;
            case OSX:
                keystoreType = "KeychainStore";
                break;
            case LINUX:
            case UNSUPPORTED:
            default:
                logger.trace("No attempt to load system keystore on OS=" + AppEnvironment.getInstance().getOsFamily());
                return SSLContexts.createSystemDefault();
        }
        try {
            logger.trace(() -> "Available Java Security providers: " + Arrays.toString(Security.getProviders()));
            KeyStore tks = KeyStore.getInstance(keystoreType);
            tks.load(null, null);
            return SSLContexts.custom().loadTrustMaterial(tks, null).build();
        } catch (KeyStoreException e) {
            logger.debug("Could not find the requested OS specific keystore", e);
        } catch (Exception e) {
            logger.debug("Error loading OS specific keystore", e);
        }
        return SSLContexts.createSystemDefault();
    }

    @Override
    public byte[] onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException {
        return doHttpGet(craftFetchUri(path, begin, end), new AbstractHttpClientResponseHandler<>() {
            @Override
            public byte[] handleEntity(HttpEntity entity) throws IOException {
                return EntityUtils.toByteArray(entity);
            }
        });
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
        } catch (IOException e) {
            logger.error("Error closing HttpDataAdapter", e);
        }
        super.close();
    }

    protected String doHttpGetJson(URI requestUri) throws DataAdapterException {
        return doHttpGet(requestUri, response -> {
            var entity = response.getEntity();
            if (response.getCode() >= 300) {
                EntityUtils.consume(entity);
                throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
            }
            if (entity == null) {
                throw new NoHttpResponseException("Invalid response to \"" + requestUri + "\": null");
            }
            String contentType = entity.getContentType();
            if (contentType == null || contentType.isBlank()) {
                throw new ClientProtocolException("No content type specified");
            }
            if (!APPLICATION_JSON.equalsIgnoreCase(contentType.split(";")[0].trim())) {
                throw new ClientProtocolException("Invalid content type: received: '" +
                        entity.getContentType() +
                        "', expected: '" + APPLICATION_JSON + "')");
            }
            return EntityUtils.toString(entity);
        });
    }

    protected <R> R doHttpGet(URI requestUri, HttpClientResponseHandler<R> responseHandler) throws DataAdapterException {
        try (Profiler p = Profiler.start("Executing HTTP request: [" + requestUri.toString() + "]", logger::perf)) {
            logger.debug(() -> "requestUri = " + requestUri);
            R result = httpClient.execute(new HttpGet(requestUri), responseHandler);
            if (result == null) {
                throw new FetchingDataFromAdapterException("Invalid response to \"" + requestUri + "\"");
            }
            return result;
        } catch (HttpResponseException e) {
            String msg = switch (e.getStatusCode()) {
                case 401 -> "Authentication failed while trying to access \"" + requestUri + "\"";
                case 403 -> "Access to the resource at \"" + requestUri + "\" is denied.";
                case 404 -> "The resource at \"" + requestUri + "\" could not be found.";
                case 500 -> "A server-side error has occurred while trying to access the resource at \""
                        + requestUri + "\": " + e.getMessage();
                default -> "Error executing HTTP request \"" + requestUri + "\": " + e.getMessage();
            };
            throw new SourceCommunicationException(msg, e);
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

    protected CloseableHttpClient httpClientFactory() throws CannotInitializeDataAdapterException {
        try {
            var schemeFactoryRegistry = RegistryBuilder.<AuthSchemeFactory>create()
                    .register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.NTLM, NTLMSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT)
                    .register(StandardAuthScheme.SPNEGO, new SPNegoSchemeFactory(
                            KerberosConfig.custom()
                                    .setRequestDelegCreds(KerberosConfig.Option.DEFAULT)
                                    .setStripPort(KerberosConfig.Option.DEFAULT)
                                    .setUseCanonicalHostname(KerberosConfig.Option.DEFAULT)
                                    .build(),
                            SystemDefaultDnsResolver.INSTANCE))
                    .build();

            var credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(null, -1),
                    new Credentials() {
                        @Override
                        public Principal getUserPrincipal() {
                            return null;
                        }

                        @Override
                        public char[] getPassword() {
                            return null;
                        }
                    });

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(createSslCustomContext())
                            .setTlsVersions(TLS.V_1_3, TLS.V_1_2)
                            .build())
                    .setDefaultSocketConfig(SocketConfig.custom()
                            .setSoTimeout(Timeout.ofSeconds(5))
                            .build())
                    .setConnectionTimeToLive(TimeValue.ofMinutes(1))
                    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .setMaxConnPerRoute(16)
                    .setMaxConnTotal(100)
                    .setConnectionTimeToLive(TimeValue.ofMinutes(1L))
                    .build();

            return HttpClients.custom()
                    .setUserAgent(AppEnvironment.APP_NAME + "/" +
                            AppEnvironment.getInstance().getVersion() +
                            " (Authenticates like: Firefox/Safari/Internet Explorer)")
                    .setConnectionManager(connectionManager)
                    .setDefaultAuthSchemeRegistry(schemeFactoryRegistry)
                    .setDefaultCredentialsProvider(credsProvider)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(Timeout.ofSeconds(5))
                            .setResponseTimeout(Timeout.ofSeconds(5))
                            .setCookieSpec(StandardCookieSpec.STRICT)
                            .build())
                    .build();

        } catch (Exception e) {
            throw new CannotInitializeDataAdapterException("Could not initialize adapter to source '" +
                    this.getSourceName() + "': " + e.getMessage(), e);
        }
    }

    protected URI craftRequestUri(String path, List<NameValuePair> params) throws SourceCommunicationException {
        Objects.requireNonNull(path);
        try {
            List<String> res = new ArrayList<>(Arrays.asList(getBaseAddress().getPath().split("/")));
            res.addAll(Arrays.asList(path.split("/")));
            String sanitizedPath = res.stream().filter(s -> !s.isEmpty()).reduce("", (p, e) -> p + "/" + e);
            URIBuilder builder = new URIBuilder(getBaseAddress().toURI().resolve(sanitizedPath));
            if (params != null) {
                builder.addParameters(params);
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new SourceCommunicationException("Error building URI for request", e);
        }
    }

    protected URI craftRequestUri(String path, NameValuePair... params) throws SourceCommunicationException {
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
            return doHttpGet(craftRequestUri(""), response -> {
                HttpEntity entity = null;
                try {
                    entity = response.getEntity();
                    return response.getCode() < 300 && entity != null;
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }
            });
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
