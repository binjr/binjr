/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.diagnostic.DiagnosticException;
import eu.binjr.common.diagnostic.HotSpotDiagnosticHelper;
import eu.binjr.common.function.CheckedConsumer;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.common.version.Version;
import eu.binjr.core.dialogs.ConsoleStage;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Provides access to the application's environmental properties
 *
 * @author Frederic Thevenet
 */
public class AppEnvironment {
    public static final String APP_NAME = "binjr";
    public static final String HTTP_GITHUB_REPO = "https://github.com/binjr/binjr";
    public static final String HTTP_WWW_BINJR_EU = "https://binjr.eu";
    public static final String HTTP_BINJR_WIKI = "https://binjr.eu/documentation/user_guide/main/";
    public static final String BINJR_PUBLIC_KEY_URL = "https://binjr.eu/openpgpkey/binjr_dev_pub_keys.asc";
    public static final byte[] BINJR_PUBLIC_FINGER_PRINT = decode(
            "0xE3", "0xD2", "0xF8", "0x00", "0xBE", "0x2B", "0x44", "0xE5", "0x97", "0x44",
            "0x7F", "0x29", "0x41", "0x2E", "0xC8", "0xA8", "0x54", "0x00", "0xAC", "0x3F");
    public static final String COPYRIGHT_NOTICE = "Copyright © 2016-2020 Frederic Thevenet";
    public static final String LICENSE = "Apache-2.0";
    public static final String PORTABLE_PROPERTY = "binjr.portable";
    public static final String MINIMUM_PLUGIN_API_LEVEL = "3.0.0";
    public static final String PLUGIN_API_LEVEL = "3.0.0";
    private static final Logger logger = Logger.create(AppEnvironment.class);
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private final BooleanProperty resizableDialogs = new SimpleBooleanProperty(false);
    private final BooleanProperty debugMode = new SimpleBooleanProperty(false);
    private final Manifest manifest;
    private final BooleanProperty updateCheckDisabled = new SimpleBooleanProperty(false);
    private final Property<StageStyle> windowsStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);
    private final Property<AppPackaging> packaging = new SimpleObjectProperty<>(AppPackaging.UNKNOWN);
    private final StringProperty updateRepoSlug = new SimpleStringProperty("binjr/binjr");
    private final BooleanProperty signatureVerificationDisabled = new SimpleBooleanProperty(false);
    private Optional<String> associatedWorkspace;
    private Path systemPluginPath;

    private AppEnvironment() {
        this.manifest = getManifest();
        debugMode.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ConsoleStage.show();
                logger.warn("Entering debug console");
            } else {
                logger.info("Leaving debug console");
                ConsoleStage.hide();
            }
        });
    }

    /**
     * Get the singleton instance for the {@link AppEnvironment} class.
     *
     * @return the singleton instance for the {@link AppEnvironment} class.
     */
    public static AppEnvironment getInstance() {
        return EnvironmentHolder.instance;
    }

    private static byte[] decode(String... strings) {
        int len = strings.length;
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = Integer.decode(strings[i]).byteValue();
        }
        return bytes;
    }

    /**
     * Returns the version information held in the containing jar's manifest
     *
     * @return the version information held in the containing jar's manifest
     */
    public Version getVersion() {
        return getVersion(this.manifest);
    }

    public void processCommandLineOptions(Application.Parameters parameters) {
        this.associatedWorkspace = parameters.getUnnamed()
                .stream()
                .filter(s -> s.endsWith(".bjr"))
                .filter(s -> Files.exists(Paths.get(s)))
                .findFirst();

        parameters.getNamed().forEach((name, val) -> {
            switch (name.toLowerCase()) {
                case "update-repo":
                    this.setUpdateRepoSlug(val);
                    break;
                case "windows-style":
                    try {
                        this.setWindowsStyle(StageStyle.valueOf(val.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Unknown windows style specified: " + val, e);
                    }
                    break;
                case "resizable-dialogs":
                    this.setResizableDialogs(Boolean.parseBoolean(val));
                    break;
                case "disable-update-check":
                    this.setUpdateCheckDisabled(Boolean.parseBoolean(val));
                    break;
                case "packaging":
                    try {
                        this.setPackaging(AppPackaging.valueOf(val.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Unknown app packaging type specified: " + val, e);
                    }
                    break;
                case "disable-signature-verification":
                    this.setSignatureVerificationDisabled(!Boolean.parseBoolean(val));
                    break;
                case "log-level":
                    UserPreferences.getInstance().rootLoggingLevel.set(Level.valueOf(val));
                    break;
                case "system-plugins-path":
                    this.setSystemPluginPath(val);
                    break;
                case "log-file":
                    break;
            }
        });
    }

    public Optional<String> getAssociatedWorkspace() {
        return associatedWorkspace;
    }

    /**
     * Returns a version number extracted from the specified manifest.
     * <p>The version number is extracted according to the following rules:</p>
     * <ul>
     * <li>If the key {@code Specification-Version} is present and can be formatted as valid {@link Version} instance then it is returned, else</li>
     * <li>If the key {@code Implementation-Version} is present and can be formatted as valid {@link Version} instance then it is returned, else</li>
     * <li> {@code Version.emptyVersion} is returned</li>
     * </ul>
     *
     * @param manifest the {@link Manifest} from which a version number should be extracted.
     * @return a version number extracted from the specified manifest.
     */
    public Version getVersion(Manifest manifest) {
        if (manifest != null) {
            String[] keys = new String[]{"Specification-Version", "Implementation-Version"};
            for (String key : keys) {
                String value = manifest.getMainAttributes().getValue(key);
                if (value != null) {
                    try {
                        return new Version(value);
                    } catch (IllegalArgumentException e) {
                        logger.error("Could not decode version number: " + value + ": " + e.getMessage());
                        logger.debug(() -> "Full stack", e);
                    }
                }
            }
        }
        return Version.emptyVersion;
    }

    public Version getVersion(Class aClass) {
        return this.getVersion(this.getManifest(aClass));
    }

    public Manifest getManifest() {
        return getManifest(this.getClass());
    }

    /**
     * Returns the manifest for the JAR which packages the specified class.
     *
     * @param aClass the class to packaged by the jar to return the manifest for.
     * @return the manifest for the JAR which packages the specified class.
     */
    public Manifest getManifest(Class<?> aClass) {
        String className = aClass.getSimpleName() + ".class";
        String classPath = aClass.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) + "/META-INF/MANIFEST.MF";
            try {
                return new Manifest(new URL(manifestPath).openStream());
            } catch (IOException e) {
                logger.error("Error extracting manifest from jar: " + e.getMessage());
                logger.debug(() -> "Full stack", e);
            }
        }
        logger.warn("Could not extract MANIFEST from jar!");
        return null;
    }

    /**
     * Returns a short description for the application.
     *
     * @return a short description for the application.
     */
    public String getAppDescription() {
        return AppEnvironment.APP_NAME + " v" + getVersion() + " (build #" + getBuildNumber() + ")";
    }

    /**
     * Returns the build number from the manifest
     *
     * @return the build number from the manifest
     */
    public String getBuildNumber() {
        if (manifest != null) {
            String value = manifest.getMainAttributes().getValue("Build-Number");
            if (value != null && value.trim().length() > 0) {
                try {
                    return value;
                } catch (NumberFormatException e) {
                    logger.error("Could not decode build number: " + value + ": " + e.getMessage());
                    logger.debug(() -> "Full stack", e);
                }
            }
        }
        return "0";
    }

    /**
     * Returns a list of system properties
     *
     * @return a list of system properties
     */
    public List<SysInfoProperty> getSysInfoProperties() {
        List<SysInfoProperty> sysInfo = new ArrayList<>();
        sysInfo.add(new SysInfoProperty("Version", getVersion().toString() + " (build #" + getBuildNumber() + ")"));
        sysInfo.add(new SysInfoProperty("Java Version", System.getProperty("java.version")));
        sysInfo.add(new SysInfoProperty("JavaFX Version", System.getProperty("javafx.version")));
        sysInfo.add(new SysInfoProperty("Java Vendor", System.getProperty("java.vendor")));
        sysInfo.add(new SysInfoProperty("Java VM name", System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")"));
        sysInfo.add(new SysInfoProperty("Java Home", System.getProperty("java.home")));
        sysInfo.add(new SysInfoProperty("Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")"));
        sysInfo.add(new SysInfoProperty("System Architecture", System.getProperty("os.arch")));
        sysInfo.add(new SysInfoProperty("JVM Heap Stats", getHeapStats()));
        sysInfo.add(new SysInfoProperty("Garbage Collectors", getGcNames()));
        return sysInfo;
    }

    public Version getJavaVersion() {
        try {
            return Version.parseVersion(System.getProperty("java.version"));
        } catch (Exception e) {
            logger.error("Error parsing Java version: " + e.getMessage());
            logger.debug("Call Stack", e);
        }
        return Version.emptyVersion;
    }

    public Version getJavaFxVersion() {
        try {
            return Version.parseVersion(System.getProperty("javafx.version"));
        } catch (Exception e) {
            logger.error("Error parsing javafx version: " + e.getMessage());
            logger.debug("Call Stack", e);
        }
        return Version.emptyVersion;
    }

    /**
     * Returns the family of the currently running OS
     *
     * @return the family of the currently running OS
     */
    public OsFamily getOsFamily() {
        if (OS_NAME.startsWith("windows")) {
            return OsFamily.WINDOWS;
        }
        if (OS_NAME.startsWith("mac")) {
            return OsFamily.OSX;
        }
        if (OS_NAME.startsWith("linux")) {
            return OsFamily.LINUX;
        } else {
            return OsFamily.UNSUPPORTED;
        }
    }

    /**
     * Returns true is debug mode is enabled, false otherwise.
     *
     * @return true is debug mode is enabled, false otherwise.
     */
    public boolean isDebugMode() {
        return debugMode.get();
    }

    /**
     * Set to true to enable debug mode, set to false to disable it.
     *
     * @param value true to enable debug mode, set to false to disable it.
     */
    public void setDebugMode(boolean value) {
        debugMode.setValue(value);
    }

    /**
     * The debugMode Property
     *
     * @return the debugMode Property
     */
    public BooleanProperty debugModeProperty() {
        return debugMode;
    }

    /**
     * Returns true if modal dialogs should be resizable, false otherwise.
     *
     * @return true if modal dialogs should be resizable, false otherwise.
     */
    public boolean isResizableDialogs() {
        return resizableDialogs.getValue();
    }

    /**
     * Set to true if modal dialogs should be resizable, false otherwise.
     *
     * @param value true if modal dialogs should be resizable, false otherwise.
     */
    public void setResizableDialogs(boolean value) {
        resizableDialogs.setValue(value);
    }

    /**
     * The resizableDialogs property.
     *
     * @return the resizableDialogs property.
     */
    public BooleanProperty resizableDialogsProperty() {
        return resizableDialogs;
    }

    /**
     * <p>Set to true to prevent binjr from checking for update</p>
     * <p><b>Remark:</b> This setting overrides the user preference to check for updates.</p>
     *
     * @param updateCheckDisabled true to prevent binjr from checking for update
     */
    public void setUpdateCheckDisabled(boolean updateCheckDisabled) {
        this.updateCheckDisabled.setValue(updateCheckDisabled);
    }

    /**
     * Returns he updateCheckDisabled property.
     *
     * @return The updateCheckDisabled property.
     */
    public BooleanProperty updateCheckDisabledProperty() {
        return updateCheckDisabled;
    }

    /**
     * Returns true if binjr is prevented from checking for update, false otherwise.
     *
     * @return true if binjr is prevented from checking for update, false otherwise.
     */
    public boolean isDisableUpdateCheck() {
        return updateCheckDisabled.getValue();
    }

    public Property<StageStyle> windowsStyleProperty() {
        return windowsStyle;
    }

    public StageStyle getWindowsStyle() {
        return windowsStyle.getValue();
    }

    public void setWindowsStyle(StageStyle windowsStyle) {
        this.windowsStyle.setValue(windowsStyle);
    }

    public String getUpdateRepoSlug() {
        return updateRepoSlug.get();
    }

    public void setUpdateRepoSlug(String updateRepoSlug) {
        this.updateRepoSlug.set(updateRepoSlug);
    }

    public StringProperty updateRepoSlugProperty() {
        return updateRepoSlug;
    }

    public boolean isSignatureVerificationDisabled() {
        return signatureVerificationDisabled.get();
    }

    public void setSignatureVerificationDisabled(boolean value) {
        signatureVerificationDisabled.setValue(value);
    }

    public BooleanProperty signatureVerificationDisabledProperty() {
        return signatureVerificationDisabled;
    }

    public JvmImplementation getRunningJvm() {
        String vmName = System.getProperty("java.vm.name");
        if (vmName != null) {
            if (vmName.toLowerCase(Locale.US).contains("OpenJDK".toLowerCase(Locale.US)) ||
                    vmName.toLowerCase(Locale.US).contains("Hotspot".toLowerCase(Locale.US))) {
                return JvmImplementation.HOTSPOT;
            }
            if (vmName.toLowerCase(Locale.US).contains("OpenJ9".toLowerCase(Locale.US))) {
                return JvmImplementation.OPENJ9;
            }
        }
        return JvmImplementation.UNSUPPORTED;

    }

    private String getHeapStats() {
        Runtime rt = Runtime.getRuntime();
        double maxMB = rt.maxMemory() / 1024.0 / 1024.0;
        double committedMB = (double) rt.totalMemory() / 1024.0 / 1024.0;
        double usedMB = ((double) rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0;
        return String.format(
                "Max: %.0fMB | Committed: %.0fMB | Used: %.0fMB",
                maxMB,
                committedMB,
                usedMB
        );
    }

    private String getGcNames() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(MemoryManagerMXBean::getName)
                .collect(Collectors.joining(", "));
    }

    public AppPackaging getPackaging() {
        return packaging.getValue();
    }

    public void setPackaging(AppPackaging value) {
        packaging.setValue(value);
    }

    public Property<AppPackaging> packagingProperty() {
        return packaging;
    }

    private void setSystemPluginPath(String val) {
        try {
            this.systemPluginPath = Path.of(val);
            if (!systemPluginPath.isAbsolute()) {
                // resolve the path on top of entry point jar's location
                var rootPath = Paths.get(getClass()
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()).getParent();
                logger.debug(() -> "binjr-core path=" + rootPath);
                this.systemPluginPath = rootPath.resolve(val);
            }
        } catch (Exception e) {
            logger.error("Cannot set system plugin path: " + e.getMessage());
            logger.debug(() -> "Stack trace", e);
        }
        logger.debug(() -> "systemPluginPath=" + systemPluginPath);
    }

    public Path getSystemPluginPath() {
        return this.systemPluginPath;
    }

    private static class EnvironmentHolder {
        private final static AppEnvironment instance = new AppEnvironment();
    }

    public void bindHeapDumpPreferences() {
        try {
            switch (AppEnvironment.getInstance().getRunningJvm()) {
                case HOTSPOT:
                    bindPrefToVmOption(UserPreferences.getInstance().heapDumpOnOutOfMemoryError, HotSpotDiagnosticHelper::setHeapDumpOnOutOfMemoryError);
                    bindPrefToVmOption(UserPreferences.getInstance().heapDumpPath, HotSpotDiagnosticHelper::setHeapDumpPath);
                    break;
                case OPENJ9:
                case UNSUPPORTED:
                default:
                    logger.debug("This diagnostic feature is not supported");
            }
        } catch (Throwable e) {
            logger.error("Failed to bind heap dump preferences" + e.getMessage());
            logger.debug(e);
        }
    }

    private <T> void bindPrefToVmOption(ObservablePreference<T> pref, CheckedConsumer<T, DiagnosticException> optionSetter) {
        try {
            optionSetter.accept(pref.get());
        } catch (DiagnosticException e) {
            logger.error(e.getMessage(), e);
        }
        pref.property().addListener((val, oldVal, newVal) -> {
            try {
                optionSetter.accept(newVal);
            } catch (DiagnosticException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

}
