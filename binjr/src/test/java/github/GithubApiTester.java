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
import java.util.Optional;

/**
 * @author Frederic Thevenet
 */
public class GithubApiTester {
    private static final Logger logger = LogManager.getLogger(GithubApiTester.class);

    public static void main(String[] args) {
        try {
            Optional<GithubRelease> release = GithubApi.getInstance().getLatestRelease("fthevenet", "binjr");
            if (release.isPresent()) {
                logger.info("release = " + release.get().toString());
                logger.info("release.getTagName() = " + release.get().getTagName().replaceAll("^v", ""));
            }
            else {
                logger.error("Could not find latest release");
            }


            List<GithubRelease> releases = GithubApi.getInstance().getAllReleases("fthevenet", "binjr");
            releases.forEach(r -> logger.info("r = " + r));


        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
