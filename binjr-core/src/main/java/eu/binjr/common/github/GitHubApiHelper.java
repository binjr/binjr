/*
 *    Copyright 2017-2023 Frederic Thevenet
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
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.io.ProxyConfiguration;
import eu.binjr.common.io.SSLContextUtils;
import eu.binjr.common.io.SSLCustomContextException;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.DoubleProperty;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * A series of helper methods to wrap some GitHub APIs
 * See: https://developer.github.com/v3/
 *
 * @author Frederic Thevenet
 */
public class GitHubApiHelper implements Closeable {
    private static final Logger logger = Logger.create(GitHubApiHelper.class);
    public static final String HTTPS_API_GITHUB_COM = "https://api.github.com";
    protected final HttpClient httpClient;
    private final URI apiEndpoint;
    protected String userCredentials;
    private static final Gson GSON = new Gson();

    private final static Type ghReleaseArrayType = new TypeToken<ArrayList<GithubRelease>>() {
    }.getType();
    private final static Type ghAssetArrayType = new TypeToken<ArrayList<GithubAsset>>() {
    }.getType();

    private GitHubApiHelper() {
        this(null);
    }

    private GitHubApiHelper(URI apiEndpoint) {
        this(apiEndpoint, null, null, null);
    }

    private GitHubApiHelper(URI apiEndpoint, ProxyConfiguration proxyConfig, String userName, String token) {
        this.apiEndpoint = Objects.requireNonNullElseGet(apiEndpoint, () -> URI.create(HTTPS_API_GITHUB_COM));
        HttpClient.Builder builder = null;

        builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        try {
            builder.sslContext(SSLContextUtils.withPlatformKeystore());
        } catch (SSLCustomContextException e) {
            logger.error("Error creating SSL context for GitHub helper:" + e.getMessage());
            logger.debug("Stacktrace", e);
        }
        if (proxyConfig != null && proxyConfig.enabled()) {
            try {
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
        setUserCredentials(userName, token);
        httpClient = builder.build();
    }


    /**
     * Initializes a new instance of the {@link GitHubApiHelper} class.
     *
     * @param apiEndpoint the URI that specifies the API endpoint.
     * @return a new instance of the {@link GitHubApiHelper} class.
     */
    public static GitHubApiHelper of(URI apiEndpoint) {
        return new GitHubApiHelper(apiEndpoint);
    }

    /**
     * Initializes a new instance of the {@link GitHubApiHelper} class.
     *
     * @param apiEndpoint        the URI that specifies the API endpoint.
     * @param proxyConfiguration Configuration for http proxy
     * @return a new instance of the {@link GitHubApiHelper} class.
     */
    public static GitHubApiHelper of(URI apiEndpoint,
                                     ProxyConfiguration proxyConfiguration,
                                     String userName,
                                     String token) {
        return new GitHubApiHelper(apiEndpoint, proxyConfiguration, userName, token);
    }

    /**
     * Initializes a new instance of the {@link GitHubApiHelper} class.
     *
     * @return a new instance of the {@link GitHubApiHelper} class.
     */
    public static GitHubApiHelper createDefault() {
        return new GitHubApiHelper();
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
    public Optional<GithubRelease> getLatestRelease(String owner, String repo) throws IOException, URISyntaxException, InterruptedException {
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
    public Optional<GithubRelease> getLatestRelease(String slug) throws IOException, URISyntaxException, InterruptedException {
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
    public Optional<GithubRelease> getRelease(String owner, String repo, String id) throws IOException, URISyntaxException, InterruptedException {
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
    public Optional<GithubRelease> getRelease(String slug, String id) throws IOException, URISyntaxException, InterruptedException {
        var requestUrl = URI.create(apiEndpoint + "/repos/" + slug + "/releases/" + id);
        logger.debug(() -> "requestUrl = " + requestUrl);
        var httpget = basicAuthGet(requestUrl);
        var response = httpClient.send(httpget, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get release from " +
                    requestUrl +
                    "(HTTP status: " + response.statusCode() + ")");
        }
        return Optional.ofNullable(GSON.fromJson(response.body(), GithubRelease.class));
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
    public List<GithubRelease> getAllReleases(String owner, String repo) throws IOException, URISyntaxException, InterruptedException {
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
    public List<GithubRelease> getAllReleases(String slug) throws IOException, URISyntaxException, InterruptedException {
        var requestUrl = URI.create(apiEndpoint +
                "/repos/" + slug + "/releases?per_page=100");
        logger.debug(() -> "requestUrl = " + requestUrl);
        var httpget = basicAuthGet(requestUrl);
        var response = httpClient.send(httpget, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get release list from " +
                    requestUrl +
                    "(HTTP status: " + response.statusCode() + ")");
        }
        return GSON.fromJson(response.body(), ghReleaseArrayType);
    }

    /**
     * Returns a list of all {@link GithubAsset} instances associated to a a {@link GithubRelease} instance.
     *
     * @param release The {@link GithubRelease} instance to get the assets from.
     * @return a list of all {@link GithubAsset} instances associated to a a {@link GithubRelease} instance.
     * @throws URISyntaxException if the crafted URI is incorrect.
     * @throws IOException        if an IO error occurs while communicating with GitHub.
     */
    public List<GithubAsset> getAssets(GithubRelease release) throws URISyntaxException, IOException, InterruptedException {
        logger.debug(() -> "requestUrl = " + release.getAssetsUrl());
        var httpget = basicAuthGet(release.getAssetsUrl().toURI());
        var response = httpClient.send(httpget, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download asset list from " +
                    release.getAssetsUrl() +
                    "(HTTP status: " + response.statusCode() + ")");
        }
        return GSON.fromJson(response.body(), ghAssetArrayType);
    }

    /**
     * Download the byteArrayTuple of the specified github asset to a temporary location.
     *
     * @param asset the asset to download.
     * @return the {@link Path} where it was downloaded.
     * @throws IOException        if an IO error occurs while attempting to download the file.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Path downloadAsset(GithubAsset asset) throws IOException, URISyntaxException, InterruptedException {
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
    public Path downloadAsset(GithubAsset asset, Path targetDir) throws IOException, URISyntaxException, InterruptedException {
        return downloadAsset(asset, targetDir, null);
    }

    /**
     * Download the byteArrayTuple of the specified github asset into the specified directory.
     *
     * @param asset     the asset to download.
     * @param targetDir the target directory to download the asset to.
     * @param progress  A {@link DoubleProperty} used to report download progression.
     * @return the {@link Path} where it was downloaded.
     * @throws IOException        if an IO error occurs while attempting to download the file.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Path downloadAsset(GithubAsset asset, Path targetDir, DoubleProperty progress) throws IOException, URISyntaxException, InterruptedException {
        if (!Files.isDirectory(targetDir)) {
            throw new NotDirectoryException(targetDir.toString());
        }
        var assetSize = asset.getSize();
        var get = HttpRequest.newBuilder()
                .uri(asset.getBrowserDownloadUrl().toURI())
                .build();
        Path target = targetDir.resolve(asset.getName());
        var response = httpClient.send(get, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download asset from " +
                    asset.getBrowserDownloadUrl() +
                    "(HTTP status: " + response.statusCode() + ")");
        }
        try (var in = response.body()) {
            if (progress == null) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                try (var out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                    IOUtils.copyStreams(in, out, assetSize, progress);
                }
            }
        }
        return target;
    }

    /**
     * Set the user credentials for api authentication.
     *
     * @param username the user name
     * @param token    a github personal access token.
     */
    public void setUserCredentials(String username, String token) {
        if (username != null && token != null && !username.trim().isEmpty()) {
            this.userCredentials = Base64.getEncoder().encodeToString((username + ":" + token).getBytes(StandardCharsets.UTF_8));
        } else {
            this.userCredentials = null;
        }
    }

    protected HttpRequest basicAuthGet(URI requestUri) {
        var requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(requestUri);
        if (userCredentials != null) {
            requestBuilder.header("Authorization", "Basic " + userCredentials);
        }
        return requestBuilder.build();
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

}
