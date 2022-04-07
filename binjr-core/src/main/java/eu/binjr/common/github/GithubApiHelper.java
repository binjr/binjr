/*
 *    Copyright 2017-2022 Frederic Thevenet
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

package eu.binjr.common.github;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.binjr.common.io.ProxyConfiguration;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.preferences.UserPreferences;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.*;

/**
 * A series of helper methods to wrap some GitHub APIs
 * See: https://developer.github.com/v3/
 *
 * @author Frederic Thevenet
 */
public class GithubApiHelper {
    private static final Logger logger = Logger.create(GithubApiHelper.class);
    public static final String HTTPS_API_GITHUB_COM = "https://api.github.com";
    protected final CloseableHttpClient httpClient;
    private final URI apiEndpoint;
    protected String userCredentials;
    private static final Gson GSON = new Gson();

    private final static Type ghReleaseArrayType = new TypeToken<ArrayList<GithubRelease>>() {
    }.getType();
    private final static Type ghAssetArrayType = new TypeToken<ArrayList<GithubAsset>>() {
    }.getType();

    private GithubApiHelper() {
        this(null);
    }

    private GithubApiHelper(URI apiEndpoint) {
        this(apiEndpoint, null);
    }

    private GithubApiHelper(URI apiEndpoint, ProxyConfiguration proxyConfig) {
        this.apiEndpoint = Objects.requireNonNullElseGet(apiEndpoint, () -> URI.create(HTTPS_API_GITHUB_COM));
        var builder = HttpClients
                .custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(StandardCookieSpec.STRICT).build());
        if (proxyConfig != null && proxyConfig.enabled()) {
            try {
                builder.setProxy(new HttpHost(proxyConfig.host(), proxyConfig.port()));
                if (proxyConfig.useAuth()) {
                    var credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(
                            new AuthScope(proxyConfig.host(), proxyConfig.port()),
                            new UsernamePasswordCredentials(proxyConfig.login(), proxyConfig.pwd()));
                    builder.setDefaultCredentialsProvider(credsProvider);
                }
            } catch (Exception e) {
                logger.error("Failed to setup http proxy: " + e.getMessage());
                logger.debug(() -> "Stack", e);
            }
        }
        httpClient = builder.build();
    }

    /**
     * Returns the singleton instance of {@link GithubApiHelper}
     *
     * @return the singleton instance of {@link GithubApiHelper}
     */
    private static GithubApiHelper getDefault() {
        return GithubApiHolder.instance;
    }

    /**
     * Initializes a new instance of the {@link ClosableGitHubApiHelper} class.
     *
     * @param apiEndpoint the URI that specifies the API endpoint.
     * @return a new instance of the {@link ClosableGitHubApiHelper} class.
     */
    public static ClosableGitHubApiHelper createCloseable(URI apiEndpoint) {
        return new ClosableGitHubApiHelper(apiEndpoint);
    }

    /**
     * Initializes a new instance of the {@link ClosableGitHubApiHelper} class.
     *
     * @param apiEndpoint        the URI that specifies the API endpoint.
     * @param proxyConfiguration Configuration for http proxy
     * @return a new instance of the {@link ClosableGitHubApiHelper} class.
     */
    public static ClosableGitHubApiHelper createCloseable(URI apiEndpoint, ProxyConfiguration proxyConfiguration) {
        return new ClosableGitHubApiHelper(apiEndpoint, proxyConfiguration);
    }

    /**
     * Initializes a new instance of the {@link ClosableGitHubApiHelper} class.
     *
     * @return a new instance of the {@link ClosableGitHubApiHelper} class.
     */
    public static ClosableGitHubApiHelper createCloseable() {
        return new ClosableGitHubApiHelper();
    }

    /**
     * Returns the latest release from the specified repository.
     *
     * @param owner the repository's owner
     * @param repo  the repository's name
     * @return An {@link Optional} that contains the latest release if it could be found.
     * @throws IOException        if an IO error occurs while communicating with GiHub
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Optional<GithubRelease> getLatestRelease(String owner, String repo) throws IOException, URISyntaxException {
        return getRelease(owner, repo, "latest");
    }

    /**
     * Returns the latest release from the specified repository.
     *
     * @param slug the repository's slug (owner/name)
     * @return An {@link Optional} that contains the latest release if it could be found.
     * @throws IOException        if an IO error occurs while communicating with GiHub
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Optional<GithubRelease> getLatestRelease(String slug) throws IOException, URISyntaxException {
        return getRelease(slug, "latest");
    }

    /**
     * Returns a specific release from the specified repository.
     *
     * @param owner the repository's owner
     * @param repo  the repository's name
     * @param id    the id of the release to retrieve
     * @return An {@link Optional} that contains the specified release if it could be found.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Optional<GithubRelease> getRelease(String owner, String repo, String id) throws IOException, URISyntaxException {
        return getRelease(owner + "/" + repo, id);
    }

    /**
     * Returns a specific release from the specified repository.
     *
     * @param slug the repository's slug (owner/name)
     * @param id   the id of the release to retrieve
     * @return An {@link Optional} that contains the specified release if it could be found.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Optional<GithubRelease> getRelease(String slug, String id) throws IOException, URISyntaxException {
        URIBuilder requestUrl = new URIBuilder(apiEndpoint)
                .setPath("/repos/" + slug + "/releases/" + id);
        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = basicAuthGet(requestUrl.build());
        return Optional.ofNullable(httpClient.execute(httpget,
                response -> GSON.fromJson(EntityUtils.toString(response.getEntity()), GithubRelease.class)));
    }

    /**
     * Returns a list of all release from the specified repository.
     *
     * @param owner the repository's owner
     * @param repo  the repository's name
     * @return a list of all release from the specified repository.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public List<GithubRelease> getAllReleases(String owner, String repo) throws IOException, URISyntaxException {
        return getAllReleases(owner + "/" + repo);
    }

    /**
     * Returns a list of all release from the specified repository.
     *
     * @param slug the repository's slug (owner/name)
     * @return a list of all release from the specified repository.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public List<GithubRelease> getAllReleases(String slug) throws IOException, URISyntaxException {
        URIBuilder requestUrl = new URIBuilder(apiEndpoint)
                .setPath("/repos/" + slug + "/releases")
                .addParameter("per_page", "100");
        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = basicAuthGet(requestUrl.build());
        return httpClient.execute(httpget,
                response -> GSON.fromJson(EntityUtils.toString(response.getEntity()), ghReleaseArrayType));
    }

    /**
     * Returns a list of all {@link GithubAsset} instances associated to a a {@link GithubRelease} instance.
     *
     * @param release The {@link GithubRelease} instance to get the assets from.
     * @return a list of all {@link GithubAsset} instances associated to a a {@link GithubRelease} instance.
     * @throws URISyntaxException if the crafted URI is incorrect.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     */
    public List<GithubAsset> getAssets(GithubRelease release) throws URISyntaxException, IOException {
        logger.debug(() -> "requestUrl = " + release.getAssetsUrl());
        HttpGet httpget = basicAuthGet(release.getAssetsUrl().toURI());
        return httpClient.execute(httpget,
                response -> GSON.fromJson(EntityUtils.toString(response.getEntity()), ghAssetArrayType));
    }

    /**
     * Download the byteArrayTuple of the specified github asset to a temporary location.
     *
     * @param asset the asset to download.
     * @return the {@link Path} where it was downloaded.
     * @throws IOException        if an IO error occurs while attempting to download the file.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Path downloadAsset(GithubAsset asset) throws IOException, URISyntaxException {
        return downloadAsset(asset, Files.createTempDirectory(UserPreferences.getInstance().temporaryFilesRoot.get(), "binjr-updates_"));
    }

    /**
     * Download the byteArrayTuple of the specified github asset into the specified directory.
     *
     * @param asset     the asset to download.
     * @param targetDir the target directory to download the asset to.
     * @return the {@link Path} where it was downloaded.
     * @throws IOException        if an IO error occurs while attempting to download the file.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Path downloadAsset(GithubAsset asset, Path targetDir) throws IOException, URISyntaxException {
        if (!Files.isDirectory(targetDir)) {
            throw new NotDirectoryException(targetDir.toString());
        }
        Path target = targetDir.resolve(asset.getName());
        HttpGet get = new HttpGet(asset.getBrowserDownloadUrl().toURI());
        return httpClient.execute(get, response -> {
            try (var fos = new FileOutputStream(target.toFile())) {
                response.getEntity().writeTo(fos);
            }
            return target;
        });
    }

    /**
     * Set the user credentials for api authentication.
     *
     * @param username the user name
     * @param token    a github personal access token.
     */
    public void setUserCredentials(String username, String token) {
        if (username != null && token != null && username.trim().length() > 0) {
            this.userCredentials = Base64.getEncoder().encodeToString((username + ":" + token).getBytes(StandardCharsets.UTF_8));
        } else {
            this.userCredentials = null;
        }
    }

    protected HttpGet basicAuthGet(URI requestUri) {
        HttpGet httpget = new HttpGet(requestUri);
        if (userCredentials != null) {
            httpget.addHeader("Authorization", "Basic " + userCredentials);
        }
        return httpget;
    }

    private static class GithubApiHolder {
        private final static GithubApiHelper instance = new GithubApiHelper();
    }

    /**
     * An instance of {@link GithubApiHelper} that implements {@link Closeable}
     */
    public static class ClosableGitHubApiHelper extends GithubApiHelper implements Closeable {
        public ClosableGitHubApiHelper() {
        }

        public ClosableGitHubApiHelper(URI apiEndpoint) {
            super(apiEndpoint);
        }

        public ClosableGitHubApiHelper(URI apiEndpoint, ProxyConfiguration proxyConfiguration) {
            super(apiEndpoint, proxyConfiguration);
        }

        @Override
        public void close() throws IOException {
            httpClient.close();
        }
    }
}
