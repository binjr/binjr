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

package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.codec.Decoder;
import eu.fthevenet.binjr.data.exceptions.*;
import eu.fthevenet.binjr.preferences.AppEnvironment;
import eu.fthevenet.util.logging.Profiler;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
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
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides a base on which to implement {@link DataAdapter} instances that communicate with sources via the HTTP protocol.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class HttpDataAdapterBase<T, A extends Decoder<T>> extends SimpleCachingDataAdapter<T, A> {
    private static final Logger logger = LogManager.getLogger(HttpDataAdapterBase.class);
    protected static final String BASE_ADDRESS_PARAM_NAME = "baseUri";
    private final CloseableHttpClient httpClient;
    private URL baseAddress;

    /**
     * Creates a new instance of the {@link HttpDataAdapterBase} class.
     *
     * @throws CannotInitializeDataAdapterException if the {@link DataAdapter} cannot be initialized.
     */
    public HttpDataAdapterBase() throws CannotInitializeDataAdapterException {
        super();
        httpClient = httpClientFactory();
    }

    /**
     * Creates a new instance of the {@link HttpDataAdapterBase} class for the specified base address.
     *
     * @param baseAddress the source's base address
     * @throws CannotInitializeDataAdapterException if the {@link DataAdapter} cannot be initialized.
     */
    public HttpDataAdapterBase(URL baseAddress) throws CannotInitializeDataAdapterException {
        super();
        this.baseAddress = baseAddress;
        httpClient = httpClientFactory();
    }

    @Override
    public byte[] onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException {
        return doHttpGet(craftFetchUri(path, begin, end), new AbstractResponseHandler<byte[]>() {
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
                        throw new InvalidAdapterParameterException("Parameter " + BASE_ADDRESS_PARAM_NAME + " is missing in adapter " + getSourceName());
                    }
                    try {
                        return baseAddress = new URL(s);
                    } catch (MalformedURLException e) {
                        throw new InvalidAdapterParameterException("Value provided for parameter " + BASE_ADDRESS_PARAM_NAME + " is not valid in adapter " + getSourceName(), e);
                    }
                });
    }

    @Override
    public void close() {
        super.close();
        try {
            this.httpClient.close();
        } catch (IOException e) {
            logger.error("Error closing HttpDataAdapterBase", e);
        }
    }
    //endregion

    protected <R> R doHttpGet(URI requestUri, ResponseHandler<R> responseHandler) throws DataAdapterException {
        try (Profiler p = Profiler.start("Executing HTTP request: [" + requestUri.toString() + "]", logger::trace)) {
            logger.debug(() -> "requestUri = " + requestUri);
            HttpGet httpget = new HttpGet(requestUri);
            // Set user-agent pattern to workaround CAS server not proposing SPNEGO authentication unless it thinks agent can handle it.
            httpget.setHeader("User-Agent", "binjr/" + AppEnvironment.getInstance().getVersion() + " (Authenticates like: Firefox/Safari/Internet Explorer)");
            R result = httpClient.execute(httpget, responseHandler);
            if (result == null) {
                throw new FetchingDataFromAdapterException("Response entity to \"" + requestUri.toString() + "\" is null.");
            }
            return result;
        } catch (HttpResponseException e) {
            String msg;
            switch (e.getStatusCode()) {
                case 401:
                    msg = "Authentication failed while trying to access \"" + requestUri.toString() + "\"";
                    break;
                case 403:
                    msg = "Access to the resource at \"" + requestUri.toString() + "\" is denied.";
                    break;
                case 404:
                    msg = "The resource at \"" + requestUri.toString() + "\" could not be found.";
                    break;
                case 500:
                    msg = "A server-side error has occurred while trying to access the resource at \"" + requestUri.toString() + "\": " + e.getMessage();
                    break;
                default:
                    msg = "Error executing HTTP request \"" + requestUri.toString() + "\": " + e.getMessage();
                    break;
            }
            throw new SourceCommunicationException(msg, e);
        } catch (ConnectException e) {
            throw new SourceCommunicationException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            throw new SourceCommunicationException("Host \"" + baseAddress.getHost() + (baseAddress.getPort() > 0 ? ":" + baseAddress.getPort() : "") + "\" could not be found.", e);
        } catch (IOException e) {
            throw new SourceCommunicationException("IO error while communicating with host \"" + baseAddress.getHost() + (baseAddress.getPort() > 0 ? ":" + baseAddress.getPort() : "") + "\"", e);
        } catch (Exception e) {
            throw new SourceCommunicationException("Unexpected error in HTTP GET", e);
        }
    }

    protected static SSLContext createSslCustomContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, NoSuchProviderException {
        // Load platform specific Trusted CA keystore
        logger.trace(() -> "Available Java Security providers: " + Arrays.toString(Security.getProviders()));
        KeyStore tks = null;
        try {
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
                    break;
            }
        } catch (Exception e) {
            logger.debug("Error locating OS specific keystore", e);
        }

        return SSLContexts.custom().loadTrustMaterial(tks, null).build();
    }

    protected CloseableHttpClient httpClientFactory() throws CannotInitializeDataAdapterException {
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
        } catch (Exception e) {
            throw new CannotInitializeDataAdapterException("Could not initialize adapter to source '" + this.getSourceName() + "': " + e.getMessage(), e);
        }
    }

    protected URI craftRequestUri(String path, List<NameValuePair> params) throws SourceCommunicationException {
        try {
            URIBuilder builder = new URIBuilder(getBaseAddress().toURI()).setPath(getBaseAddress().getPath() + path);
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
}
