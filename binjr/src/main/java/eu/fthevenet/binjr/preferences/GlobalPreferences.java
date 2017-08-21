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

import eu.fthevenet.binjr.dialogs.UserInterfaceThemes;
import javafx.beans.property.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Stores the global user preferences for the application.
 *
 * @author Frederic Thevenet
 */
public class GlobalPreferences {
    public static final String HTTP_WWW_BINJR_EU = "http://www.binjr.eu";
    public static final String HTTP_BINJR_WIKI = "https://github.com/fthevenet/binjr/wiki";
    public static final String HTTP_GITHUB_REPO = "https://github.com/fthevenet/binjr";

    private static final Logger logger = LogManager.getLogger(GlobalPreferences.class);
    private static final String CHART_ANIMATION_ENABLED = "enableChartAnimation";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String DOWN_SAMPLING_ENABLED = "enableDownSampling";
    private static final String BINJR_GLOBAL = "binjr/global";
    private static final String MOST_RECENT_SAVE_FOLDER = "mostRecentSaveFolder";
    private static final String MOST_RECENT_SAVED_WORKSPACE = "mostRecentSavedWorkspace";
    private static final String LOAD_LAST_WORKSPACE_ON_STARTUP = "loadLastWorkspaceOnStartup";
    private static final String CHECK_FOR_UPDATE_ON_START_UP = "checkForUpdateOnStartUp";
    private static final String UI_THEME_NAME = "userInterfaceTheme";
    private static final String RECENT_FILES = "recentFiles";
    private static final int MAX_RECENT_FILES = 20;
    private static final String HORIZONTAL_MARKER_ON = "horizontalMarkerOn";
    private static final String VERTICAL_MARKER_ON = "verticalMarkerOn";
    private static final String SHOW_AREA_OUTLINE = "showAreaOutline";
    private static final String DEFAULT_GRAPH_OPACITY = "defaultGraphOpacity";

    private BooleanProperty loadLastWorkspaceOnStartup;
    private BooleanProperty downSamplingEnabled;
    private IntegerProperty downSamplingThreshold;
    private BooleanProperty chartAnimationEnabled;
    private Preferences prefs;
    private StringProperty mostRecentSaveFolder;
    private Property<Path> mostRecentSavedWorkspace;
    private BooleanProperty checkForUpdateOnStartUp;
    private Property<UserInterfaceThemes> userInterfaceTheme;
    private Deque<String> recentFiles;
    private BooleanProperty horizontalMarkerOn;
    private BooleanProperty verticalMarkerOn;
    private BooleanProperty showAreaOutline;
    private DoubleProperty defaultGraphOpacity;
    private BooleanProperty shiftPressed = new SimpleBooleanProperty(false);
    private BooleanProperty ctrlPressed = new SimpleBooleanProperty(false);

    public Boolean isShiftPressed() {
        return shiftPressed.get();
    }

    public void setShiftPressed(Boolean shiftPressed) {
        this.shiftPressed.set(shiftPressed);
    }

    public BooleanProperty shiftPressedProperty() {
        return shiftPressed;
    }

    public Boolean isCtrlPressed() {
        return ctrlPressed.getValue();
    }

    public void setCtrlPressed(Boolean ctrlPressed) {
        this.ctrlPressed.set(ctrlPressed);
    }

    public BooleanProperty ctrlPressedProperty() {
        return ctrlPressed;
    }

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
        chartAnimationEnabled = new SimpleBooleanProperty(prefs.getBoolean(CHART_ANIMATION_ENABLED, true));
        chartAnimationEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHART_ANIMATION_ENABLED, newValue));
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
        horizontalMarkerOn = new SimpleBooleanProperty(prefs.getBoolean(HORIZONTAL_MARKER_ON, false));
        horizontalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(HORIZONTAL_MARKER_ON, newValue));
        verticalMarkerOn = new SimpleBooleanProperty(prefs.getBoolean(VERTICAL_MARKER_ON, true));
        verticalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(VERTICAL_MARKER_ON, newValue));
        showAreaOutline = new SimpleBooleanProperty(prefs.getBoolean(SHOW_AREA_OUTLINE, true));
        showAreaOutline.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SHOW_AREA_OUTLINE, newValue));
        defaultGraphOpacity = new SimpleDoubleProperty(prefs.getDouble(DEFAULT_GRAPH_OPACITY, 0.8d));
        defaultGraphOpacity.addListener((observable, oldValue, newValue) -> prefs.putDouble(DEFAULT_GRAPH_OPACITY, newValue.doubleValue()));
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

    public boolean isShowAreaOutline() {
        return showAreaOutline.getValue();
    }

    public BooleanProperty showAreaOutlineProperty() {
        return showAreaOutline;
    }

    public void setShowAreaOutline(boolean showAreaOutline) {
        this.showAreaOutline.setValue(showAreaOutline);
    }

    public double getDefaultGraphOpacity() {
        return defaultGraphOpacity.get();
    }

    public DoubleProperty defaultGraphOpacityProperty() {
        return defaultGraphOpacity;
    }

    public void setDefaultGraphOpacity(double defaultGraphOpacity) {
        this.defaultGraphOpacity.set(defaultGraphOpacity);
    }


}