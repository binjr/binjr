/*
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

package eu.binjr.core.update;

import eu.binjr.common.version.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * The implementation of {@link PlatformUpdater} for the Linux platform.
 *
 * @author Frederic Thevenet
 */
public class LinuxTarballUpdater implements PlatformUpdater {
    private static final Logger logger = LogManager.getLogger(LinuxTarballUpdater.class);

    @Override
    public boolean isInAppUpdateSupported() {
        return true;
    }

    @Override
    public void launchUpdater(Path updatePackage, Version updateVersion, boolean restartRequested) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        File jar = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        File oldVersionDirectory = jar.getParentFile().getParentFile();
        File rootDirectory = oldVersionDirectory.getParentFile();
        File upgradeFile = new File(rootDirectory, "upgrade");
        Files.copy(new File(oldVersionDirectory, "resources/scripts/upgrade.sh").toPath(), upgradeFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        processBuilder.directory(rootDirectory);
        Map<String, String> environment = processBuilder.environment();
        environment.put("OLD_VERSION", oldVersionDirectory.getName());
        environment.put("NEW_VERSION", updateVersion.toString());
        environment.put("PACKAGE", updatePackage.toString());
        environment.put("RESTART", restartRequested ? "true" : "false");
        processBuilder.command(upgradeFile.getPath());
        logger.debug(() -> "Launching update command: " + processBuilder.command());
        processBuilder.start();
    }
}
