/*
 *    Copyright 2017-2021 Frederic Thevenet
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

import eu.binjr.common.github.GithubApiHelper;
import eu.binjr.common.github.GithubRelease;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.version.Version;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import impl.org.controlsfx.skin.NotificationBar;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

/**
 * Defines a series of methods to manage updates
 *
 * @author Frederic Thevenet
 */
public class UpdateManager {
    private static final Logger logger = Logger.create(UpdateManager.class);
    private Path updatePackage = null;
    private Version updateVersion = null;
    private boolean restartRequested = false;
    private final GithubApiHelper github;
    private final PlatformUpdater platformUpdater;

    private static class UpdateManagerHolder {
        private final static UpdateManager instance = new UpdateManager();
    }

    private UpdateManager() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        this.github = GithubApiHelper.createCloseable(URI.create(AppEnvironment.HTTP_WWW_BINJR_EU));
        UserPreferences.getInstance().githubUserName.property().addListener((observable, oldValue, newValue) -> {
            github.setUserCredentials(newValue, UserPreferences.getInstance().githubAuthToken.get().toPlainText());
        });
        UserPreferences.getInstance().githubAuthToken.property().addListener((observable, oldValue, newValue) -> {
            github.setUserCredentials(UserPreferences.getInstance().githubUserName.get(), newValue.toPlainText());
        });
        github.setUserCredentials(
                UserPreferences.getInstance().githubUserName.get(),
                UserPreferences.getInstance().githubAuthToken.get().toPlainText());

        switch (AppEnvironment.getInstance().getPackaging()) {
            case LINUX_TAR:
                platformUpdater = new LinuxTarballUpdater();
                break;
            case WIN_MSI:
                platformUpdater = new WindowsMsiUpdater();
                break;
            case MAC_DMG:
            case MAC_TAR:
            case WIN_ZIP:
            case LINUX_DEB:
            case LINUX_RPM:
            case UNKNOWN:
            default:
                platformUpdater = new NotifyOnlyUpdater();
                break;
        }
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

    private void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Consumer<Version> upToDate, Runnable onFailure, boolean forceCheck) {
        if (AppEnvironment.getInstance().isDisableUpdateCheck()) {
            logger.trace(() -> "Update check is explicitly disabled.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        if (!forceCheck && LocalDateTime.now().minus(1, ChronoUnit.HOURS).isBefore( UserPreferences.getInstance().lastCheckForUpdate.get())) {
            logger.trace(() -> "Available update check ignored as it already took place less than 1 hour ago.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        UserPreferences.getInstance().lastCheckForUpdate.set(LocalDateTime.now());
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

    private void asyncDownloadUpdatePackage(GithubRelease release, Consumer<Path> onDownloadComplete, Consumer<Throwable> onFailure) {
        Task<Path> downloadTask = new Task<Path>() {
            @Override
            protected Path call() throws Exception {
                var targetDir = Files.createTempDirectory("binjr-update_");
                String packageAssetName = String.format("binjr-%s_%s.%s",
                        release.getVersion(),
                        AppEnvironment.getInstance().getOsFamily().getPlatformClassifier(),
                        AppEnvironment.getInstance().getPackaging().getBundleExtension());
                var packagePath = downloadAsset(release, packageAssetName, targetDir);
                if (!AppEnvironment.getInstance().isSignatureVerificationDisabled()) {
                    var sigPath = downloadAsset(release, packageAssetName + ".asc", targetDir);
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
                platformUpdater.launchUpdater(updatePackage, updateVersion, restartRequested);
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
        List<Action> actions = new ArrayList<>();
        actions.add(
                new Action("More info", event -> {
                    URL newReleaseUrl = release.getHtmlUrl();
                    if (newReleaseUrl != null) {
                        try {
                            Dialogs.launchUrlInExternalBrowser(newReleaseUrl);
                        } catch (IOException | URISyntaxException e) {
                            logger.error("Failed to launch url in browser " + newReleaseUrl, e);
                        }
                    }
                }));
        if (platformUpdater.isInAppUpdateSupported()) {
            actions.add(new Action("Download update", event -> {
                this.asyncDownloadUpdatePackage(
                        release,
                        path -> {
                            updatePackage = path;
                            updateVersion = release.getVersion();
                            showUpdateReadyNotification(root);
                        },
                        exception -> Dialogs.notifyException("Error downloading update", exception, root));
                dismissNotificationPopup((Node) event.getSource());
            }));
        }
        n.action(actions.toArray(Action[]::new));
        n.showInformation();
    }

    private Path downloadAsset(GithubRelease release, String assetName, Path targetDir) throws IOException, URISyntaxException {
        var asset = release.getAssets()
                .stream()
                .filter(a -> a.getName().equalsIgnoreCase(assetName))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Unknown asset " + assetName +
                        " for release " +
                        release.getName()));
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
        URL publicKeyUrl = new URL(AppEnvironment.BINJR_PUBLIC_KEY_URL);
        try (var packageStream = Files.newInputStream(updatePath, StandardOpenOption.READ)) {
            try (var sigStream = Files.newInputStream(sigPath, StandardOpenOption.READ)) {
                try (var keyStream = publicKeyUrl.openStream()) {
                    if (!verifyOpenPGP(packageStream, sigStream, keyStream)) {
                        throw new UnsupportedOperationException("Update package's signature could not be verified.");
                    }
                    logger.debug("GPG signature verified successfully for " + updatePath);
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
        if (Arrays.compare(AppEnvironment.BINJR_PUBLIC_FINGER_PRINT, key.getFingerprint()) != 0) {
            throw new IllegalArgumentException("Cannot verify signature: Unexpected fingerprint for the key downloaded at " +
                    AppEnvironment.BINJR_PUBLIC_KEY_URL);
        }
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
        if (n instanceof NotificationBar notifBar) {
            // found it, hide the popup.
            notifBar.hide();
            return;
        }
        // keep looking.
        dismissNotificationPopup(n.getParent());
    }
}
