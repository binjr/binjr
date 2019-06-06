/*
 *    Copyright 2017-2018 Frederic Thevenet
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private static final Logger logger = LogManager.getLogger(GithubApiHelper.class);
    public static final String GITHUB_API_HOSTNAME = "api.github.com";
    public static final String URL_PROTOCOL = "https";
    protected String userCredentials;
    protected final CloseableHttpClient httpClient;
    protected Gson gson;

    private static class GithubApiHolder {
        private final static GithubApiHelper instance = new GithubApiHelper();
    }

    private GithubApiHelper() {
        gson = new Gson();
        httpClient = HttpClients
                .custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
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
     * @return a new instance of the {@link ClosableGitHubApiHelper} class.
     */
    public static ClosableGitHubApiHelper createCloseable() {
        return new ClosableGitHubApiHelper();
    }

    public static class ClosableGitHubApiHelper extends GithubApiHelper implements Closeable {
        @Override
        public void close() throws IOException {
            httpClient.close();
        }
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
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(URL_PROTOCOL)
                .setHost(GITHUB_API_HOSTNAME)
                .setPath("/repos/" + slug + "/releases/" + id);
        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = basicAuthGet(requestUrl.build());
        return Optional.ofNullable(httpClient.execute(httpget, new AbstractResponseHandler<>() {
            @Override
            public GithubRelease handleResponse(HttpResponse response) throws HttpResponseException, IOException {
                logger.debug(() -> Arrays.toString(response.getHeaders("X-RateLimit-Limit")) + " " +
                        Arrays.toString(response.getHeaders("X-RateLimit-Remaining")));
                return super.handleResponse(response);
            }

            @Override
            public GithubRelease handleEntity(HttpEntity entity) throws IOException {
                return gson.fromJson(EntityUtils.toString(entity), GithubRelease.class);
            }
        }));
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
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(URL_PROTOCOL)
                .setHost(GITHUB_API_HOSTNAME)
                .setPath("/repos/" + slug + "/releases")
                .addParameter("per_page", "100");
        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = basicAuthGet(requestUrl.build());

        return httpClient.execute(httpget, new AbstractResponseHandler<>() {
            @Override
            public List<GithubRelease> handleEntity(HttpEntity entity) throws IOException {
                return gson.fromJson(EntityUtils.toString(entity), new TypeToken<ArrayList<GithubRelease>>() {
                }.getType());
            }
        });
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

        return httpClient.execute(httpget, new AbstractResponseHandler<>() {
            @Override
            public List<GithubAsset> handleEntity(HttpEntity entity) throws IOException {
                return gson.fromJson(EntityUtils.toString(entity), new TypeToken<ArrayList<GithubAsset>>() {
                }.getType());
            }
        });
    }

    /**
     * Download the payload of the specified github asset to a temporary location.
     *
     * @param asset the asset to download.
     * @return the {@link Path} where it was downloaded.
     * @throws IOException        if an IO error occurs while attempting to download the file.
     * @throws URISyntaxException if the crafted URI is incorrect.
     */
    public Path downloadAsset(GithubAsset asset) throws IOException, URISyntaxException {
        return downloadAsset(asset, Files.createTempDirectory("binjr-updates_"));
    }

    /**
     * Download the payload of the specified github asset into the specified directory.
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
}
