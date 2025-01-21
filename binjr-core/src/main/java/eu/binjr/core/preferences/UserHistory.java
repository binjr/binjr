/*
 *    Copyright 2019-2025 Frederic Thevenet
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
import eu.binjr.common.preferences.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * Manages user history metadata
 */
public class UserHistory extends MruFactory {
    private static final Logger logger = Logger.create(UserHistory.class);
    public static final String BACKING_STORE_KEY = "binjr/history";
    private final ObfuscatedString.Obfuscator obfuscator = UserPreferences.getInstance().getObfuscator();
    private final CredentialStore savedCredentials = new CredentialStore();


    public class CredentialStore extends ObservablePreferenceFactory {
        private static final Logger logger = Logger.create(UserHistory.CredentialStore.class);
        public static final String USER_NAME = "_userName";
        public static final String PASSWORD = "_password";

        private static final ThreadLocal<MessageDigest> tlMd = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                logger.error("Failed to instantiate message digest: " + e.getMessage());
                logger.debug("Stack trace", e);
                return null;
            }
        });

        public CredentialStore() {
            super(BACKING_STORE_KEY + "/credentials");
        }

        public void putUserName(String key, String userName, String password) {
            this.put(hashKey(key + USER_NAME),  obfuscator.obfuscateString(userName));
            this.put(hashKey(key + PASSWORD), obfuscator.obfuscateString(password));
        }

        public Optional<String> getUserName(String key) {
            return get(key, USER_NAME);
        }

        public Optional<String> getPassword(String key) {
            return get(key, PASSWORD);
        }

        private Optional<String> get(String key, String suffix) {
            return this.lookup(hashKey(key + suffix), "").map(p -> obfuscator.deObfuscateString(p.get()));
        }

        private String hashKey(String value) {
            var md = tlMd.get();
            if (md == null) {
                return value;
            }
            md.update((value).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md.digest());
        }

        public void clearCredentials(String key) {
            this.remove(hashKey(key + USER_NAME));
            this.remove(hashKey(key + PASSWORD));
        }
    }

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

    public CredentialStore getSavedCredentials() {
        return savedCredentials;
    }

    private UserHistory() {
        super(BACKING_STORE_KEY);
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
