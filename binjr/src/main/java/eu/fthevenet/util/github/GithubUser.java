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


    /**
     * @return hireable
     */
    public boolean isHireable() {
        return hireable;
    }

    /**
     * @param hireable
     * @return this user
     */
    public GithubUser setHireable(boolean hireable) {
        this.hireable = hireable;
        return this;
    }

    /**
     * @return createdAt
     */
    public Date getCreatedAt() {
        return new Date(createdAt.getTime());
    }

    /**
     * @param createdAt
     * @return this user
     */
    public GithubUser setCreatedAt(Date createdAt) {
        this.createdAt = new Date(createdAt.getTime());
        return this;
    }

    /**
     * @return collaborators
     */
    public int getCollaborators() {
        return collaborators;
    }

    /**
     * @param collaborators
     * @return this user
     */
    public GithubUser setCollaborators(int collaborators) {
        this.collaborators = collaborators;
        return this;
    }

    /**
     * @return diskUsage
     */
    public int getDiskUsage() {
        return diskUsage;
    }

    /**
     * @param diskUsage
     * @return this user
     */
    public GithubUser setDiskUsage(int diskUsage) {
        this.diskUsage = diskUsage;
        return this;
    }

    /**
     * @return followers
     */
    public int getFollowers() {
        return followers;
    }

    /**
     * @param followers
     * @return this user
     */
    public GithubUser setFollowers(int followers) {
        this.followers = followers;
        return this;
    }

    /**
     * @return following
     */
    public int getFollowing() {
        return following;
    }

    /**
     * @param following
     * @return this user
     */
    public GithubUser setFollowing(int following) {
        this.following = following;
        return this;
    }

    /**
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id
     * @return this user
     */
    public GithubUser setId(int id) {
        this.id = id;
        return this;
    }

    /**
     * @return ownedPrivateRepos
     */
    public int getOwnedPrivateRepos() {
        return ownedPrivateRepos;
    }

    /**
     * @param ownedPrivateRepos
     * @return this user
     */
    public GithubUser setOwnedPrivateRepos(int ownedPrivateRepos) {
        this.ownedPrivateRepos = ownedPrivateRepos;
        return this;
    }

    /**
     * @return privateGists
     */
    public int getPrivateGists() {
        return privateGists;
    }

    /**
     * @param privateGists
     * @return this user
     */
    public GithubUser setPrivateGists(int privateGists) {
        this.privateGists = privateGists;
        return this;
    }

    /**
     * @return publicGists
     */
    public int getPublicGists() {
        return publicGists;
    }

    /**
     * @param publicGists
     * @return this user
     */
    public GithubUser setPublicGists(int publicGists) {
        this.publicGists = publicGists;
        return this;
    }

    /**
     * @return publicRepos
     */
    public int getPublicRepos() {
        return publicRepos;
    }

    /**
     * @param publicRepos
     * @return this user
     */
    public GithubUser setPublicRepos(int publicRepos) {
        this.publicRepos = publicRepos;
        return this;
    }

    /**
     * @return totalPrivateRepos
     */
    public int getTotalPrivateRepos() {
        return totalPrivateRepos;
    }

    /**
     * @param totalPrivateRepos
     * @return this user
     */
    public GithubUser setTotalPrivateRepos(int totalPrivateRepos) {
        this.totalPrivateRepos = totalPrivateRepos;
        return this;
    }

    /**
     * @return avatarUrl
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * @param avatarUrl
     * @return this user
     */
    public GithubUser setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    /**
     * @return bio
     */
    public String getBio() {
        return bio;
    }

    /**
     * @param bio
     * @return this user
     */
    public GithubUser setBio(String bio) {
        this.bio = bio;
        return this;
    }

    /**
     * @return blog
     */
    public String getBlog() {
        return blog;
    }

    /**
     * @param blog
     * @return this user
     */
    public GithubUser setBlog(String blog) {
        this.blog = blog;
        return this;
    }

    /**
     * @return company
     */
    public String getCompany() {
        return company;
    }

    /**
     * @param company
     * @return this user
     */
    public GithubUser setCompany(String company) {
        this.company = company;
        return this;
    }

    /**
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email
     * @return this user
     */
    public GithubUser setEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * @return gravatarId
     * @deprecated
     */
    @Deprecated
    public String getGravatarId() {
        return gravatarId;
    }

    /**
     * @param gravatarId
     * @return this user
     * @deprecated
     */
    @Deprecated
    public GithubUser setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
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
     * @return this user
     */
    public GithubUser setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    /**
     * @return location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location
     * @return this user
     */
    public GithubUser setLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * @return login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @param login
     * @return this user
     */
    public GithubUser setLogin(String login) {
        this.login = login;
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
     * @return this user
     */
    public GithubUser setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     * @return this user
     */
    public GithubUser setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     * @return this user
     */
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