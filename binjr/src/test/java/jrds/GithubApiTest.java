package jrds;

import eu.fthevenet.util.github.GithubApi;
import eu.fthevenet.util.github.GithubRelease;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by FTT2 on 24/04/2017.
 */
public class GithubApiTest {
    private static final Logger logger = LogManager.getLogger(GithubApiTest.class);

    public static void main(String[] args) {
        try {
            GithubRelease release = GithubApi.getInstance().getLatestRelease("fthevenet", "binjr");
            logger.info("release = " + release.toString());
            logger.info("release.getTagName() = " + release.getTagName().replaceAll("^v", ""));


            List<GithubRelease> releases = GithubApi.getInstance().getReleases("fthevenet", "binjr");
            releases.forEach(r -> logger.info("r = " + r));


        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
