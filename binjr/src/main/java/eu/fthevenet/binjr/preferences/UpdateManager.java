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

package eu.fthevenet.binjr.preferences;

import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import eu.fthevenet.util.github.GithubApi;
import eu.fthevenet.util.github.GithubRelease;
import eu.fthevenet.util.version.Version;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Defines a series of methods to manage updates
 */
public class UpdateManager {
    private static final Logger logger = LogManager.getLogger(UpdateManager.class);
    private static final String GITHUB_OWNER = "fthevenet";
    private static final String GITHUB_REPO = "binjr";
    private static final String LAST_CHECK_FOR_UPDATE = "lastCheckForUpdate";
    private static final String BINJR_UPDATE = "binjr/update";
    private Property<LocalDateTime> lastCheckForUpdate;

    private static class UpdateManagerHolder {
        private final static UpdateManager instance = new UpdateManager();
    }

    private UpdateManager() {
        Preferences prefs = Preferences.userRoot().node(BINJR_UPDATE);
        lastCheckForUpdate = new SimpleObjectProperty<>(LocalDateTime.parse(prefs.get(LAST_CHECK_FOR_UPDATE, "1900-01-01T00:00:00"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        lastCheckForUpdate.addListener((observable, oldValue, newValue) -> prefs.put(LAST_CHECK_FOR_UPDATE, newValue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Get the singleton instance for the {@link UpdateManager} class.
     *
     * @return the singleton instance for the {@link UpdateManager} class.
     */
    public static UpdateManager getInstance() {
        return UpdateManagerHolder.instance;
    }

    /**
     * Check for available update asynchronously. It includes a  built-in limit to 1 check per hour.
     *
     * @param newReleaseAvailable The delegate run in the event that a new release is available
     * @param upToDate            The delegate to run in the event that tha current version is up to date
     * @param onFailure           The delegate to run in the event of an error while checking for an update
     */
    public void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Consumer<Version> upToDate, Runnable onFailure) {
        asyncCheckForUpdate(newReleaseAvailable, upToDate, onFailure, false);
    }

    /**
     * Force an async check for available update and ignore 1 check per hour limit.
     *
     * @param newReleaseAvailable The delegate run in the event that a new release is available
     * @param upToDate            The delegate to run in the event that tha current version is up to date
     * @param onFailure           The delegate to run in the event of an error while checking for an update
     */
    public void asyncForcedCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Consumer<Version> upToDate, Runnable onFailure) {
        asyncCheckForUpdate(newReleaseAvailable, upToDate, onFailure, true);
    }

    /**
     * Get the time stamp of the latest update check
     *
     * @return the time stamp of the latest update check
     */
    public LocalDateTime getLastCheckForUpdate() {
        return lastCheckForUpdate.getValue();
    }

    /**
     * Get the lastCheckForUpdateProperty property
     *
     * @return the lastCheckForUpdateProperty property
     */
    public Property<LocalDateTime> lastCheckForUpdateProperty() {
        return lastCheckForUpdate;
    }

    private void setLastCheckForUpdate(LocalDateTime lastCheckForUpdate) {
        this.lastCheckForUpdate.setValue(lastCheckForUpdate);
    }

    private void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Consumer<Version> upToDate, Runnable onFailure, boolean forceCheck) {
        if (forceCheck || LocalDateTime.now().minus(1, ChronoUnit.HOURS).isAfter(getLastCheckForUpdate())) {
            setLastCheckForUpdate(LocalDateTime.now());
            Task<Optional<GithubRelease>> getLatestTask = new Task<Optional<GithubRelease>>() {
                @Override
                protected Optional<GithubRelease> call() throws Exception {
                    logger.trace("getNewRelease running on " + Thread.currentThread().getName());
                    return GithubApi.getInstance().getLatestRelease(GITHUB_OWNER, GITHUB_REPO).filter(r -> r.getVersion().compareTo(AppEnvironment.getInstance().getManifestVersion()) > 0);
                }
            };
            getLatestTask.setOnSucceeded(workerStateEvent -> {
                logger.trace("UI update running on " + Thread.currentThread().getName());
                Optional<GithubRelease> latest = getLatestTask.getValue();
                Version current = AppEnvironment.getInstance().getManifestVersion();
                if (latest.isPresent()) {
                    newReleaseAvailable.accept(latest.get());
                }
                else {
                    if (upToDate != null) {
                        upToDate.accept(current);
                    }
                }
            });
            getLatestTask.setOnFailed(workerStateEvent -> {
                logger.error("Error while checking for update", getLatestTask.getException());
                if (onFailure != null) {
                    onFailure.run();
                }
            });
            AsyncTaskManager.getInstance().submit(getLatestTask);
        }
        else {
            logger.debug(() -> "Available update check ignored as it already took place less than 1 hour ago.");
        }
    }


}
