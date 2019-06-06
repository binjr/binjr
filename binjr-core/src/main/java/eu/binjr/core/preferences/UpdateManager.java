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

import eu.binjr.common.github.GithubApiHelper;
import eu.binjr.common.github.GithubRelease;
import eu.binjr.common.version.Version;
import eu.binjr.core.data.async.AsyncTaskManager;
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
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private Version updateVersion = null;
    private boolean restartRequested = false;
    private final GithubApiHelper github;

    private static class UpdateManagerHolder {
        private final static UpdateManager instance = new UpdateManager();
    }

    private UpdateManager() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
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
                var targetDir = Files.createTempDirectory("binjr-update_");
                var packagePath = downloadAsset(release, AppEnvironment.getInstance().getOsFamily(), false, targetDir);
                if (!AppEnvironment.getInstance().isSignatureVerificationDisabled()) {
                    var sigPath = downloadAsset(release, AppEnvironment.getInstance().getOsFamily(), true, targetDir);
                    verifyUpdatePackage(packagePath, sigPath);
                }
                return packagePath;
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
                        break;
                    case OSX:
                    case LINUX:
                        File jar = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
                        File versionDirectory = jar.getParentFile().getParentFile();
                        File rootDirectory = versionDirectory.getParentFile();
                        final StringBuilder command = new StringBuilder();
                        command.append("(");
                        command.append("cd \"").append(rootDirectory.getPath()).append("\" && ");
                        command.append("cp -rl \"").append(versionDirectory.getAbsolutePath()).append("\" \"").append(updateVersion).append("\" && ");
                        command.append("while read file; do test -f \"./").append(updateVersion).append("/$file\" && rm \"./").append(updateVersion).append("/$file\"; done < \"").append(versionDirectory.getAbsolutePath()).append("/.installed\" && ");
                        command.append("tar xzf \"").append(updatePackage).append("\" \"").append(updateVersion).append("\" && ");
                        command.append("ln -s \"").append(updateVersion).append("/binjr\" \"new_binjr").append("\" && ");
                        command.append("mv \"new_binjr\" \"binjr\" && "); // atomic upgrade
                        command.append("rm -rf \"").append(versionDirectory.getAbsolutePath()).append("\" && ");
                        command.append("rm \"").append(updatePackage.toAbsolutePath()).append("\"");
                        command.append(") > ").append(new File(rootDirectory, "binjr-install.log").getAbsolutePath()).append(" 2>&1");
                        if (restartRequested) {
                            command.append(" ; ").append(new File(rootDirectory, "binjr").getPath());
                        }
                        processBuilder.command(
                                "sh",
                                "-c",
                                command.toString());
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
                                updateVersion = release.getVersion();
                                showUpdateReadyNotification(root);
                            },
                            exception -> Dialogs.notifyException("Error downloading update", exception, root));
                    dismissNotificationPopup((Node) event.getSource());
                }));
        n.showInformation();
    }

    private Path downloadAsset(GithubRelease release, OsFamily os, boolean isSignature, Path targetDir) throws IOException, URISyntaxException {
        var asset = github.getAssets(release)
                .stream()
                .filter(a -> a.getName().equalsIgnoreCase(
                        String.format("binjr-%s_%s.%s%s",
                                release.getVersion(),
                                os.getPlatformClassifier(),
                                os.getBundleExtension(),
                                (isSignature ? ".asc" : ""))))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Failed to find " +
                        (isSignature ? "signature" : "package") + " for release " +
                        release.getName() +
                        " / platform " +
                        AppEnvironment.getInstance().getOsFamily().getPlatformClassifier()));
        logger.info("Downloading asset from " + asset.getBrowserDownloadUrl());
        return github.downloadAsset(asset, targetDir);
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

    private void verifyUpdatePackage(Path updatePath, Path sigPath) throws IOException, PGPException {
        try (var packageStream = Files.newInputStream(updatePath, StandardOpenOption.READ)) {
            try (var sigStream = Files.newInputStream(sigPath, StandardOpenOption.READ)) {
                try (var keyStream = this.getClass().getResourceAsStream("/eu/binjr/pubkey/5400AC3F.asc")) {
                    if (!verifyOpenPGP(packageStream, sigStream, keyStream)) {
                        throw new UnsupportedOperationException("Update package's signature could not be verified.");
                    }
                }
            }
        }
    }

    private boolean verifyOpenPGP(InputStream in, InputStream signature, InputStream keyIn) throws IOException, PGPException {
        Objects.requireNonNull(in, "File input stream cannot be null");
        Objects.requireNonNull(signature, "Signature input stream cannot be null");
        Objects.requireNonNull(keyIn, "Key input stream cannot be null");
        signature = PGPUtil.getDecoderStream(signature);
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(signature);
        PGPSignature sig = ((PGPSignatureList) pgpFact.nextObject()).get(0);
        PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn),
                new JcaKeyFingerprintCalculator());
        PGPPublicKey key = pgpPubRingCollection.getPublicKey(sig.getKeyID());
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key);
        byte[] buff = new byte[1024];
        int read = 0;
        while ((read = in.read(buff)) != -1) {
            sig.update(buff, 0, read);
        }
        return sig.verify();
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
