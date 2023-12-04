/*
 *    Copyright 2017-2023 Frederic Thevenet
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

import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.github.GitHubApiHelper;
import eu.binjr.common.github.GithubRelease;
import eu.binjr.common.io.ProxyConfiguration;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.version.Version;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
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
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final AppEnvironment appEnv = AppEnvironment.getInstance();
    private Path updatePackage = null;
    private Version updateVersion = null;
    private boolean restartRequested = false;
    private GitHubApiHelper github;
    private final PlatformUpdater platformUpdater;
    private final ReadWriteLockHelper ghhMonitor = new ReadWriteLockHelper();

    private static class UpdateManagerHolder {
        private final static UpdateManager instance = new UpdateManager();
    }

    private void resetGithubHelper() {
        ghhMonitor.write().tryLock(() -> {
            if (this.github != null) {
                try {
                    github.close();
                } catch (IOException e) {
                    logger.error("Error while attempting to close GitHub helper: " + e.getMessage());
                    logger.debug("StackTrace", e);
                }
            }
            this.github = GitHubApiHelper.of(URI.create(AppEnvironment.HTTP_WWW_BINJR_EU),
                    new ProxyConfiguration(userPrefs.enableHttpProxy.get(),
                            userPrefs.httpProxyHost.get(),
                            userPrefs.httpProxyPort.get().intValue(),
                            userPrefs.useHttpProxyAuth.get(),
                            userPrefs.httpProxyLogin.get(),
                            userPrefs.httpProxyPassword.get().toPlainText().toCharArray()),
                    userPrefs.githubUserName.get(),
                    userPrefs.githubAuthToken.get().toPlainText());
        });
    }

    private UpdateManager() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        resetGithubHelper();
        userPrefs.enableHttpProxy.property().addListener(observable -> resetGithubHelper());
        userPrefs.httpProxyHost.property().addListener(observable -> resetGithubHelper());
        userPrefs.httpProxyPort.property().addListener(observable -> resetGithubHelper());
        userPrefs.useHttpProxyAuth.property().addListener(observable -> resetGithubHelper());
        userPrefs.httpProxyLogin.property().addListener(observable -> resetGithubHelper());
        userPrefs.httpProxyPassword.property().addListener(observable -> resetGithubHelper());
        userPrefs.githubUserName.property().addListener(observable -> resetGithubHelper());
        userPrefs.githubAuthToken.property().addListener(observable -> resetGithubHelper());
        platformUpdater = switch (appEnv.getPackaging()) {
            case LINUX_TAR -> new LinuxTarballUpdater();
            case WIN_MSI -> new WindowsMsiUpdater();
            case MAC_DMG, MAC_TAR, WIN_ZIP, LINUX_DEB, LINUX_RPM, UNKNOWN -> new NotifyOnlyUpdater();
        };
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
    public void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable,
                                    Consumer<Version> upToDate,
                                    Runnable onFailure) {
        asyncCheckForUpdate(newReleaseAvailable, upToDate, onFailure, false);
    }

    /**
     * Force an async check for available update and ignore 1 check per hour limit.
     *
     * @param newReleaseAvailable The delegate run in the event that a new release is available
     * @param upToDate            The delegate to run in the event that tha current version is up to date
     * @param onFailure           The delegate to run in the event of an error while checking for an update
     */
    public void asyncForcedCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable,
                                          Consumer<Version> upToDate,
                                          Runnable onFailure) {
        asyncCheckForUpdate(newReleaseAvailable, upToDate, onFailure, true);
    }

    private void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable,
                                     Consumer<Version> upToDate,
                                     Runnable onFailure,
                                     boolean forceCheck) {
        if (appEnv.isDisableUpdateCheck()) {
            logger.trace(() -> "Update check is explicitly disabled.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        if (!forceCheck && LocalDateTime.now().minus(1, ChronoUnit.HOURS).isBefore(userPrefs.lastCheckForUpdate.get())) {
            logger.trace(() -> "Available update check ignored as it already took place less than 1 hour ago.");
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        userPrefs.lastCheckForUpdate.set(LocalDateTime.now());
        Task<Optional<GithubRelease>> getLatestTask = new Task<>() {
            @Override
            protected Optional<GithubRelease> call() throws Exception {
                logger.trace("getNewRelease running on " + Thread.currentThread().getName());
                return ghhMonitor.read().tryLock(() -> github.getLatestRelease(appEnv.getUpdateRepoSlug())
                        .filter(r -> r.getVersion().compareTo(appEnv.getVersion()) > 0)).orElseThrow();
            }
        };
        getLatestTask.setOnSucceeded(workerStateEvent -> {
            logger.trace("UI update running on " + Thread.currentThread().getName());
            Optional<GithubRelease> latest = getLatestTask.getValue();
            Version current = appEnv.getVersion();
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

    private void asyncDownloadUpdatePackage(GithubRelease release,
                                            Consumer<Path> onDownloadComplete,
                                            Consumer<Throwable> onFailure) {
        Task<Path> downloadTask = new Task<Path>() {
            @Override
            protected Path call() throws Exception {
                var targetDir = Files.createTempDirectory(userPrefs.temporaryFilesRoot.get(), "binjr-update_");
                String packageAssetName = String.format("binjr-%s_%s.%s",
                        release.getVersion(),
                        appEnv.getOsFamily().getPlatformClassifier(),
                        appEnv.getPackaging().getBundleExtension());
                var packagePath = downloadAsset(release, packageAssetName, targetDir);
                if (!appEnv.isSignatureVerificationDisabled()) {
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
        if (!appEnv.isDisableUpdateCheck() && updatePackage != null) {
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
                        appEnv.getVersion() +
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
                Dialogs.dismissParentNotificationPopup((Node) event.getSource());
            }));
        }
        n.action(actions.toArray(Action[]::new));
        n.showInformation();
    }

    private Path downloadAsset(GithubRelease release,
                               String assetName,
                               Path targetDir) throws Exception {
        var asset = release.getAssets()
                .stream()
                .filter(a -> a.getName().equalsIgnoreCase(assetName))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Unknown asset " + assetName +
                        " for release " +
                        release.getName()));
        logger.info("Downloading asset from " + asset.getBrowserDownloadUrl());
        return ghhMonitor.read().tryLock(() -> github.downloadAsset(asset, targetDir)).orElseThrow();
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
                    Dialogs.dismissParentNotificationPopup((Node) event.getSource());
                }));
        n.showInformation();
    }

    private void restartApp(Node root) {
        var stage = NodeUtils.getStage(root);
        restartRequested = true;
        if (stage != null) {
            var handler = stage.getOnCloseRequest();
            if (handler != null) {
                handler.handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        }
    }

    public void verifyUpdatePackage(Path updatePath, Path sigPath) throws IOException, PGPException {
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

    private boolean verifyOpenPGP(InputStream in,
                                  InputStream signature,
                                  InputStream keyIn) throws IOException, PGPException {
        Objects.requireNonNull(in, "File input stream cannot be null");
        Objects.requireNonNull(signature, "Signature input stream cannot be null");
        Objects.requireNonNull(keyIn, "Key input stream cannot be null");
        signature = PGPUtil.getDecoderStream(signature);
        JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(signature);
        PGPSignature sig = ((PGPSignatureList) pgpFactory.nextObject()).get(0);
        PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn),
                new JcaKeyFingerprintCalculator());
        PGPPublicKey key = pgpPubRingCollection.getPublicKey(sig.getKeyID());
        if (Arrays.compare(AppEnvironment.BINJR_PUBLIC_FINGER_PRINT, key.getFingerprint()) != 0 &&
                Arrays.compare(AppEnvironment.ALT_BINJR_PUBLIC_FINGER_PRINT, key.getFingerprint()) != 0) {
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
}
