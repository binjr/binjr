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

package github;

import eu.fthevenet.util.github.GithubApi;
import eu.fthevenet.util.github.GithubRelease;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Frederic Thevenet
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
