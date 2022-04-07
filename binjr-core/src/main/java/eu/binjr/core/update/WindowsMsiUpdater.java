/*
 *    Copyright 2019-2022 Frederic Thevenet
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
import eu.binjr.core.preferences.UserPreferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        var processBuilder = new ProcessBuilder();
        var launcherPath = Files.createTempFile(UserPreferences.getInstance().temporaryFilesRoot.get(),
                "binjr-install-", ".bat");
        var payload = List.of(
                "echo off",
                "call msiexec /passive LAUNCHREQUESTED=" + (restartRequested ? "1" : "0") +
                        " /log " + updatePackage.getParent().resolve("binjr-install.log") +
                        " /i " + updatePackage,
                "del /Q /F " + updatePackage + "*",
                "(goto) 2>nul & del \"%~f0\"");
        Files.write(launcherPath, payload, StandardCharsets.US_ASCII);
        processBuilder.command("cmd.exe", "/C", launcherPath.toString());
        logger.debug(() -> "Launching update command: " + processBuilder.command());
        processBuilder.start();
    }
}
