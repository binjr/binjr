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

package eu.fthevenet.util.github;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A series of helper method to wrap GitHub API
 * See: https://developer.github.com/v3/
 *
 * @author Frederic Thevenet
 */
public class GithubApi {
    private static final Logger logger = LogManager.getLogger(GithubApi.class);
    public static final String API_GITHUB_COM = "api.github.com";
    public static final String HTTPS = "https";
    private final CloseableHttpClient httpClient;
    private Gson gson;

    private static class GithubApiHolder {
        private final static GithubApi instance = new GithubApi();
    }

    private GithubApi() {
        gson = new Gson();
        httpClient = HttpClients.createDefault();
    }

    /**
     * Returns the singleton instance of {@link GithubApi}
     *
     * @return the singleton instance of {@link GithubApi}
     */
    public static GithubApi getInstance() {
        return GithubApiHolder.instance;
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
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(HTTPS)
                .setHost(API_GITHUB_COM)
                .setPath("/repos/" + owner + "/" + repo + "/releases/" + id);

        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = new HttpGet(requestUrl.build());
        GithubRelease release = httpClient.execute(httpget, new AbstractResponseHandler<GithubRelease>() {
            @Override
            public GithubRelease handleEntity(HttpEntity entity) throws IOException {
                return gson.fromJson(EntityUtils.toString(entity), GithubRelease.class);
            }
        });
        return release != null ? Optional.of(release) : Optional.empty();
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
    public List<GithubRelease> getReleases(String owner, String repo) throws IOException, URISyntaxException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(HTTPS)
                .setHost(API_GITHUB_COM)
                .setPath("/repos/" + owner + "/" + repo + "/releases");

        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = new HttpGet(requestUrl.build());
        return httpClient.execute(httpget, new AbstractResponseHandler<List<GithubRelease>>() {
            @Override
            public List<GithubRelease> handleEntity(HttpEntity entity) throws IOException {
                return gson.fromJson(EntityUtils.toString(entity), new TypeToken<ArrayList<GithubRelease>>() {
                }.getType());
            }
        });
    }
}
