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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Stores the global user prefs for the application.
 *
 * @author Frederic Thevenet
 */
public class GlobalPreferences {

    private static final Logger logger = LogManager.getLogger(GlobalPreferences.class);
    private static final String BINJR_GLOBAL = "binjr/global";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String DOWN_SAMPLING_ENABLED = "enableDownSampling";
    private static final String MOST_RECENT_SAVE_FOLDER = "mostRecentSaveFolder";
    private static final String MOST_RECENT_SAVED_WORKSPACE = "mostRecentSavedWorkspace";
    private static final String LOAD_LAST_WORKSPACE_ON_STARTUP = "loadLastWorkspaceOnStartup";
    private static final String CHECK_FOR_UPDATE_ON_START_UP = "checkForUpdateOnStartUp";
    private static final String UI_THEME_NAME = "userInterfaceTheme";
    private static final String RECENT_FILES = "recentFiles";
    private static final String HORIZONTAL_MARKER_ON = "horizontalMarkerOn";
    private static final String VERTICAL_MARKER_ON = "verticalMarkerOn";
    private static final String SHOW_AREA_OUTLINE = "showAreaOutline";
    private static final String DEFAULT_GRAPH_OPACITY = "defaultGraphOpacity";
    private static final int MAX_RECENT_FILES = 20;
    private static final String PLUGINS_LOCATION = "pluginsLocation";
    private static final String DEFAULT_PLUGINS_LOCATION = ".";

    private final BooleanProperty loadLastWorkspaceOnStartup = new SimpleBooleanProperty();
    private final BooleanProperty downSamplingEnabled = new SimpleBooleanProperty();
    private final IntegerProperty downSamplingThreshold = new SimpleIntegerProperty();
    private final StringProperty mostRecentSaveFolder = new SimpleStringProperty();
    private final Property<Path> mostRecentSavedWorkspace = new SimpleObjectProperty<>();
    private final BooleanProperty checkForUpdateOnStartUp = new SimpleBooleanProperty();
    private final Property<UserInterfaceThemes> userInterfaceTheme = new SimpleObjectProperty<>();
    ;
    private final BooleanProperty horizontalMarkerOn = new SimpleBooleanProperty();
    private final BooleanProperty verticalMarkerOn = new SimpleBooleanProperty();
    private final BooleanProperty showAreaOutline = new SimpleBooleanProperty();
    private final DoubleProperty defaultGraphOpacity = new SimpleDoubleProperty();
    private final BooleanProperty shiftPressed = new SimpleBooleanProperty(false);
    private final BooleanProperty ctrlPressed = new SimpleBooleanProperty(false);
    private final Property<Path> pluginsLocation = new SimpleObjectProperty<>();
    private final Preferences prefs;
    private Deque<String> recentFiles;




    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }

    private GlobalPreferences() {
        this.prefs = Preferences.userRoot().node(BINJR_GLOBAL);
        this.load();
        mostRecentSaveFolder.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                prefs.put(MOST_RECENT_SAVE_FOLDER, newValue);
            }
        });
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        mostRecentSavedWorkspace.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVED_WORKSPACE, newValue.toString()));
        loadLastWorkspaceOnStartup.addListener((observable, oldValue, newValue) -> prefs.putBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, newValue));
        userInterfaceTheme.addListener((observable, oldValue, newValue) -> prefs.put(UI_THEME_NAME, newValue.name()));
        checkForUpdateOnStartUp.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHECK_FOR_UPDATE_ON_START_UP, newValue));
        horizontalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(HORIZONTAL_MARKER_ON, newValue));
        verticalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(VERTICAL_MARKER_ON, newValue));
        showAreaOutline.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SHOW_AREA_OUTLINE, newValue));
        defaultGraphOpacity.addListener((observable, oldValue, newValue) -> prefs.putDouble(DEFAULT_GRAPH_OPACITY, newValue.doubleValue()));
        pluginsLocation.addListener((observable, oldValue, newValue) -> prefs.put(PLUGINS_LOCATION, newValue.toString()));
    }

    private void load() {
        recentFiles = new ArrayDeque<>(Arrays.stream(prefs.get(RECENT_FILES, "").split("\\|")).filter(s -> s != null && s.trim().length() > 0).collect(Collectors.toList()));
        showAreaOutline.setValue(prefs.getBoolean(SHOW_AREA_OUTLINE, false));
        defaultGraphOpacity.setValue(prefs.getDouble(DEFAULT_GRAPH_OPACITY, 0.8d));
        verticalMarkerOn.setValue(prefs.getBoolean(VERTICAL_MARKER_ON, true));
        horizontalMarkerOn.setValue(prefs.getBoolean(HORIZONTAL_MARKER_ON, false));
        checkForUpdateOnStartUp.setValue(prefs.getBoolean(CHECK_FOR_UPDATE_ON_START_UP, true));
        userInterfaceTheme.setValue(UserInterfaceThemes.valueOf(prefs.get(UI_THEME_NAME, UserInterfaceThemes.MODERN.name())));
        loadLastWorkspaceOnStartup.setValue(prefs.getBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, true));
        mostRecentSavedWorkspace.setValue(Paths.get(prefs.get(MOST_RECENT_SAVED_WORKSPACE, "Untitled")));
        downSamplingThreshold.setValue(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 5000));
        downSamplingEnabled.setValue(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
        mostRecentSaveFolder.setValue(prefs.get(MOST_RECENT_SAVE_FOLDER, System.getProperty("user.home")));
        pluginsLocation.setValue(Paths.get(prefs.get(PLUGINS_LOCATION, DEFAULT_PLUGINS_LOCATION)));
    }

    /**
     * Reset all prefs to their default values
     *
     * @throws BackingStoreException if an error occurred while trying to clear the prefs backing store.
     */
    public void reset() throws BackingStoreException {
        prefs.clear();
        this.load();
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

    public Path getPluginsLocation() {
        return pluginsLocation.getValue();
    }

    public void setPluginsLocation(Path pluginsLocation) {
        this.pluginsLocation.setValue(pluginsLocation);
    }

    public Property<Path> pluginsLocationProperty() {
        return pluginsLocation;
    }



}