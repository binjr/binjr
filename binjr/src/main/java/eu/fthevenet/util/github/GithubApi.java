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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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

    public static GithubApi getInstance() {
        return GithubApiHolder.instance;
    }

    public GithubRelease getLatestRelease(String owner, String repo) throws IOException, URISyntaxException {
        return getRelease(owner, repo, "latest");
    }


    public GithubRelease getRelease(String owner, String repo, String id) throws IOException, URISyntaxException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(HTTPS)
                .setHost(API_GITHUB_COM)
                .setPath("/repos/" + owner + "/" + repo + "/releases/" + id);

        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = new HttpGet(requestUrl.build());
        return httpClient.execute(httpget, response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return gson.fromJson(EntityUtils.toString(entity), GithubRelease.class);
                }
                else {
                    throw new IllegalStateException("Entity in http response is null");
                }
            }
            else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        });
    }

    public List<GithubRelease> getReleases(String owner, String repo) throws IOException, URISyntaxException {
        URIBuilder requestUrl = new URIBuilder()
                .setScheme(HTTPS)
                .setHost(API_GITHUB_COM)
                .setPath("/repos/" + owner + "/" + repo + "/releases");

        logger.debug(() -> "requestUrl = " + requestUrl);
        HttpGet httpget = new HttpGet(requestUrl.build());
        return httpClient.execute(httpget, response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return gson.fromJson(EntityUtils.toString(entity), new TypeToken<ArrayList<GithubRelease>>() {
                    }.getType());
                }
                else {
                    throw new IllegalStateException("Entity in http response is null");
                }
            }
            else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        });
    }
}
