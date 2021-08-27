/*
 *    Copyright 2019-2021 Frederic Thevenet
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

package eu.binjr.core.preferences;

import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.common.preferences.MruFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserHistory extends MruFactory {
    private static final Logger logger = Logger.create(UserHistory.class);

    public final MostRecentlyUsedList<Path> logFilesHistory =
            pathMostRecentlyUsedList("logFilesHistory",
                    UserPreferences.getInstance().maxLogFilesToKeep.get().intValue(),
                    false);

    public final MostRecentlyUsedList<Path> mostRecentWorkspaces =
            pathMostRecentlyUsedList("mostRecentWorkspaces", 20, false);

    public final MostRecentlyUsedList<Path> mostRecentSaveFolders =
            pathMostRecentlyUsedList("mostRecentSaveFolders", 20, true);

    public final MostRecentlyUsedList<String> mostRecentLogFilters =
            stringMostRecentlyUsedList("logWorksheetMostRecentFilters", 30);

    private UserHistory() {
        super("binjr/history");
        logFilesHistory.setOnItemEvicted(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (SecurityException | IOException e) {
                logger.error("An error occurred while attempting to delete log file " + path.toString() + ": " + e.getMessage());
                logger.debug(() -> "Call Stack", e);
            } catch (Throwable t) {
                logger.error("An unexpected error occurred while attempting to delete log file " + path.toString() + ": " + t.getMessage());
                logger.debug(() -> "Call Stack", t);
            }
        });
    }

    public static UserHistory getInstance() {
        return UserHistoryHolder.instance;
    }

    private static class UserHistoryHolder {
        private final static UserHistory instance = new UserHistory();
    }
}
