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

import eu.binjr.core.dialogs.UserInterfaceThemes;
import javafx.beans.property.*;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    private static final String UI_THEME_NAME = "userInterfaceTheme_v2";
    private static final String RECENT_FILES = "recentFiles";
    private static final String HORIZONTAL_MARKER_ON = "horizontalMarkerOn";
    private static final String VERTICAL_MARKER_ON = "verticalMarkerOn";
    private static final String SHOW_AREA_OUTLINE = "showAreaOutline";
    private static final String DEFAULT_GRAPH_OPACITY = "defaultGraphOpacity";
    private static final int MAX_RECENT_FILES = 20;
    private static final String PLUGINS_LOCATION = "pluginsLocation";
    private static final String DEFAULT_PLUGINS_LOCATION = System.getProperty("user.home");
    private static final String NOTIFICATION_POPUP_DURATION = "notificationPopupDuration";
    private static final String LOAD_PLUGINS_FROM_EXTERNAL_LOCATION = "loadPluginsFromExternalLocation";
    private static final String CONSOLE_MAX_LINE_CAPACITY = "consoleMaxLineCapacity";
    private static final Duration DEFAULT_NOTIFICATION_POPUP_DURATION = Duration.seconds(10);

    private final BooleanProperty loadLastWorkspaceOnStartup = new SimpleBooleanProperty();
    private final BooleanProperty downSamplingEnabled = new SimpleBooleanProperty();
    private final IntegerProperty downSamplingThreshold = new SimpleIntegerProperty();
    private final Property<Path> mostRecentSaveFolder = new SimpleObjectProperty<>();
    private final Property<Path> mostRecentSavedWorkspace = new SimpleObjectProperty<>();
    private final BooleanProperty checkForUpdateOnStartUp = new SimpleBooleanProperty();
    private final Property<UserInterfaceThemes> userInterfaceTheme = new SimpleObjectProperty<>();
    private final BooleanProperty horizontalMarkerOn = new SimpleBooleanProperty();
    private final BooleanProperty verticalMarkerOn = new SimpleBooleanProperty();
    private final BooleanProperty showAreaOutline = new SimpleBooleanProperty();
    private final DoubleProperty defaultGraphOpacity = new SimpleDoubleProperty();
    private final BooleanProperty shiftPressed = new SimpleBooleanProperty(false);
    private final BooleanProperty ctrlPressed = new SimpleBooleanProperty(false);
    private final Property<Path> pluginsLocation = new SimpleObjectProperty<>();
    private final Property<Duration> notificationPopupDuration = new SimpleObjectProperty<>();
    private final BooleanProperty loadPluginsFromExternalLocation = new SimpleBooleanProperty();
    private final IntegerProperty consoleMaxLineCapacity = new SimpleIntegerProperty();

    private final Preferences prefs;
    private Deque<String> recentFiles;

    private GlobalPreferences() {
        this.prefs = Preferences.userRoot().node(BINJR_GLOBAL);
        this.load();
        mostRecentSaveFolder.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVE_FOLDER, newValue.toString()));
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        consoleMaxLineCapacity.addListener((observable, oldValue, newValue) -> prefs.putInt(CONSOLE_MAX_LINE_CAPACITY, newValue.intValue()));
        mostRecentSavedWorkspace.addListener((observable, oldValue, newValue) -> prefs.put(MOST_RECENT_SAVED_WORKSPACE, newValue.toString()));
        loadLastWorkspaceOnStartup.addListener((observable, oldValue, newValue) -> prefs.putBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, newValue));
        userInterfaceTheme.addListener((observable, oldValue, newValue) -> prefs.put(UI_THEME_NAME, newValue.name()));
        horizontalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(HORIZONTAL_MARKER_ON, newValue));
        verticalMarkerOn.addListener((observable, oldValue, newValue) -> prefs.putBoolean(VERTICAL_MARKER_ON, newValue));
        showAreaOutline.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SHOW_AREA_OUTLINE, newValue));
        defaultGraphOpacity.addListener((observable, oldValue, newValue) -> prefs.putDouble(DEFAULT_GRAPH_OPACITY, newValue.doubleValue()));
        pluginsLocation.addListener((observable, oldValue, newValue) -> prefs.put(PLUGINS_LOCATION, newValue.toString()));
        notificationPopupDuration.addListener((observable, oldValue, newValue) -> prefs.putDouble(NOTIFICATION_POPUP_DURATION, newValue.toSeconds()));
        loadPluginsFromExternalLocation.addListener((observable, oldValue, newValue) -> prefs.putBoolean(LOAD_PLUGINS_FROM_EXTERNAL_LOCATION, newValue));
    }

    /**
     * Returns the singleton instance of {@link GlobalPreferences}
     *
     * @return the singleton instance of {@link GlobalPreferences}
     */
    public static GlobalPreferences getInstance() {
        return GlobalPreferencesHolder.instance;
    }

    private void load() {
        try {
            recentFiles = new ArrayDeque<>(Arrays.stream(prefs.get(RECENT_FILES, "").split("\\|")).filter(s -> s != null && s.trim().length() > 0).collect(Collectors.toList()));
            showAreaOutline.setValue(prefs.getBoolean(SHOW_AREA_OUTLINE, false));
            defaultGraphOpacity.setValue(prefs.getDouble(DEFAULT_GRAPH_OPACITY, 0.8d));
            verticalMarkerOn.setValue(prefs.getBoolean(VERTICAL_MARKER_ON, true));
            horizontalMarkerOn.setValue(prefs.getBoolean(HORIZONTAL_MARKER_ON, false));
            checkForUpdateOnStartUp.setValue(prefs.getBoolean(CHECK_FOR_UPDATE_ON_START_UP, true));
            userInterfaceTheme.setValue(UserInterfaceThemes.valueOf(prefs.get(UI_THEME_NAME, ""), UserInterfaceThemes.LIGHT));
            loadLastWorkspaceOnStartup.setValue(prefs.getBoolean(LOAD_LAST_WORKSPACE_ON_STARTUP, true));
            mostRecentSavedWorkspace.setValue(Paths.get(prefs.get(MOST_RECENT_SAVED_WORKSPACE, "Untitled")));
            downSamplingThreshold.setValue(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 5000));
            consoleMaxLineCapacity.setValue(prefs.getInt(CONSOLE_MAX_LINE_CAPACITY, 2000));
            downSamplingEnabled.setValue(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
            mostRecentSaveFolder.setValue(Paths.get(prefs.get(MOST_RECENT_SAVE_FOLDER, System.getProperty("user.home"))));
            notificationPopupDuration.setValue(Duration.seconds(prefs.getDouble(NOTIFICATION_POPUP_DURATION, DEFAULT_NOTIFICATION_POPUP_DURATION.toSeconds())));
            pluginsLocation.setValue(Paths.get(prefs.get(PLUGINS_LOCATION, DEFAULT_PLUGINS_LOCATION)));
            loadPluginsFromExternalLocation.setValue(prefs.getBoolean(LOAD_PLUGINS_FROM_EXTERNAL_LOCATION, false));
        } catch (Exception e) {
            logger.error("Error while loading application preferences", e);
        }
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
     * Returns true if series down-sampling is enabled, false otherwise.
     *
     * @return true if series down-sampling is enabled, false otherwise.
     */
    public Boolean getDownSamplingEnabled() {
        return downSamplingEnabled.getValue();
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
     * Returns the down-sampling property
     *
     * @return the down-sampling property
     */
    public BooleanProperty downSamplingEnabledProperty() {
        return downSamplingEnabled;
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
     * Sets the series down-sampling threshold value
     *
     * @param downSamplingThreshold the series down-sampling threshold value
     */
    public void setDownSamplingThreshold(int downSamplingThreshold) {
        this.downSamplingThreshold.setValue(downSamplingThreshold);
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
     * Gets the path of the folder of the most recently saved item
     *
     * @return the path of the folder of the most recently saved item
     */
    public Path getMostRecentSaveFolder() {
        Path validatedPath = Paths.get(System.getProperty("user.home"));
        try {
            if (mostRecentSaveFolder.getValue() != null
                    && mostRecentSaveFolder.getValue().toRealPath(LinkOption.NOFOLLOW_LINKS) != null
                    && mostRecentSaveFolder.getValue().toFile().isDirectory()) {
                validatedPath = mostRecentSaveFolder.getValue();
            } else {
                logger.error("MostRecentSaveFolder property does not point to a valid directory");
            }
        } catch (Exception e) {
            logger.error("MostRecentSaveFolder property does not point to a valid directory", e);
        }
        return validatedPath;
    }

    /**
     * Sets the path of the folder of the most recently saved item
     *
     * @param mostRecentSaveFolder the path of the folder of the most recently saved item
     */
    public void setMostRecentSaveFolder(Path mostRecentSaveFolder) {
        if (mostRecentSaveFolder == null) {
            throw new IllegalArgumentException("mostRecentSaveFolder parameter cannot be null");
        }
        if (!mostRecentSaveFolder.toFile().isDirectory()) {
            mostRecentSaveFolder = mostRecentSaveFolder.getParent();
            if (mostRecentSaveFolder == null) {
                throw new IllegalArgumentException("mostRecentSaveFolder is not a directory");
            }
        }
        this.mostRecentSaveFolder.setValue(mostRecentSaveFolder);
    }

    /**
     * The mostRecentSaveFolder property
     *
     * @return the mostRecentSaveFolder property
     */
    public Property<Path> mostRecentSaveFolderProperty() {
        return mostRecentSaveFolder;
    }

    /**
     * Gets the path from the most recently saved workspace
     *
     * @return the path from the most recently saved workspace
     */
    public Optional<Path> getMostRecentSavedWorkspace() {
        return Paths.get("Untitled").equals(mostRecentSavedWorkspace.getValue()) ? Optional.empty() : Optional.of(mostRecentSavedWorkspace.getValue());
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
     * The mostRecentSavedWorkspace property
     *
     * @return the  mostRecentSavedWorkspace property
     */
    public Property<Path> mostRecentSavedWorkspaceProperty() {
        return mostRecentSavedWorkspace;
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
     * Sets to true if  the most recently saved workspace should be re-opned on startup, false otherwise
     *
     * @param loadLastWorkspaceOnStartup true if  the most recently saved workspace should be re-opned on startup, false otherwise
     */
    public void setLoadLastWorkspaceOnStartup(boolean loadLastWorkspaceOnStartup) {
        this.loadLastWorkspaceOnStartup.set(loadLastWorkspaceOnStartup);
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
     * Gets the current UI theme
     *
     * @return the current UI theme
     */
    public UserInterfaceThemes getUserInterfaceTheme() {
        return userInterfaceTheme.getValue();
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
     * The UI theme property
     *
     * @return the UI theme property
     */
    public Property<UserInterfaceThemes> userInterfaceThemeProperty() {
        return userInterfaceTheme;
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

    /**
     * Returns true is auto check for update is on, false otherwise.
     *
     * @return true is auto check for update is on, false otherwise.
     */
    public boolean isCheckForUpdateOnStartUp() {
        return checkForUpdateOnStartUp.get();
    }

    /**
     * Set to true to check for update on startup.
     *
     * @param checkForUpdateOnStartUp true to check for update on startup.
     */
    public void setCheckForUpdateOnStartUp(boolean checkForUpdateOnStartUp) {
        this.checkForUpdateOnStartUp.set(checkForUpdateOnStartUp);
    }

    /**
     * The checkForUpdateOnStartUp property.
     *
     * @return the checkForUpdateOnStartUp property.
     */
    public BooleanProperty checkForUpdateOnStartUpProperty() {
        return checkForUpdateOnStartUp;
    }

    /**
     * Returns true is the horizontal marker is enabled, false otherwise.
     *
     * @return true is the horizontal marker is enabled, false otherwise.
     */
    public boolean getHorizontalMarkerOn() {
        return horizontalMarkerOn.get();
    }

    /**
     * Set to true to enable the horizontal marker, to false to disable it.
     *
     * @param horizontalMarkerOn true to enable the horizontal marker, to false to disable it.
     */
    public void setHorizontalMarkerOn(boolean horizontalMarkerOn) {
        this.horizontalMarkerOn.set(horizontalMarkerOn);
    }

    /**
     * The horizontalMarkerOn property
     *
     * @return the horizontalMarkerOn property
     */
    public BooleanProperty horizontalMarkerOnProperty() {
        return horizontalMarkerOn;
    }

    /**
     * Returns true is the vertical marker is enabled, false otherwise.
     *
     * @return true is the vertical marker is enabled, false otherwise.
     */
    public boolean getVerticalMarkerOn() {
        return verticalMarkerOn.get();
    }

    /**
     * Set to true to enable the vertical marker, to false to disable it.
     *
     * @param verticalMarkerOn true to enable the vertical marker, to false to disable it.
     */
    public void setVerticalMarkerOn(boolean verticalMarkerOn) {
        this.verticalMarkerOn.set(verticalMarkerOn);
    }

    /**
     * The verticalMarkerOn property
     *
     * @return the verticalMarkerOn property
     */
    public BooleanProperty verticalMarkerOnProperty() {
        return verticalMarkerOn;
    }

    /**
     * Returns true is the outline of area charts is displayed, false otherwise.
     *
     * @return true is the outline of area charts is displayed, false otherwise.
     */
    public boolean isShowAreaOutline() {
        return showAreaOutline.getValue();
    }

    /**
     * Set to true to display the outline on area charts.
     *
     * @param showAreaOutline true to display the outline on area charts.
     */
    public void setShowAreaOutline(boolean showAreaOutline) {
        this.showAreaOutline.setValue(showAreaOutline);
    }

    /**
     * The showAreaOutline property
     *
     * @return the showAreaOutline property
     */
    public BooleanProperty showAreaOutlineProperty() {
        return showAreaOutline;
    }

    /**
     * Returns the default opacity for new area charts.
     *
     * @return the default opacity for new area charts.
     */
    public double getDefaultGraphOpacity() {
        return defaultGraphOpacity.get();
    }

    /**
     * Sets the default opacity for new area charts.
     *
     * @param defaultGraphOpacity the default opacity for new area charts.
     */
    public void setDefaultGraphOpacity(double defaultGraphOpacity) {
        this.defaultGraphOpacity.set(defaultGraphOpacity);
    }

    /**
     * The defaultGraphOpacity property.
     *
     * @return the defaultGraphOpacity property.
     */
    public DoubleProperty defaultGraphOpacityProperty() {
        return defaultGraphOpacity;
    }

    /**
     * Returns true is the shift key is pressed, false otherwise.
     *
     * @return true is the shift key is pressed, false otherwise.
     */
    public Boolean isShiftPressed() {
        return shiftPressed.get();
    }

    /**
     * Sets the shiftPressed property's value.
     *
     * @param shiftPressed the shiftPressed property's value.
     */
    public void setShiftPressed(Boolean shiftPressed) {
        this.shiftPressed.set(shiftPressed);
    }

    /**
     * The shiftPressed property
     *
     * @return the shiftPressed property
     */
    public BooleanProperty shiftPressedProperty() {
        return shiftPressed;
    }

    /**
     * Returns true is the control key is pressed, false otherwise.
     *
     * @return true is the control key is pressed, false otherwise.
     */
    public Boolean isCtrlPressed() {
        return ctrlPressed.getValue();
    }

    /**
     * The ctrlPressed property
     *
     * @param ctrlPressed the ctrlPressed property
     */
    public void setCtrlPressed(Boolean ctrlPressed) {
        this.ctrlPressed.set(ctrlPressed);
    }

    /**
     * The ctrlPressed property
     *
     * @return the ctrlPressed property
     */
    public BooleanProperty ctrlPressedProperty() {
        return ctrlPressed;
    }

    /**
     * Returns the location for plugins.
     *
     * @return the location for plugins.
     */
    public Path getPluginsLocation() {
        return pluginsLocation.getValue();
    }

    /**
     * Sets  the location for plugins.
     *
     * @param pluginsLocation the location for plugins.
     */
    public void setPluginsLocation(Path pluginsLocation) {
        this.pluginsLocation.setValue(pluginsLocation);
    }

    /**
     * The pluginsLocation property.
     *
     * @return the pluginsLocation property.
     */
    public Property<Path> pluginsLocationProperty() {
        return pluginsLocation;
    }

    /**
     * Returns the duration to display notification popups.
     *
     * @return the duration to display notification popups.
     */
    public Duration getNotificationPopupDuration() {
        return notificationPopupDuration.getValue();
    }

    /**
     * Sets the duration to display notification popups.
     *
     * @param notificationPopupDuration the duration to display notification popups.
     */
    public void setNotificationPopupDuration(Duration notificationPopupDuration) {
        this.notificationPopupDuration.setValue(notificationPopupDuration);
    }

    /**
     * The notificationPopupDuration property.
     *
     * @return the notificationPopupDuration property.
     */
    public Property<Duration> notificationPopupDurationProperty() {
        return notificationPopupDuration;
    }

    /**
     * Returns true is plugins should be loaded from external location, false otherwise.
     *
     * @return true is plugins should be loaded from external location, false otherwise.
     */
    public boolean isLoadPluginsFromExternalLocation() {
        return loadPluginsFromExternalLocation.get();
    }

    /**
     * Set to true if plugins should be loaded from external location, false otherwise.
     *
     * @param loadPluginsFromExternalLocation true if plugins should be loaded from external location, false otherwise.
     */
    public void setLoadPluginsFromExternalLocation(boolean loadPluginsFromExternalLocation) {
        this.loadPluginsFromExternalLocation.set(loadPluginsFromExternalLocation);
    }

    /**
     * The loadPluginsFromExternalLocation property.
     *
     * @return the loadPluginsFromExternalLocation property.
     */
    public BooleanProperty loadPluginsFromExternalLocationProperty() {
        return loadPluginsFromExternalLocation;
    }

    /**
     * Returns the maximum number of lines to display in the output console.
     *
     * @return the maximum number of lines to display in the output console.
     */
    public int getConsoleMaxLineCapacity() {
        return consoleMaxLineCapacity.get();
    }

    /**
     * Sets the maximum number of lines to display in the output console.
     *
     * @param consoleMaxLineCapacity the maximum number of lines to display in the output console.
     */
    public void setConsoleMaxLineCapacity(int consoleMaxLineCapacity) {
        this.consoleMaxLineCapacity.set(consoleMaxLineCapacity);
    }

    /**
     * The consoleMaxLineCapacity property.
     *
     * @return the consoleMaxLineCapacity property.
     */
    public IntegerProperty consoleMaxLineCapacityProperty() {
        return consoleMaxLineCapacity;
    }

    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }
}