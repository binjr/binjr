package eu.binjr.common.github;/*
 *    Copyright 2019 Frederic Thevenet
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public class MakeChangeLog {

    private static final String HTML_HEADER =
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>binjr</title>\n" +
            "    <meta charset=\"utf-8\"/>\n" +
            "    <link rel=\"stylesheet\" href=\"./resources/css/air.css\">\n" +
            "</head>\n\n";

    public static void main(String[] args) {
        try {
            buildChangeLog("binjr", "binjr", Paths.get("distribution/info/CHANGELOG.md"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

    public static void buildChangeLog(String owner, String repo, Path output) throws IOException, URISyntaxException {
        try (FileOutputStream fout = new FileOutputStream(output.toFile())) {
            buildChangeLog(owner, repo, fout);
        }
    }

    public static void buildChangeLog(String owner, String repo, OutputStream output) throws IOException, URISyntaxException {
        var df = new SimpleDateFormat("EEE, d MMM yyyy");
        try (var writer = new OutputStreamWriter(output)) {
            writer.write("# Change Log\n\n");
            for (var release : GithubApi.getInstance().getAllReleases(owner, repo)) {
                writer.write("### [" + release.getName() + "](" + release.getHtmlUrl() + ")\n");
                writer.write("Released on " + df.format(release.getPublishedAt()) + "\n\n");
                writer.write(release.getBody());
                writer.write("\n\n");
            }
        }

    }

}
