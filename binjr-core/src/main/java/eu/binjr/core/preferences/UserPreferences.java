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
import eu.binjr.common.preferences.Preference;
import eu.binjr.common.preferences.PreferenceFactory;
import eu.binjr.common.xml.XmlUtils;
import eu.binjr.core.appearance.BuiltInUserInterfaceThemes;
import eu.binjr.core.appearance.UserInterfaceThemes;
import eu.binjr.core.data.async.ThreadPoolPolicy;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class UserPreferences extends PreferenceFactory {
    private static final Logger logger = LogManager.getLogger(UserPreferences.class);

    /**
     * True if series down-sampling is enabled, false otherwise.
     */
    public final Preference<Boolean> downSamplingEnabled = booleanPreference("downSamplingEnabled", true);

    /**
     * The series down-sampling threshold value.
     */
    public final Preference<Number> downSamplingThreshold = integerPreference("downSamplingThreshold", 1500);

    /**
     * The username used for authenticated access to the GitHub API
     */
    public final Preference<String> githubUserName = stringPreference("githubUserName", "");

    /**
     * The authentication token  used for authenticated access to the GitHub API
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
     * The threading policy used by the async main thread pool
     */
    public final Preference<ThreadPoolPolicy> asyncThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "asyncThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    /**
     * The threading policy used by the sub-tasks thread pool
     */
    public final Preference<ThreadPoolPolicy> subTasksThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "subTasksThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    public final Preference<Boolean> loadLastWorkspaceOnStartup = booleanPreference("loadLastWorkspaceOnStartup", true);

    public final Preference<Boolean> checkForUpdateOnStartUp = booleanPreference("checkForUpdateOnStartUp", true);
    public final Preference<Boolean> horizontalMarkerOn = booleanPreference("horizontalMarkerOn", false);
    public final Preference<Boolean> verticalMarkerOn = booleanPreference("verticalMarkerOn", true);
    public final Preference<Boolean> showAreaOutline = booleanPreference("verticalMarkerOn", true);
    public final Preference<Number> defaultGraphOpacity = doublePreference("defaultGraphOpacity", 0.45d);
    public final Preference<Boolean> shiftPressed = booleanPreference("shiftPressed", false);
    public final Preference<Boolean> ctrlPressed = booleanPreference("ctrlPressed", false);
    public final Preference<Path> pluginsLocation = pathPreference("pluginsLocation", Path.of(System.getProperty("user.home")));

    public final Preference<Duration> notificationPopupDuration = objectPreference(Duration.class,
            "notificationPopupDuration",
            Duration.seconds(10),
            duration -> Double.toString(duration.toSeconds()),
            s -> Duration.seconds(Double.parseDouble(s)));
    Gson gson = new Gson();

    public final Preference<Boolean> loadPluginsFromExternalLocation = booleanPreference("loadPluginsFromExternalLocation", false);
    public final Preference<Number> consoleMaxLineCapacity = integerPreference("consoleMaxLineCapacity", 2000);
    public final Preference<Boolean> fullHeightCrosshairMarker = booleanPreference("fullHeightCrosshairMarker", true);
    public final Preference<Number> maxAsyncTasksParallelism = integerPreference("maxAsyncTasksParallelism", 4);
    public final Preference<Number> maxSubTasksParallelism = integerPreference("maxSubTasksParallelism", 4);
    public final Preference<Rectangle2D> windowLastPosition = objectPreference(Rectangle2D.class,
            "windowLastPosition",
            new Rectangle2D(Double.MAX_VALUE, Double.MAX_VALUE, 0, 0),
            rectangle2D -> gson.toJson(rectangle2D),
            s -> gson.fromJson(s, Rectangle2D.class));

    public final Preference<Number> maxSeriesPerChartBeforeWarning = integerPreference("maxSeriesPerChartBeforeWarning", 50);
    public final Preference<Number> asyncTasksTimeOutMs = longPreference("asyncTasksTimeOutMs", 120000L);
    public final Preference<Number> minCharsTreeFiltering = integerPreference("minCharsTreeFiltering", 3);
    public final Preference<Boolean> httpPoolingEnabled = booleanPreference("httpPoolingEnabled", true);
    public final Preference<Boolean> forceNanToZero = booleanPreference("forceNanToZero", true);


    private UserPreferences() {
        super(Preferences.userRoot().node("binjr/global"));
    }

    public static UserPreferences getInstance() {
        return UserPreferencesHolder.instance;
    }

    private static class UserPreferencesHolder {
        private final static UserPreferences instance = new UserPreferences();
    }


}
