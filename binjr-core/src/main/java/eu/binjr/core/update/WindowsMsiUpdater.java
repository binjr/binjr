/*
 *    Copyright 2019-2020 Frederic Thevenet
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

package eu.binjr.core.update;

import eu.binjr.common.logging.Logger;
import eu.binjr.common.version.Version;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link PlatformUpdater} for the Windows platform.
 *
 * @author Frederic Thevenet
 */
public class WindowsMsiUpdater implements PlatformUpdater {
    private static final Logger logger = Logger.create(WindowsMsiUpdater.class);

    @Override
    public boolean isInAppUpdateSupported() {
        return true;
    }

    @Override
    public void launchUpdater(Path updatePackage, Version updateVersion, boolean restartRequested) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Path installLauncherPath = Files.createTempFile("binjr-install-", ".bat");
        List<String> lines = new ArrayList<>();
        lines.add("echo off");
        lines.add("call msiexec /passive LAUNCHREQUESTED=" + (restartRequested ? "1" : "0") +
                " /log " + updatePackage.getParent().resolve("binjr-install.log").toString() +
                " /i " + updatePackage.toString());
        lines.add("del /Q /F " + updatePackage.toString() + "*");
        lines.add("(goto) 2>nul & del \"%~f0\"");
        Files.write(installLauncherPath, lines, StandardCharsets.US_ASCII);
        processBuilder.command(
                "cmd.exe",
                "/C",
                installLauncherPath.toString());
        logger.debug(() -> "Launching update command: " + processBuilder.command());
        processBuilder.start();
    }
}
