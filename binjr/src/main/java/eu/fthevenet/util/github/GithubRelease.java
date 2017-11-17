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


import com.google.gson.annotations.SerializedName;
import eu.fthevenet.util.version.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;


/**
 * GitHub release POJO
 *
 * @author Frederic Thevenet
 */
public class GithubRelease {
    private static final Logger logger = LogManager.getLogger(GithubRelease.class);
    private String url;
    @SerializedName("html_url")
    private String htmlUrl;
    @SerializedName("assets_url")
    private String assetsUrl;
    @SerializedName("upload_url")
    private String uploadUrl;
    @SerializedName("tarball_url")
    private String tarballUrl;
    @SerializedName("zipball_url")
    private String zipballUrl;
    private long id;
    @SerializedName("tag_name")
    private String tagName;
    @SerializedName("target_commitish")
    private String targetCommitish;
    private String name;
    private String body;
    @SerializedName("draft")
    private boolean isDraft;
    @SerializedName("prerelease")
    private boolean isPrerelease;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("published_at")
    private Date publishedAt;
    private GithubUser author;

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     * @return this release
     */
    public GithubRelease setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return htmlUrl
     */
    public String getHtmlUrl() {
        return htmlUrl;
    }

    /**
     * @param htmlUrl
     * @return this release
     */
    public GithubRelease setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    /**
     * @return assetsUrl
     */
    public String getAssetsUrl() {
        return assetsUrl;
    }

    /**
     * @param assetsUrl
     * @return this release
     */
    public GithubRelease setAssetsUrl(String assetsUrl) {
        this.assetsUrl = assetsUrl;
        return this;
    }

    /**
     * @return uploadUrl
     */
    public String getUploadUrl() {
        return uploadUrl;
    }

    /**
     * @param uploadUrl
     * @return this release
     */
    public GithubRelease setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        return this;
    }

    /**
     * @return tarballUrl
     */
    public String getTarballUrl() {
        return tarballUrl;
    }

    /**
     * @param tarballUrl
     * @return this release
     */
    public GithubRelease setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
        return this;
    }

    /**
     * @return zipballUrl
     */
    public String getZipballUrl() {
        return zipballUrl;
    }

    /**
     * @param zipballUrl
     * @return this release
     */
    public GithubRelease setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
        return this;
    }

    /**
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     * @return this release
     */
    public GithubRelease setId(long id) {
        this.id = id;
        return this;
    }

    /**
     * @return tagName
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * @param tagName
     * @return this release
     */
    public GithubRelease setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    /**
     * @return targetCommitish
     */
    public String getTargetCommitish() {
        return targetCommitish;
    }

    /**
     * @param targetCommitish
     * @return this release
     */
    public GithubRelease setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
        return this;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     * @return this release
     */
    public GithubRelease setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return body
     */
    public String getBody() {
        return body;
    }

    /**
     * @param body
     * @return this release
     */
    public GithubRelease setBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * @return isDraft
     */
    public boolean isDraft() {
        return isDraft;
    }

    /**
     * @param isDraft
     * @return this release
     */
    public GithubRelease setDraft(boolean isDraft) {
        this.isDraft = isDraft;
        return this;
    }

    /**
     * @return isPrerelease
     */
    public boolean isPrerelease() {
        return isPrerelease;
    }

    /**
     * @param isPrerelease
     * @return this release
     */
    public GithubRelease setPrerelease(boolean isPrerelease) {
        this.isPrerelease = isPrerelease;
        return this;
    }

    /**
     * @return createdAt
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt
     * @return this release
     */
    public GithubRelease setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * @return publishedAt
     */
    public Date getPublishedAt() {
        return publishedAt;
    }

    /**
     * @param publishedAt
     * @return this release
     */
    public GithubRelease setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    /**
     * @return author
     */
    public GithubUser getAuthor() {
        return author;
    }

    /**
     * @param author
     * @return this release
     */
    public GithubRelease setAuthor(GithubUser author) {
        this.author = author;
        return this;
    }

    public Version getVersion() {
        if (tagName == null) {
            return Version.emptyVersion;
        }
        try {
            return new Version(tagName.replaceAll("^v", ""));
        } catch (IllegalArgumentException e) {
            logger.error("Could not decode version number from tag: " + tagName, e);
            return Version.emptyVersion;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GithubRelease{");
        sb.append("url='").append(url).append('\'');
        sb.append(", htmlUrl='").append(htmlUrl).append('\'');
        sb.append(", assetsUrl='").append(assetsUrl).append('\'');
        sb.append(", uploadUrl='").append(uploadUrl).append('\'');
        sb.append(", tarballUrl='").append(tarballUrl).append('\'');
        sb.append(", zipballUrl='").append(zipballUrl).append('\'');
        sb.append(", id=").append(id);
        sb.append(", tagName='").append(tagName).append('\'');
        sb.append(", targetCommitish='").append(targetCommitish).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", body='").append(body).append('\'');
        sb.append(", isDraft=").append(isDraft);
        sb.append(", isPrerelease=").append(isPrerelease);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", publishedAt=").append(publishedAt);
        sb.append(", author=").append(author);
        sb.append('}');
        return sb.toString();
    }
}
