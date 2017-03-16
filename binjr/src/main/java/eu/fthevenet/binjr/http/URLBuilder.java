package eu.fthevenet.binjr.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple URL builder
 *
 * @author Frederic Thevenet
 */
public class URLBuilder {
    private static final Logger logger = LogManager.getLogger(URLBuilder.class);
    private final String charset;
    private String host;
    private String protocol;
    private int port;
    private final Map<String, String> queryParams = new HashMap<>();
    private String userInfo;
    private String path;


    public URLBuilder() {
        this("utf-8");
    }

    public URLBuilder(String charset) {
        this.charset = charset;
    }

    public URLBuilder setPath(String path) {
        try {
            this.path = URLEncoder.encode(path.replaceAll("/$", "").replaceAll("^/", ""), charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding URL path", e);
        }
        return this;
    }

    public URLBuilder setUserInfo(String userInfo) {
        try {
            this.userInfo = URLEncoder.encode(userInfo, charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding URL userInfo", e);
        }
        return this;
    }

    public URLBuilder addParameter(String parameter, String value) {
        try {
            queryParams.put(
                    URLEncoder.encode(parameter, charset),
                    URLEncoder.encode(value, charset));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error URL encoding parameter", e);
        }
        return this;
    }

    public URLBuilder setProtocol(String protocol) {
        try {
            this.protocol = URLEncoder.encode(protocol, charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding URL protocol", e);
        }
        return this;
    }

    public URLBuilder setHost(String host) {
        try {
            this.host = URLEncoder.encode(host, charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding URL host", e);
        }
        return this;
    }

    public URLBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public URL build() throws URISyntaxException, MalformedURLException {
        return new URI(buildString()).toURL();
    }

    @Override
    public String toString() {
        return buildString();
    }

    private String buildString() {
        final StringBuilder sb = new StringBuilder();
        if (this.protocol != null) {
            sb.append(this.protocol).append(':');
        }
        if (this.host != null) {
            sb.append("//");
            if (this.userInfo != null) {
                sb.append(this.userInfo).append("@");
            }
            sb.append(this.host);

            if (this.port >= 0) {
                sb.append(":").append(this.port);
            }
        }
        if (this.path != null) {
            sb.append("/").append(this.path);
            if (this.queryParams.size() > 0) {
                sb.append("?").append(queryParams.entrySet().stream()
                        .map(e -> e.getKey()+ "=" + e.getValue())
                        .collect(Collectors.joining("&")));
            }
        }
        return sb.toString();
    }
}