/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.common.preferences;

import eu.binjr.core.Binjr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * PreferencesFactory implementation that stores the preferences in a user-defined file. To use it,
 * set the system property {@code java.util.prefs.PreferencesFactor}
 *
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 */
public class FilePreferencesFactory implements PreferencesFactory {

    private static final Logger logger = LogManager.getLogger(FilePreferencesFactory.class);

    Preferences userRootPreferences;
    Preferences systemRootPreferences;


    public Preferences systemRoot() {
        if (systemRootPreferences == null) {
            logger.trace("Instantiating system root preferences");
            try {
                userRootPreferences = new FilePreferences(getBackingFile("system"), null, "");
            } catch (Exception e) {
                logger.fatal("Failed to create file-backed preferences for system root", e);
            }
        }
        return systemRootPreferences;
    }

    @Override
    public Preferences userRoot() {
        if (userRootPreferences == null) {
            logger.trace("Instantiating user root preferences");
            try {
                userRootPreferences = new FilePreferences(getBackingFile("user"), null, "");
            } catch (Exception e) {
                logger.fatal("Failed to create file-backed preferences for user root", e);
            }
        }
        return userRootPreferences;
    }

    private File getBackingFile(String fileName) throws IOException {
        try {
            var jarLocation = Paths.get(Binjr.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            var configDir = jarLocation.getParent().getParent().resolve("settings");
            if (!Files.isDirectory(configDir)) {
                Files.createDirectory(configDir);
            }
            var preferencesFile = configDir.resolve(fileName).toFile();
            if (!preferencesFile.exists()) {
                preferencesFile.createNewFile();
            }
            return preferencesFile;
        } catch (Exception e) {
            logger.error("Failed to create file to store preferences" + e.getMessage());
            logger.debug(() -> "Stack Trace", e);
            logger.warn("Attempting to store preferences in temporary file as a fallback");
            return Files.createTempFile("binjr_", "_settings").toFile();
        }
    }

}