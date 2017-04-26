package eu.fthevenet.binjr.preferences;

import eu.fthevenet.binjr.dialogs.UserInterfaceThemes;
import eu.fthevenet.util.github.GithubApi;
import eu.fthevenet.util.github.GithubRelease;
import eu.fthevenet.util.version.Version;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Stores the global user preferences for the application.
 *
 * @author Frederic Thevenet
 */
public class GlobalPreferences {
    private static final Logger logger = LogManager.getLogger(GlobalPreferences.class);
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String HTTP_WWW_BINJR_EU = "http://www.binjr.eu";
    public static final String HTTP_BINJR_WIKI = "https://github.com/fthevenet/binjr/wiki";
    public static final String HTTP_LATEST_RELEASE = "https://github.com/fthevenet/binjr/releases/latest";
    private static final String CHART_ANIMATION_ENABLED = "enableChartAnimation";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String SAMPLE_SYMBOLS_VISIBLE = "sampleSymbolsVisible";
    private static final String DOWN_SAMPLING_ENABLED = "enableDownSampling";
    private static final String BINJR_GLOBAL = "binjr/global";
    private static final String USE_SOURCE_COLORS = "useSourceColors";
    private static final String MOST_RECENT_SAVE_FOLDER = "mostRecentSaveFolder";
    private static final String MOST_RECENT_SAVED_WORKSPACE = "mostRecentSavedWorkspace";
    private static final String LOAD_LAST_WORKSPACE_ON_STARTUP = "loadLastWorkspaceOnStartup";
    public static final String CHECK_FOR_UPDATE_ON_START_UP = "checkForUpdateOnStartUp";
    private static final String UI_THEME_NAME = "userInterfaceTheme";
    private static final String RECENT_FILES = "recentFiles";
    private static final int MAX_RECENT_FILES = 20;
    public static final String GITHUB_OWNER = "fthevenet";
    public static final String GITHUB_REPO = "binjr";
    public static final String ENABLE_CROSSHAIR_ON_KEY_PRESSED = "enableCrosshairOnKeyPressed";
    public static final String HORIZONTAL_MARKER_ON = "horizontalMarkerOn";
    public static final String VERTICAL_MARKER_ON = "verticalMarkerOn";
    public static final String LAST_CHECK_FOR_UPDATE = "lastCheckForUpdate";


    private final Manifest manifest;
    private BooleanProperty loadLastWorkspaceOnStartup;
    private BooleanProperty downSamplingEnabled;
    private IntegerProperty downSamplingThreshold;
    private BooleanProperty sampleSymbolsVisible;
    private BooleanProperty chartAnimationEnabled;
    private Preferences prefs;
    private BooleanProperty useSourceColors;
    private StringProperty mostRecentSaveFolder;
    private Property<Path> mostRecentSavedWorkspace;
    private BooleanProperty checkForUpdateOnStartUp;
    private Property<UserInterfaceThemes> userInterfaceTheme;
    private Deque<String> recentFiles;
    private BooleanProperty enableCrosshairOnKeyPressed;
    private BooleanProperty horizontalMarkerOn;
    private BooleanProperty verticalMarkerOn;
    private Property<LocalDateTime> lastCheckForUpdate;



    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }

    private GlobalPreferences() {
        prefs = Preferences.userRoot().node(BINJR_GLOBAL);
        mostRecentSaveFolder = new SimpleStringProperty(prefs.get(MOST_RECENT_SAVE_FOLDER, System.getProperty("user.home")));
        mostRecentSaveFolder.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                prefs.put(MOST_RECENT_SAVE_FOLDER, newValue);
            }
        });
        downSamplingEnabled = new SimpleBooleanProperty(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold = new SimpleIntegerProperty(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 5000));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        sampleSymbolsVisible = new SimpleBooleanProperty(prefs.getBoolean(SAMPLE_SYMBOLS_VISIBLE, false));
        sampleSymbolsVisible.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SAMPLE_SYMBOLS_VISIBLE, newValue));
        chartAnimationEnabled = new SimpleBooleanProperty(prefs.getBoolean(CHART_ANIMATION_ENABLED, true));
        chartAnimationEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHART_ANIMATION_ENABLED, newValue));
        useSourceColors = new SimpleBooleanProperty(prefs.getBoolean(USE_SOURCE_COLORS, true));
        useSourceColors.addListener((observable, oldValue, newValue) -> prefs.putBoolean(USE_SOURCE_COLORS, newValue));
        mostRecentSavedWorkspace = new SimpleObjectProperty<>(Paths.get(prefs.get(MOST_RECENT_SAVED_WORKSPACE, "Untitled")));
        mostRecentSavedWorkspace.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVED_WORKSPACE, newValue.toString()));
        loadLastWorkspaceOnStartup = new SimpleBooleanProperty(prefs.getBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, true));
        loadLastWorkspaceOnStartup.addListener((observable, oldValue, newValue) -> prefs.putBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, newValue));
        String recentFileString = prefs.get(RECENT_FILES, "");
        recentFiles = new ArrayDeque<>(Arrays.stream(recentFileString.split("\\|")).filter(s -> s != null && s.trim().length() > 0).collect(Collectors.toList()));
        userInterfaceTheme = new SimpleObjectProperty<>(UserInterfaceThemes.valueOf(prefs.get(UI_THEME_NAME, UserInterfaceThemes.MODERN.name())));
        userInterfaceTheme.addListener((observable, oldValue, newValue) -> prefs.put(UI_THEME_NAME, newValue.name()));
        checkForUpdateOnStartUp = new SimpleBooleanProperty(prefs.getBoolean(CHECK_FOR_UPDATE_ON_START_UP, true));
        checkForUpdateOnStartUp.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHECK_FOR_UPDATE_ON_START_UP, newValue));
        enableCrosshairOnKeyPressed = new SimpleBooleanProperty(prefs.getBoolean(ENABLE_CROSSHAIR_ON_KEY_PRESSED, false));
        enableCrosshairOnKeyPressed.addListener((observable, oldValue, newValue) -> prefs.putBoolean(ENABLE_CROSSHAIR_ON_KEY_PRESSED, newValue));
        horizontalMarkerOn = new SimpleBooleanProperty(prefs.getBoolean(HORIZONTAL_MARKER_ON, false));
        horizontalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(HORIZONTAL_MARKER_ON, newValue));
        verticalMarkerOn = new SimpleBooleanProperty(prefs.getBoolean(VERTICAL_MARKER_ON, true));
        verticalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(VERTICAL_MARKER_ON, newValue));
        lastCheckForUpdate = new SimpleObjectProperty<>(LocalDateTime.parse(prefs.get(LAST_CHECK_FOR_UPDATE, "1900-01-01T00:00:00"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        lastCheckForUpdate.addListener((observable, oldValue, newValue) -> prefs.put(LAST_CHECK_FOR_UPDATE, newValue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        this.manifest = getManifest();
        if (logger.isDebugEnabled()) {
            logger.debug("Global preferences initial values");
            logger.debug("  downSamplingThreshold = " + downSamplingThreshold.getValue());
            logger.debug("  sampleSymbolsVisible = " + sampleSymbolsVisible.getValue());
            logger.debug("  downSamplingEnabled = " + downSamplingEnabled.getValue());
            logger.debug("  mostRecentSaveFolder = " + mostRecentSaveFolder.getValue());
            logger.debug("  useSourceColors = " + useSourceColors.getValue());
            logger.debug("  sampleSymbolsVisible = " + sampleSymbolsVisible.getValue());
            logger.debug("  mostRecentSavedWorkspace = " + mostRecentSavedWorkspace.getValue());
            logger.debug("  loadLastWorkspaceOnStartup = " + loadLastWorkspaceOnStartup.getValue());
            logger.debug("  recentFileString = " + recentFileString);
        }
    }

    /**
     * Returns the singleton instance of {@link GlobalPreferences}
     *
     * @return the singleton instance of {@link GlobalPreferences}
     */
    public static GlobalPreferences getInstance() {
        return GlobalPreferencesHolder.instance;
    }

    /**
     * Returns true if the chart animation is enabled, false otherwise.
     *
     * @return true if the chart animation is enabled, false otherwise.
     */
    public Boolean getChartAnimationEnabled() {
        return downSamplingEnabled.getValue();
    }

    /**
     * Returns the chart animation property
     *
     * @return the chart animation property
     */
    public BooleanProperty chartAnimationEnabledProperty() {
        return chartAnimationEnabled;
    }

    /**
     * Enables or disables the chart animation
     *
     * @param chartAnimationEnabled true to enable the chart animation, false otherwise.
     */
    public void setChartAnimationEnabled(boolean chartAnimationEnabled) {
        this.chartAnimationEnabled.setValue(chartAnimationEnabled);
    }

    /**
     * Returns true if the chart symbols are visible, false otherwise.
     *
     * @return true if the chart symbols are visible, false otherwise.
     */
    public Boolean getSampleSymbolsVisible() {
        return sampleSymbolsVisible.getValue();
    }

    /**
     * Return the chart symbols visibility property
     *
     * @return the chart symbols visibility property
     */
    public BooleanProperty sampleSymbolsVisibleProperty() {
        return sampleSymbolsVisible;
    }

    /**
     * Sets the visibility of chart symbols
     *
     * @param sampleSymbolsVisible the visibility of chart symbols
     */
    public void setSampleSymbolsVisible(boolean sampleSymbolsVisible) {
        this.sampleSymbolsVisible.setValue(sampleSymbolsVisible);
    }

    /**
     * Returns true if series down-sampling is enabled, false otherwise.
     *
     * @return true if series down-sampling is enabled, false otherwise.
     */
    public Boolean getDownSamplingEnabled() {
        return downSamplingEnabled.getValue();
    }

    /**
     * Returns the down-sampling property
     *
     * @return the down-sampling property
     */
    public BooleanProperty downSamplingEnabledProperty() {
        return downSamplingEnabled;
    }

    /**
     * Enables or disables series down-sampling
     *
     * @param downSamplingEnabled true to enable series down-sampling, false otherwise.
     */
    public void setDownSamplingEnabled(boolean downSamplingEnabled) {
        this.downSamplingEnabled.setValue(downSamplingEnabled);
    }

    /**
     * Returns the series down-sampling threshold value
     *
     * @return the series down-sampling threshold value
     */
    public int getDownSamplingThreshold() {
        return downSamplingThreshold.getValue();
    }

    /**
     * Returns the property for the series down-sampling threshold value
     *
     * @return the property for the series down-sampling threshold value
     */
    public IntegerProperty downSamplingThresholdProperty() {
        return downSamplingThreshold;
    }

    /**
     * Sets the series down-sampling threshold value
     *
     * @param downSamplingThreshold the series down-sampling threshold value
     */
    public void setDownSamplingThreshold(int downSamplingThreshold) {
        this.downSamplingThreshold.setValue(downSamplingThreshold);
    }

    /**
     * Returns true if the series graph should use the colors defined in the source, false otherwise
     *
     * @return true if the series graph should use the colors defined in the source, false otherwise
     */
    public Boolean isUseSourceColors() {
        return useSourceColors.getValue();
    }

    /**
     * The useSourceColors property
     *
     * @return the useSourceColors property
     */
    public BooleanProperty useSourceColorsProperty() {
        return useSourceColors;
    }

    /**
     * Set to true to have the series graph use the colors defined in the source, false otherwise
     *
     * @param useSourceColors Set to true to have the series graph use the colors defined in the source, false otherwise
     */
    public void setUseSourceColors(Boolean useSourceColors) {
        this.useSourceColors.setValue(useSourceColors);
    }

    /**
     * Gets the path of the folder of the most recently saved item
     *
     * @return the path of the folder of the most recently saved item
     */
    public String getMostRecentSaveFolder() {
        String recentPath = mostRecentSaveFolder.getValue();
        if (recentPath == null || recentPath.trim().length() == 0) {
            recentPath = System.getProperty("user.home");
        }
        return recentPath;
    }

    /**
     * The mostRecentSaveFolder property
     *
     * @return the mostRecentSaveFolder property
     */
    public StringProperty mostRecentSaveFolderProperty() {
        return mostRecentSaveFolder;
    }

    /**
     * Sets the path of the folder of the most recently saved item
     *
     * @param mostRecentSaveFolder the path of the folder of the most recently saved item
     */
    public void setMostRecentSaveFolder(String mostRecentSaveFolder) {
        if (mostRecentSaveFolder == null) {
            throw new IllegalArgumentException("mostRecentSaveFolder parameter cannot be null");
        }
        this.mostRecentSaveFolder.setValue(mostRecentSaveFolder);
    }

    /**
     * Gets the path from the most recently saved workspace
     *
     * @return the path from the most recently saved workspace
     */
    public Path getMostRecentSavedWorkspace() {
        return mostRecentSavedWorkspace.getValue() == null ? Paths.get("untitled") : mostRecentSavedWorkspace.getValue();
    }

    /**
     * The mostRecentSavedWorkspace property
     *
     * @return the  mostRecentSavedWorkspace property
     */
    public Property<Path> mostRecentSavedWorkspaceProperty() {
        return mostRecentSavedWorkspace;
    }

    /**
     * Sets  the path from the most recently saved workspace
     *
     * @param mostRecentSavedWorkspace the path from the most recently saved workspace
     */
    public void setMostRecentSavedWorkspace(Path mostRecentSavedWorkspace) {
        if (mostRecentSavedWorkspace == null) {
            throw new IllegalArgumentException("mostRecentSavedWorkspace parameter cannot be null");
        }
        this.mostRecentSavedWorkspace.setValue(mostRecentSavedWorkspace);
    }

    /**
     * Returns true if  the most recently saved workspace should be re-opned on startup, false otherwise
     *
     * @return true if  the most recently saved workspace should be re-opned on startup, false otherwise
     */
    public boolean isLoadLastWorkspaceOnStartup() {
        return loadLastWorkspaceOnStartup.get();
    }

    /**
     * The loadLastWorkspaceOnStartup property
     *
     * @return the loadLastWorkspaceOnStartup property
     */
    public BooleanProperty loadLastWorkspaceOnStartupProperty() {
        return loadLastWorkspaceOnStartup;
    }

    /**
     * Sets to true if  the most recently saved workspace should be re-opned on startup, false otherwise
     *
     * @param loadLastWorkspaceOnStartup true if  the most recently saved workspace should be re-opned on startup, false otherwise
     */
    public void setLoadLastWorkspaceOnStartup(boolean loadLastWorkspaceOnStartup) {
        this.loadLastWorkspaceOnStartup.set(loadLastWorkspaceOnStartup);
    }

    /**
     * Gets the current UI theme
     *
     * @return the current UI theme
     */
    public UserInterfaceThemes getUserInterfaceTheme() {
        return userInterfaceTheme.getValue();
    }

    /**
     * The UI theme property
     *
     * @return the UI theme property
     */
    public Property<UserInterfaceThemes> userInterfaceThemeProperty() {
        return userInterfaceTheme;
    }

    /**
     * Sets the UI theme
     *
     * @param userInterfaceTheme the UI theme to apply
     */
    public void setUserInterfaceTheme(UserInterfaceThemes userInterfaceTheme) {
        this.userInterfaceTheme.setValue(userInterfaceTheme);
    }

    /**
     * Remove a path from the list of recently opened files
     *
     * @param value a path to remove from the list of recently opened files
     */
    public void removeFromRecentFiles(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value parameter cannot be null");
        }
        if (recentFiles.contains(value)) {
            recentFiles.remove(value);
            prefs.put(RECENT_FILES, recentFiles.stream().collect(Collectors.joining("|")));
        }
    }

    /**
     * Puts a path into the list of recently opened files
     *
     * @param value a path to put into the list of recently opened files
     */
    public void putToRecentFiles(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value parameter cannot be null");
        }
        if (recentFiles.contains(value)) {
            recentFiles.remove(value);
        }
        recentFiles.addFirst(value);
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }
        prefs.put(RECENT_FILES, recentFiles.stream().collect(Collectors.joining("|")));
    }

    /**
     * Gets the list of recently opened files
     *
     * @return the list of recently opened files
     */
    public Collection<String> getRecentFiles() {
        return recentFiles;
    }

    public boolean isCheckForUpdateOnStartUp() {
        return checkForUpdateOnStartUp.get();
    }

    public BooleanProperty checkForUpdateOnStartUpProperty() {
        return checkForUpdateOnStartUp;
    }

    public void setCheckForUpdateOnStartUp(boolean checkForUpdateOnStartUp) {
        this.checkForUpdateOnStartUp.set(checkForUpdateOnStartUp);
    }

    public boolean isEnableCrosshairOnKeyPressed() {
        return enableCrosshairOnKeyPressed.get();
    }

    public BooleanProperty enableCrosshairOnKeyPressedProperty() {
        return enableCrosshairOnKeyPressed;
    }

    public void setEnableCrosshairOnKeyPressed(boolean enableCrosshairOnKeyPressed) {
        this.enableCrosshairOnKeyPressed.set(enableCrosshairOnKeyPressed);
    }

    public boolean getHorizontalMarkerOn() {
        return horizontalMarkerOn.get();
    }

    public BooleanProperty horizontalMarkerOnProperty() {
        return horizontalMarkerOn;
    }

    public void setHorizontalMarkerOn(boolean horizontalMarkerOn) {
        this.horizontalMarkerOn.set(horizontalMarkerOn);
    }

    public boolean getVerticalMarkerOn() {
        return verticalMarkerOn.get();
    }

    public BooleanProperty verticalMarkerOnProperty() {
        return verticalMarkerOn;
    }

    public void setVerticalMarkerOn(boolean verticalMarkerOn) {
        this.verticalMarkerOn.set(verticalMarkerOn);
    }

    public LocalDateTime getLastCheckForUpdate() {
        return lastCheckForUpdate.getValue();
    }

    public Property<LocalDateTime> lastCheckForUpdateProperty() {
        return lastCheckForUpdate;
    }

    public void setLastCheckForUpdate(LocalDateTime lastCheckForUpdate) {
        this.lastCheckForUpdate.setValue(lastCheckForUpdate);
    }
    /**
     * Returns the version information held in the containing jar's manifest
     *
     * @return the version information held in the containing jar's manifest
     */
    public Version getManifestVersion() {
        if (manifest != null) {
            String[] keys = new String[]{"Specification-Version", "Implementation-Version"};
            for (String key : keys) {
                String value = manifest.getMainAttributes().getValue(key);
                if (value != null) {
                    try {
                        return new Version(value);
                    } catch (IllegalArgumentException e) {
                        logger.error("Could not parse version number: " + value, e);
                    }
                }
            }
        }
        return Version.emptyVersion;
    }

    /**
     * Returns a list of system properties
     *
     * @return a list of system properties
     */
    public List<SysInfoProperty> getSysInfoProperties() {
        Runtime rt = Runtime.getRuntime();
        double usedMB = ((double) rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        double percentUsage = (((double) rt.totalMemory() - rt.freeMemory()) / rt.totalMemory()) * 100;

        List<SysInfoProperty> sysInfo = new ArrayList<>();
        sysInfo.add(new SysInfoProperty("binjr version", getManifestVersion().toString()));
        sysInfo.add(new SysInfoProperty("Java version", System.getProperty("java.version")));
        sysInfo.add(new SysInfoProperty("Java vendor", System.getProperty("java.vendor")));
        sysInfo.add(new SysInfoProperty("Java VM name", System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")"));
        sysInfo.add(new SysInfoProperty("Java home", System.getProperty("java.home")));
        sysInfo.add(new SysInfoProperty("Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")"));
        sysInfo.add(new SysInfoProperty("System Architecture", System.getProperty("os.arch")));
        sysInfo.add(new SysInfoProperty("JVM Heap Max size", String.format("%.0f MB", (double) rt.maxMemory() / 1024 / 1024)));
        sysInfo.add(new SysInfoProperty("JVM Heap Usage", String.format("%.2f%% (%.0f/%.0f MB)", percentUsage, usedMB, (double) rt.totalMemory() / 1024 / 1024)));
        return sysInfo;
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
        }
        else {
            return OsFamily.UNSUPPORTED;
        }
    }

//    public Optional<GithubRelease> getNewRelease() throws IOException, URISyntaxException {
//       try {
//            GithubRelease latestRelease = GithubApi.getInstance().getLatestRelease(GITHUB_OWNER, GITHUB_REPO);
//            if (latestRelease != null) {
//                if (latestRelease.getVersion().compareTo(getManifestVersion()) > 0) {
//                    return Optional.of(latestRelease);
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Error while checking for update", e);
//        }
//        return Optional.empty();
//    }

    public void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable) {
        asyncCheckForUpdate(newReleaseAvailable, null, null);
    }

    public void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Runnable onFailure) {
        asyncCheckForUpdate(newReleaseAvailable, null, onFailure);
    }


    public void asyncCheckForUpdate(Consumer<GithubRelease> newReleaseAvailable, Consumer<Version> upToDate, Runnable onFailure) {
        Task<Optional<GithubRelease>> getLatestTask = new Task<Optional<GithubRelease>>() {
            @Override
            protected Optional<GithubRelease> call() throws Exception {
                logger.trace("getNewRelease running on " + Thread.currentThread().getName());
                GithubRelease latestRelease = GithubApi.getInstance().getLatestRelease(GITHUB_OWNER, GITHUB_REPO);
                if (latestRelease != null) {
                    if (latestRelease.getVersion().compareTo(getManifestVersion()) > 0) {
                        return Optional.of(latestRelease);
                    }
                }
                return Optional.empty();
            }
        };
        getLatestTask.setOnSucceeded(workerStateEvent -> {
            logger.trace("UI update running on " + Thread.currentThread().getName());
            Optional<GithubRelease> latest = getLatestTask.getValue();
            Version current = GlobalPreferences.getInstance().getManifestVersion();
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
        ForkJoinPool.commonPool().submit(getLatestTask);
    }

    private Manifest getManifest() {
        String className = this.getClass().getSimpleName() + ".class";
        String classPath = this.getClass().getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            try {
                return new Manifest(new URL(manifestPath).openStream());
            } catch (IOException e) {
                logger.error("Error extracting manifest from jar", e);
            }
        }
        logger.warn("Could not extract MANIFEST from jar!");
        return null;
    }
}
