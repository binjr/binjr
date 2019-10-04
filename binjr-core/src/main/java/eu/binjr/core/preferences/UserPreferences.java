/*
 *    Copyright 2019 Frederic Thevenet
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

import com.google.gson.Gson;
import eu.binjr.common.logging.Log4j2Level;
import eu.binjr.common.preferences.Preference;
import eu.binjr.common.preferences.PreferenceFactory;
import eu.binjr.core.appearance.BuiltInUserInterfaceThemes;
import eu.binjr.core.appearance.UserInterfaceThemes;
import eu.binjr.core.data.async.ThreadPoolPolicy;
import javafx.geometry.Rectangle2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Manage all user preferences
 *
 * @author Frederic Thevenet
 */
public class UserPreferences extends PreferenceFactory {
    private static final Logger logger = LogManager.getLogger(UserPreferences.class);
    private static final Gson gson = new Gson();
    public static final String BINJR_GLOBAL = "binjr/global";

    /**
     * True if series down-sampling is enabled, false otherwise.
     */
    public final Preference<Boolean> downSamplingEnabled = booleanPreference("downSamplingEnabled", true);

    /**
     * The series down-sampling threshold value.
     */
    public final Preference<Number> downSamplingThreshold = integerPreference("downSamplingThreshold", 1500);

    /**
     * The username used for authenticated access to the GitHub API.
     */
    public final Preference<String> githubUserName = stringPreference("githubUserName", "");

    /**
     * The authentication token  used for authenticated access to the GitHub API.
     */
    public final Preference<String> githubAuthToken = stringPreference("githubAuthToken", "");

    /**
     * The User Interface theme applied to the application.
     */
    public final Preference<UserInterfaceThemes> userInterfaceTheme =
            objectPreference(UserInterfaceThemes.class,
                    "userInterfaceTheme_v2",
                    BuiltInUserInterfaceThemes.LIGHT,
                    UserInterfaceThemes::name,
                    s -> UserInterfaceThemes.valueOf(s, BuiltInUserInterfaceThemes.LIGHT));

    /**
     * True if the last open workspace should be reload next time the app if started, false otherwise.
     */
    public final Preference<Boolean> loadLastWorkspaceOnStartup = booleanPreference("loadLastWorkspaceOnStartup", true);

    /**
     * True to check if a new release is available each time the application starts, false otherwise.
     */
    public final Preference<Boolean> checkForUpdateOnStartUp = booleanPreference("checkForUpdateOnStartUp", true);

    /**
     * True if the horizontal marker should be displayed on chart views, false otherwise.
     */
    public final Preference<Boolean> horizontalMarkerOn = booleanPreference("horizontalMarkerOn", false);

    /**
     * True if the vertical marker should be displayed on chart views, false otherwise.
     */
    public final Preference<Boolean> verticalMarkerOn = booleanPreference("verticalMarkerOn", true);

    /**
     * True if series on area charts should display a brighter coloured outline, false otherwise.
     */
    public final Preference<Boolean> showAreaOutline = booleanPreference("verticalMarkerOn", true);

    /**
     * The default opacity value to apply to series on aera charts.
     */
    public final Preference<Number> defaultGraphOpacity = doublePreference("defaultGraphOpacity", 0.45d);

    /**
     * True is the shift key is pressed, false otherwise.
     */
    public final Preference<Boolean> shiftPressed = booleanPreference("shiftPressed", false);

    /**
     * True is the control key is pressed, false otherwise.
     */
    public final Preference<Boolean> ctrlPressed = booleanPreference("ctrlPressed", false);

    /**
     * The amount of time notification should over before being automatically dismissed.
     */
    public final Preference<NotificationDurationChoices> notificationPopupDuration =
            enumPreference(NotificationDurationChoices.class,
                    "notificationPopupDuration",
                    NotificationDurationChoices.TEN_SECONDS);
    /**
     * The location to load plugins from in addition to those on the classpath.
     */
    public final Preference<Path> pluginsLocation =
            pathPreference("pluginsLocation", Path.of(System.getProperty("user.home")));

    /**
     * True if plugins from the location defined by "pluginsLocation" should be loaded in addition to those
     * on the classpath.
     */
    public final Preference<Boolean> loadPluginsFromExternalLocation =
            booleanPreference("loadPluginsFromExternalLocation", false);

    /**
     * The line buffer depth for the debug console.
     */
    public final Preference<Number> consoleMaxLineCapacity = integerPreference("consoleMaxLineCapacity", 2000);

    /**
     * True if the vertical maker should span over all stacked charts on a worksheet, false if it should only show
     * over the focused chart.
     */
    public final Preference<Boolean> fullHeightCrosshairMarker = booleanPreference("fullHeightCrosshairMarker", true);

    /**
     * The maximum number of thread to allocate to the asynchronous tasks thread pool.
     */
    public final Preference<Number> maxAsyncTasksParallelism = integerPreference("maxAsyncTasksParallelism", 4);

    /**
     * The maximum number of thread to allocate to the sub-tasks thread pool.
     */
    public final Preference<Number> maxSubTasksParallelism = integerPreference("maxSubTasksParallelism", 4);

    /**
     * The threading policy used by the async main thread pool.
     */
    public final Preference<ThreadPoolPolicy> asyncThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "asyncThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    /**
     * The threading policy used by the sub-tasks thread pool.
     */
    public final Preference<ThreadPoolPolicy> subTasksThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "subTasksThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    /**
     * Records the last position of the application window before closing.
     */
    public final Preference<Rectangle2D> windowLastPosition = objectPreference(Rectangle2D.class,
            "windowLastPosition",
            new Rectangle2D(Double.MAX_VALUE, Double.MAX_VALUE, 0, 0),
            rectangle2D -> gson.toJson(rectangle2D),
            s -> gson.fromJson(s, Rectangle2D.class));

    /**
     * Display a warning if the number of series a user attempts to drop onto a worksheet in a single operation
     * is greater than this value.
     */
    public final Preference<Number> maxSeriesPerChartBeforeWarning =
            integerPreference("maxSeriesPerChartBeforeWarning", 50);

    /**
     * The timeout value in ms for asynchronous tasks (an exception will be thrown past this delay).
     */
    public final Preference<Number> asyncTasksTimeOutMs = longPreference("asyncTasksTimeOutMs", 120000L);

    /**
     * Only trigger treeview filter after this amount of characters have been entered into the text field.
     */
    public final Preference<Number> minCharsTreeFiltering = integerPreference("minCharsTreeFiltering", 3);

    /**
     * True if request pooling on the http client should be enabled, false otherwise.
     */
    public final Preference<Boolean> httpPoolingEnabled = booleanPreference("httpPoolingEnabled", true);

    /**
     * True if NaN values in series should be replaced by zero before drawing the chart.
     */
    public final Preference<Boolean> forceNanToZero = booleanPreference("forceNanToZero", true);

    /**
     * True if a heap dump should be generate on out of memory errors, false otherwise.
     */
    public final Preference<Boolean> heapDumpOnOutOfMemoryError =
            booleanPreference("heapDumpOnOutOfMemoryError", false);

    /**
     * The path where to save heap dumps.
     */
    public final Preference<Path> heapDumpPath =
            pathPreference("heapDumpPath",Path.of(System.getProperty("java.io.tmpdir") + "/binjr"));

    /**
     * The amount of time in ms the pointer must have hovered above a node before a tooltip is shown.
     */
    public final Preference<Number> tooltipShowDelayMs = longPreference("tooltipShowDelayMs", 500L);

    public final Preference<Log4j2Level> rootLoggingLevel =
            enumPreference(Log4j2Level.class, "rootLoggingLevel", Log4j2Level.INFO);

    public final Preference<Boolean> redirectStdOutToLogs = booleanPreference("redirectStdOutToLogging", true);

    public final Preference<Path> logFilesLocation =
            pathPreference("logFilesLocation", Path.of( System.getProperty("java.io.tmpdir") + "/binjr"));

    public final Preference<Number> maxLogFilesToKeep = integerPreference("maxLogFilesToKeep", 10);

    public Preference<Boolean> persistLogsToFile = booleanPreference("persistLogsToFile", true);

    private UserPreferences() {
        super(Preferences.userRoot().node(BINJR_GLOBAL));
    }

    public static UserPreferences getInstance() {
        return UserPreferencesHolder.instance;
    }

    private static class UserPreferencesHolder {
        private final static UserPreferences instance = new UserPreferences();
    }
}
