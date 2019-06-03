/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.common.github.GithubApiHelper;
import eu.binjr.common.github.GithubRelease;
import eu.binjr.common.version.Version;
import eu.binjr.core.dialogs.Dialogs;
import impl.org.controlsfx.skin.NotificationBar;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String LAST_CHECK_FOR_UPDATE = "lastCheckForUpdate";
    private static final String BINJR_UPDATE = "binjr/update";
    private Property<LocalDateTime> lastCheckForUpdate;
    private Path updatePackage = null;
    private boolean restartRequested = false;
    private final GithubApiHelper github;

    private static class UpdateManagerHolder {
        private final static UpdateManager instance = new UpdateManager();
    }

    private UpdateManager() {
        this.github = GithubApiHelper.createCloseable();
        Preferences prefs = Preferences.userRoot().node(BINJR_UPDATE);
        lastCheckForUpdate = new SimpleObjectProperty<>(LocalDateTime.parse(prefs.get(LAST_CHECK_FOR_UPDATE, "1900-01-01T00:00:00"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        lastCheckForUpdate.addListener((observable, oldValue, newValue) -> prefs.put(LAST_CHECK_FOR_UPDATE, newValue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        GlobalPreferences.getInstance().githubUserNameProperty().addListener((observable, oldValue, newValue) -> {
            github.setUserCredentials(newValue, GlobalPreferences.getInstance().getGithubAuthToken());
        });
        GlobalPreferences.getInstance().githubAuthTokenProperty().addListener((observable, oldValue, newValue) -> {
            github.setUserCredentials(GlobalPreferences.getInstance().getGithubUserName(), newValue);
        });
        github.setUserCredentials(
                GlobalPreferences.getInstance().getGithubUserName(),
                GlobalPreferences.getInstance().getGithubAuthToken());
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
        if (AppEnvironment.getInstance().isDisableUpdateCheck()) {
            logger.trace(() -> "Update check is explicitly disabled.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        if (!forceCheck && LocalDateTime.now().minus(1, ChronoUnit.HOURS).isBefore(getLastCheckForUpdate())) {
            logger.trace(() -> "Available update check ignored as it already took place less than 1 hour ago.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        setLastCheckForUpdate(LocalDateTime.now());
        Task<Optional<GithubRelease>> getLatestTask = new Task<>() {
            @Override
            protected Optional<GithubRelease> call() throws Exception {
                logger.trace("getNewRelease running on " + Thread.currentThread().getName());
                return github
                        .getLatestRelease(AppEnvironment.getInstance().getUpdateRepoSlug())
                        .filter(r -> r.getVersion().compareTo(AppEnvironment.getInstance().getVersion()) > 0);
            }
        };
        getLatestTask.setOnSucceeded(workerStateEvent -> {
            logger.trace("UI update running on " + Thread.currentThread().getName());
            Optional<GithubRelease> latest = getLatestTask.getValue();
            Version current = AppEnvironment.getInstance().getVersion();
            if (latest.isPresent()) {
                newReleaseAvailable.accept(latest.get());
            } else {
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

    public void asyncDownloadUpdatePackage(GithubRelease release, Consumer<Path> onDownloadComplete, Consumer<Throwable> onFailure) {

        Task<Path> downloadTask = new Task<Path>() {
            @Override
            protected Path call() throws Exception {
                var asset = github.getAssets(release)
                        .stream()
                        .filter(a -> a.getName().contains(AppEnvironment.getInstance().getOsFamily().getPlatformClassifier()))
                        .findFirst()
                        .orElseThrow();
                logger.info("Downloading update from " + asset.getBrowserDownloadUrl());
                return github.downloadAsset(asset);
            }
        };

        downloadTask.setOnSucceeded(event -> {
            logger.info("Update download complete (" + downloadTask.getValue() + ")");
            onDownloadComplete.accept(downloadTask.getValue());
        });

        downloadTask.setOnFailed(event -> {
            logger.error("Error while downloading update package", downloadTask.getException());
            if (onFailure != null) {
                onFailure.accept(downloadTask.getException());
            }
        });
        AsyncTaskManager.getInstance().submit(downloadTask);
    }

    public void startUpdate() {
        if (!AppEnvironment.getInstance().isDisableUpdateCheck() && updatePackage != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                switch (AppEnvironment.getInstance().getOsFamily()) {
                    case WINDOWS:
                        processBuilder.command(
                                "msiexec",
                                "/passive",
                                "LAUNCHREQUESTED=" + (restartRequested ? "1" : "0"),
                                "/log", updatePackage.getParent().resolve("binjr-install.log").toString(),
                                "/i", updatePackage.toString());
                        break;
                    case OSX:
                    case LINUX:
                        File jar = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
                        File directory = jar.getParentFile().getParentFile();
                        File fileList = new File(directory, ".installed");
                        try (BufferedReader fileReader = new BufferedReader(new FileReader(fileList))) {
                            for (String line;
                                 (line = fileReader.readLine()) != null;
                                 ) {
                                File file = new File(directory, line);
                                if (!file.isDirectory() && !file.delete()) {
                                    logger.error(() -> "Unable to remove file before upgrade: '" + file.getAbsolutePath() + "'");
                                    logger.info(() -> "Consider deleting all files listed in '" + fileList.getAbsolutePath() + "' and installing again from '" + updatePackage.toString() + "'");
                                    return;
                                }
                            }
                        }
                        String command = "cd \"" + directory.getPath() + "\" && tar xzf \"" + updatePackage.toString() + "\" && rm \"" + updatePackage.toAbsolutePath() + "\"";
                        if (restartRequested) {
                            command += " ; " + new File(directory, "binjr").getPath();
                        }
                        processBuilder.command(
                            "sh",
                            "-c",
                            command);
                        break;
                    case UNSUPPORTED:
                    default:
                        return;
                }
                logger.debug(() -> "Launching update command: " + processBuilder.command());
                processBuilder.start();
            } catch (Exception e) {
                logger.error("Error starting update", e);
            }
        }
    }

    public Optional<Path> getUpdatePackagePath() {
        return updatePackage == null ? Optional.empty() : Optional.of(updatePackage);
    }

    public void showUpdateAvailableNotification(GithubRelease release, Node root) {
        if (updatePackage != null) {
            showUpdateReadyNotification(root);
            return;
        }
        Notifications n = Notifications.create()
                .title("New release available!")
                .text("You are currently running " + AppEnvironment.APP_NAME + " version " +
                        AppEnvironment.getInstance().getVersion() +
                        "\t\t\nVersion " + release.getVersion() + " is now available.")
                .hideAfter(Duration.seconds(20))
                .position(Pos.BOTTOM_RIGHT)
                .owner(root);
        n.action(new Action("More info", event -> {
                    URL newReleaseUrl = release.getHtmlUrl();
                    if (newReleaseUrl != null) {
                        try {
                            Dialogs.launchUrlInExternalBrowser(newReleaseUrl);
                        } catch (IOException | URISyntaxException e) {
                            logger.error("Failed to launch url in browser " + newReleaseUrl, e);
                        }
                    }
                }),
                new Action("Download update", event -> {
                    UpdateManager.getInstance().asyncDownloadUpdatePackage(
                            release,
                            path -> {
                                updatePackage = path;
                                showUpdateReadyNotification(root);
                            },
                            exception -> Dialogs.notifyException("Error downloading update", exception, root));
                    dismissNotificationPopup((Node) event.getSource());
                }));
        n.showInformation();
    }

    private void showUpdateReadyNotification(Node root) {
        Notifications n = Notifications.create()
                .title("binjr is now ready to be updated!")
                .text("The update package has been downloaded successfully.")
                .hideAfter(Duration.seconds(20))
                .position(Pos.BOTTOM_RIGHT)
                .owner(root);
        n.action(new Action("Restart & update now", event -> restartApp(root)),
                new Action("Update when I exit", event -> {
                    dismissNotificationPopup((Node) event.getSource());
                }));
        n.showInformation();
    }

    private void restartApp(Node root) {
        var stage = Dialogs.getStage(root);
        restartRequested = true;
        if (stage != null) {
            var handler = stage.getOnCloseRequest();
            if (handler != null) {
                handler.handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        }
    }

    // This is pretty nasty (and probably breaks with Jigsaw),
    // but couldn't find another way to close the notification popup.
    private void dismissNotificationPopup(Node n) {
        if (n == null) {
            //couldn't find NotificationBar, giving up.
            return;
        }
        if (n instanceof NotificationBar) {
            // found it, hide the popup.
            ((NotificationBar) n).hide();
            return;
        }
        // keep looking.
        dismissNotificationPopup(n.getParent());
    }


}
