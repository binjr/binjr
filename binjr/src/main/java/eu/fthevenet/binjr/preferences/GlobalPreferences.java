package eu.fthevenet.binjr.preferences;

import com.sun.javafx.collections.ObservableSetWrapper;
import javafx.beans.property.*;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;

/**
 * Stores the global user preferences for the application.
 *
 * @author Frederic Thevenet
 */
public class GlobalPreferences {
    public static final String HTTP_WWW_BINJR_EU = "http://www.binjr.eu";
    public static final String HTTP_LATEST_RELEASE = "https://github.com/fthevenet/binjr/releases/latest";


    private static final Logger logger = LogManager.getLogger(GlobalPreferences.class);
    private static final String CHART_ANIMATION_ENABLED = "chartAnimationEnabled";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String SAMPLE_SYMBOLS_VISIBLE = "sampleSymbolsVisible";
    private static final String DOWN_SAMPLING_ENABLED = "downSamplingEnabled";
    private static final String BINJR_GLOBAL = "binjr/global";
    private static final String USE_SOURCE_COLORS = "useSourceColors";
    private static final String MOST_RECENT_SAVE_FOLDER = "mostRecentSaveFolder";
    private static final String MOST_RECENT_SAVED_WORKSPACE = "mostRecentSavedWorkspace";
    private static final String LOAD_LAST_WORKSPACE_ON_STARTUP = "loadLastWorkspaceOnStartup";
    private static final String RECENT_FILES = "recentFiles";
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
    private ObservableSet<String> recentFiles;

    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }

    private GlobalPreferences() {
        prefs = Preferences.userRoot().node(BINJR_GLOBAL);
        mostRecentSaveFolder = new SimpleStringProperty(prefs.get(MOST_RECENT_SAVE_FOLDER, System.getProperty("user.home")));
        mostRecentSaveFolder.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVE_FOLDER, newValue));
        downSamplingEnabled = new SimpleBooleanProperty(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold = new SimpleIntegerProperty(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 5000));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        sampleSymbolsVisible = new SimpleBooleanProperty(prefs.getBoolean(SAMPLE_SYMBOLS_VISIBLE, false));
        sampleSymbolsVisible.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SAMPLE_SYMBOLS_VISIBLE, newValue));
        chartAnimationEnabled = new SimpleBooleanProperty(prefs.getBoolean(CHART_ANIMATION_ENABLED, false));
        chartAnimationEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHART_ANIMATION_ENABLED, newValue));
        useSourceColors = new SimpleBooleanProperty(prefs.getBoolean(USE_SOURCE_COLORS, true));
        useSourceColors.addListener((observable, oldValue, newValue) -> prefs.putBoolean(USE_SOURCE_COLORS, newValue));
        mostRecentSavedWorkspace = new SimpleObjectProperty<>(Paths.get(prefs.get(MOST_RECENT_SAVED_WORKSPACE, "Untitled")));
        mostRecentSavedWorkspace.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVED_WORKSPACE, newValue.toString()));
        loadLastWorkspaceOnStartup = new SimpleBooleanProperty(prefs.getBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, true));
        loadLastWorkspaceOnStartup.addListener((observable, oldValue, newValue) -> prefs.putBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, newValue));

        Set<String> lruRecentFiles = new LinkedHashSet<>();
        String[] recentPath = prefs.get(RECENT_FILES, "").split("|");
        lruRecentFiles.addAll(Arrays.asList(recentPath));
        recentFiles = new ObservableSetWrapper<>(lruRecentFiles);
        recentFiles.addListener((SetChangeListener<String>) change -> {
            // prefs.put(RECENT_FILES, change.getMap().values().stream().collect(Collectors.joining("|")));
        });

        this.manifest = getManifest();
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
    public void setChartAnimationEnabled(Boolean chartAnimationEnabled) {
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
    public void setSampleSymbolsVisible(Boolean sampleSymbolsVisible) {
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
    public void setDownSamplingEnabled(Boolean downSamplingEnabled) {
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

    public Boolean isUseSourceColors() {
        return useSourceColors.getValue();
    }

    public BooleanProperty useSourceColorsProperty() {
        return useSourceColors;
    }

    public void setUseSourceColors(Boolean useSourceColors) {
        this.useSourceColors.setValue(useSourceColors);
    }

    public String getMostRecentSaveFolder() {
        return mostRecentSaveFolder.getValue();
    }

    public StringProperty mostRecentSaveFolderProperty() {
        return mostRecentSaveFolder;
    }

    public void setMostRecentSaveFolder(String mostRecentSaveFolder) {
        this.mostRecentSaveFolder.setValue(mostRecentSaveFolder);
    }

    public Path getMostRecentSavedWorkspace() {
        return mostRecentSavedWorkspace.getValue();
    }

    public Property<Path> mostRecentSavedWorkspaceProperty() {
        return mostRecentSavedWorkspace;
    }

    public void setMostRecentSavedWorkspace(Path mostRecentSavedWorkspace) {
        this.mostRecentSavedWorkspace.setValue(mostRecentSavedWorkspace);
    }

    public boolean isLoadLastWorkspaceOnStartup() {
        return loadLastWorkspaceOnStartup.get();
    }

    public BooleanProperty loadLastWorkspaceOnStartupProperty() {
        return loadLastWorkspaceOnStartup;
    }

    public boolean putToRecentFiles(String value) {
        //Explicitly remove the element if present to enforce strict FIFO order
        if (recentFiles.contains(value)) {
            recentFiles.remove(value);
        }
        return recentFiles.add(value);
    }

    public Collection<String> getRecentFiles() {
        //TODO reverse order of set
        return recentFiles;
    }


    public void setLoadLastWorkspaceOnStartup(boolean loadLastWorkspaceOnStartup) {
        this.loadLastWorkspaceOnStartup.set(loadLastWorkspaceOnStartup);
    }

    public String getManifestVersion() {
        if (manifest != null) {
            String[] keys = new String[]{"Specification-Version", "Implementation-Version"};
            for (String key : keys) {
                String value = manifest.getMainAttributes().getValue(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return "unknown";
    }

    public List<SysInfoProperty> getSysInfoProperties() {
        Runtime rt = Runtime.getRuntime();
        double usedMB = ((double) rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        double percentUsage = (((double) rt.totalMemory() - rt.freeMemory()) / rt.totalMemory()) * 100;

        List<SysInfoProperty> sysInfo = new ArrayList<>();
        sysInfo.add(new SysInfoProperty("binjr version", getManifestVersion()));
        sysInfo.add(new SysInfoProperty("Java version", System.getProperty("java.version")));
        sysInfo.add(new SysInfoProperty("Java vendor",System.getProperty("java.vendor") ));
        sysInfo.add(new SysInfoProperty("Java VM name",  System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")"));
        sysInfo.add(new SysInfoProperty("Java home", System.getProperty("java.home")));
        sysInfo.add(new SysInfoProperty("Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")"));
        sysInfo.add(new SysInfoProperty("System Architecture", System.getProperty("os.arch")));
        sysInfo.add(new SysInfoProperty("JVM Heap Max size",  String.format("%.0f MB", (double) rt.maxMemory() / 1024 / 1024)));
        sysInfo.add(new SysInfoProperty("JVM Heap Usage", String.format("%.2f%% (%.0f/%.0f MB)", percentUsage, usedMB, (double) rt.totalMemory() / 1024 / 1024)));
        return sysInfo;
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
