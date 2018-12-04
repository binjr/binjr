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

package eu.fthevenet.util.github;

import com.google.gson.annotations.SerializedName;

import java.util.Date;


/**
 * GitHub user POJO
 *
 * @author Frederic Thevenet
 */
public class GithubUser {
    private boolean hireable;
    @SerializedName("created_at")
    private Date createdAt;
    private int collaborators;
    @SerializedName("disk_usage")
    private int diskUsage;
    private int followers;
    private int following;
    private int id;
    @SerializedName("owned_private_repos")
    private int ownedPrivateRepos;
    @SerializedName("private_gists")
    private int privateGists;
    @SerializedName("public_gists")
    private int publicGists;
    @SerializedName("public_repos")
    private int publicRepos;
    @SerializedName("total_private_repos")
    private int totalPrivateRepos;
    @SerializedName("avatar_url")
    private String avatarUrl;
    private String bio;
    private String blog;
    private String company;
    private String email;
    @SerializedName("gravatar_id")
    private String gravatarId;
    @SerializedName("html_url")
    private String htmlUrl;
    private String location;
    private String login;
    private String name;
    private String type;
    private String url;

    public boolean isHireable() {
        return hireable;
    }

    public GithubUser setHireable(boolean hireable) {
        this.hireable = hireable;
        return this;
    }

    public Date getCreatedAt() {
        return new Date(createdAt.getTime());
    }

    public GithubUser setCreatedAt(Date createdAt) {
        this.createdAt = new Date(createdAt.getTime());
        return this;
    }

    public int getCollaborators() {
        return collaborators;
    }

    public GithubUser setCollaborators(int collaborators) {
        this.collaborators = collaborators;
        return this;
    }

    public int getDiskUsage() {
        return diskUsage;
    }

    public GithubUser setDiskUsage(int diskUsage) {
        this.diskUsage = diskUsage;
        return this;
    }

    public int getFollowers() {
        return followers;
    }

    public GithubUser setFollowers(int followers) {
        this.followers = followers;
        return this;
    }

    public int getFollowing() {
        return following;
    }

    public GithubUser setFollowing(int following) {
        this.following = following;
        return this;
    }

    public int getId() {
        return id;
    }

    public GithubUser setId(int id) {
        this.id = id;
        return this;
    }

    public int getOwnedPrivateRepos() {
        return ownedPrivateRepos;
    }

    public GithubUser setOwnedPrivateRepos(int ownedPrivateRepos) {
        this.ownedPrivateRepos = ownedPrivateRepos;
        return this;
    }

    public int getPrivateGists() {
        return privateGists;
    }

    public GithubUser setPrivateGists(int privateGists) {
        this.privateGists = privateGists;
        return this;
    }

    public int getPublicGists() {
        return publicGists;
    }

    public GithubUser setPublicGists(int publicGists) {
        this.publicGists = publicGists;
        return this;
    }

    public int getPublicRepos() {
        return publicRepos;
    }

    public GithubUser setPublicRepos(int publicRepos) {
        this.publicRepos = publicRepos;
        return this;
    }

    public int getTotalPrivateRepos() {
        return totalPrivateRepos;
    }

    public GithubUser setTotalPrivateRepos(int totalPrivateRepos) {
        this.totalPrivateRepos = totalPrivateRepos;
        return this;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public GithubUser setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public String getBio() {
        return bio;
    }

    public GithubUser setBio(String bio) {
        this.bio = bio;
        return this;
    }

    public String getBlog() {
        return blog;
    }

    public GithubUser setBlog(String blog) {
        this.blog = blog;
        return this;
    }

    public String getCompany() {
        return company;
    }

    public GithubUser setCompany(String company) {
        this.company = company;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public GithubUser setEmail(String email) {
        this.email = email;
        return this;
    }

    @Deprecated
    public String getGravatarId() {
        return gravatarId;
    }

    @Deprecated
    public GithubUser setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
        return this;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public GithubUser setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public GithubUser setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public GithubUser setLogin(String login) {
        this.login = login;
        return this;
    }

    public String getName() {
        return name;
    }

    public GithubUser setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public GithubUser setType(String type) {
        this.type = type;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public GithubUser setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GithubUser{");
        sb.append("hireable=").append(hireable);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", collaborators=").append(collaborators);
        sb.append(", diskUsage=").append(diskUsage);
        sb.append(", followers=").append(followers);
        sb.append(", following=").append(following);
        sb.append(", id=").append(id);
        sb.append(", ownedPrivateRepos=").append(ownedPrivateRepos);
        sb.append(", privateGists=").append(privateGists);
        sb.append(", publicGists=").append(publicGists);
        sb.append(", publicRepos=").append(publicRepos);
        sb.append(", totalPrivateRepos=").append(totalPrivateRepos);
        sb.append(", avatarUrl='").append(avatarUrl).append('\'');
        sb.append(", bio='").append(bio).append('\'');
        sb.append(", blog='").append(blog).append('\'');
        sb.append(", company='").append(company).append('\'');
        sb.append(", email='").append(email).append('\'');
        sb.append(", gravatarId='").append(gravatarId).append('\'');
        sb.append(", htmlUrl='").append(htmlUrl).append('\'');
        sb.append(", location='").append(location).append('\'');
        sb.append(", login='").append(login).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}