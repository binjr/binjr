/*
 *    Copyright 2019-2025 Frederic Thevenet
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
import com.google.gson.GsonBuilder;
import eu.binjr.common.io.SSLContextUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.*;
import eu.binjr.core.appearance.BuiltInChartColorPalettes;
import eu.binjr.core.appearance.BuiltInUserInterfaceThemes;
import eu.binjr.core.appearance.UserInterfaceThemes;
import eu.binjr.core.controllers.TimelineDisplayMode;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.async.ThreadPoolPolicy;
import eu.binjr.core.data.indexes.IndexDirectoryLocation;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.Level;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;

/**
 * Manage all user preferences
 *
 * @author Frederic Thevenet
 */
public class UserPreferences extends ObservablePreferenceFactory {
    private static final Logger logger = Logger.create(UserPreferences.class);
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final String BINJR_GLOBAL = "binjr/global";
    private static final LocalKeyring keyring = new LocalKeyring();
    public ObservablePreference<Boolean> highlightCurrentColumn = booleanPreference("highlightCurrentColumn", true);

    private static class LocalKeyring extends AesKeyring {
        private LocalKeyring() {
            super("binjr/local");
        }

        private final ObservablePreference<SecretKey> masterKey = secretKeyPreference("application_id");
    }

    public ObfuscatedString.Obfuscator getObfuscator() {
        return obfuscator;
    }

    private final ObfuscatedString.Obfuscator obfuscator = new AesStringObfuscator(keyring.masterKey.get());

    private final UserFavorites favorites = new UserFavorites(BINJR_GLOBAL);

    /**
     * True if series down-sampling is enabled, false otherwise.
     */
    public final ObservablePreference<Boolean> downSamplingEnabled = booleanPreference("downSamplingEnabled", true);

    /**
     * The series down-sampling threshold value.
     */
    public final ObservablePreference<Number> downSamplingThreshold = integerPreference("downSamplingThreshold", 1500);

    /**
     * The username used for authenticated access to the GitHub API.
     */
    public final ObservablePreference<String> githubUserName = stringPreference("githubUserName", "");

    /**
     * The authentication token  used for authenticated access to the GitHub API.
     */
    public final ObservablePreference<ObfuscatedString> githubAuthToken = obfuscatedStringPreference("githubAuthToken", "", this.obfuscator);

    /**
     * The User Interface theme applied to the application.
     */
    public final ObservablePreference<UserInterfaceThemes> userInterfaceTheme =
            objectPreference(UserInterfaceThemes.class,
                    "userInterfaceTheme",
                    BuiltInUserInterfaceThemes.SYSTEM,
                    UserInterfaceThemes::name,
                    s -> UserInterfaceThemes.valueOf(s, BuiltInUserInterfaceThemes.SYSTEM));


    /**
     * True if the last open workspace should be reload next time the app if started, false otherwise.
     */
    public final ObservablePreference<Boolean> loadLastWorkspaceOnStartup = booleanPreference("loadLastWorkspaceOnStartup", false);

    /**
     * True to check if a new release is available each time the application starts, false otherwise.
     */
    public final ObservablePreference<Boolean> checkForUpdateOnStartUp = booleanPreference("checkForUpdateOnStartUp", true);

    /**
     * True if the horizontal marker should be displayed on chart views, false otherwise.
     */
    public final ObservablePreference<Boolean> horizontalMarkerOn = booleanPreference("horizontalMarkerOn", false);

    /**
     * True if the vertical marker should be displayed on chart views, false otherwise.
     */
    public final ObservablePreference<Boolean> verticalMarkerOn = booleanPreference("verticalMarkerOn", true);

    /**
     * True if series on area charts should display a brighter coloured outline, false otherwise.
     */
    public final ObservablePreference<Boolean> showOutlineOnAreaCharts = booleanPreference("showOutlineOnAreaCharts", true);

    /**
     * The default opacity value to apply to series on area charts.
     */
    public final ObservablePreference<Number> defaultOpacityAreaCharts = doublePreference("defaultOpacityAreaCharts", 0.30d);

    /**
     * True if series on stacked area charts should display a brighter coloured outline, false otherwise.
     */
    public final ObservablePreference<Boolean> showOutlineOnStackedAreaCharts = booleanPreference("showOutlineOnStackedAreaCharts", false);

    /**
     * The default opacity value to apply to series on stacked area charts.
     */
    public final ObservablePreference<Number> defaultOpacityStackedAreaCharts = doublePreference("defaultOpacityStackedAreaCharts", 0.70d);


    /**
     * The default stroke width value for line and area charts.
     */
    public final ObservablePreference<Number> defaultStrokeWidthLineCharts = doublePreference("defaultStrokeWidthLineCharts", 1.0d);

    /**
     * The default stroke width value for scatter point charts.
     */
    public final ObservablePreference<Number> defaultStrokeWidthScatterCharts = doublePreference("defaultStrokeWidthScatterCharts", 1.5d);

    /**
     * The amount of time notification should over before being automatically dismissed.
     */
    public final ObservablePreference<NotificationDurationChoices> notificationPopupDuration =
            enumPreference(NotificationDurationChoices.class,
                    "notificationPopupDuration",
                    NotificationDurationChoices.TEN_SECONDS);
    /**
     * The location to load plugins from in addition to those on the classpath.
     */
    public final ObservablePreference<Path> userPluginsLocation =
            pathPreference("pluginsLocation", Path.of(System.getProperty("user.home")));


    /**
     * True if plugins from the location defined by "pluginsLocation" should be loaded in addition to those
     * on the classpath.
     */
    public final ObservablePreference<Boolean> loadPluginsFromExternalLocation =
            booleanPreference("loadPluginsFromExternalLocation", false);

    /**
     * The line buffer depth for the debug console.
     */
    public final ObservablePreference<Number> consoleMaxLineCapacity = integerPreference("consoleMaxLineCapacity", 2000);

    /**
     * True if the vertical maker should span over all stacked charts on a worksheet, false if it should only show
     * over the focused chart.
     */
    public final ObservablePreference<Boolean> fullHeightCrosshairMarker = booleanPreference("fullHeightCrosshairMarker", true);

    /**
     * The maximum number of thread to allocate to the asynchronous tasks thread pool.
     */
    public final ObservablePreference<Number> maxAsyncTasksParallelism = integerPreference("maxAsyncTasksParallelism", 4);

    /**
     * The maximum number of thread to allocate to the sub-tasks thread pool.
     */
    public final ObservablePreference<Number> maxSubTasksParallelism = integerPreference("maxSubTasksParallelism", 4);

    /**
     * The threading policy used by the async main thread pool.
     */
    public final ObservablePreference<ThreadPoolPolicy> asyncThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "asyncThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    /**
     * The threading policy used by the sub-tasks thread pool.
     */
    public final ObservablePreference<ThreadPoolPolicy> subTasksThreadPoolPolicy =
            enumPreference(ThreadPoolPolicy.class, "subTasksThreadPoolPolicy", ThreadPoolPolicy.WORK_STEALING);

    /**
     * Records the last position of the application window before closing.
     */
    public final ObservablePreference<Rectangle2D> windowLastPosition = objectPreference(Rectangle2D.class,
            "windowLastPosition",
            new Rectangle2D(Double.MAX_VALUE, Double.MAX_VALUE, 0, 0),
            GSON::toJson,
            s -> GSON.fromJson(s, Rectangle2D.class));

    /**
     * Display a warning if the number of series a user attempts to drop onto a worksheet in a single operation
     * is greater than this value.
     */
    public final ObservablePreference<Number> maxSeriesPerChartBeforeWarning =
            integerPreference("maxSeriesPerChartBeforeWarning", 50);

    /**
     * The timeout value in ms for asynchronous tasks (an exception will be thrown past this delay).
     */
    public final ObservablePreference<Number> asyncTasksTimeOutMs = longPreference("asyncTasksTimeOutMs", 120000L);

    /**
     * Only trigger treeview filter after this amount of characters have been entered into the text field.
     */
    public final ObservablePreference<Number> minCharsTreeFiltering = integerPreference("minCharsTreeFiltering", 1);

    /**
     * True if request pooling on the http client should be enabled, false otherwise.
     */
    public final ObservablePreference<Boolean> httpPoolingEnabled = booleanPreference("httpPoolingEnabled", true);

    /**
     * True if NaN values in series should be replaced by zero before drawing the chart.
     */
    public final ObservablePreference<Boolean> forceNanToZero = booleanPreference("forceNanToZero", true);

    /**
     * True if a heap dump should be generated on out of memory errors, false otherwise.
     */
    public final ObservablePreference<Boolean> heapDumpOnOutOfMemoryError =
            booleanPreference("heapDumpOnOutOfMemoryError", false);

    /**
     * The path where to save heap dumps.
     */
    public final ObservablePreference<Path> heapDumpPath =
            pathPreference("heapDumpPath", Path.of(System.getProperty("java.io.tmpdir") + "/binjr"));

    /**
     * The amount of time in ms the pointer must have hovered above a node before a tooltip is shown.
     */
    public final ObservablePreference<Number> tooltipShowDelayMs = longPreference("tooltipShowDelayMs", 500L);

    public final ObservablePreference<Level> rootLoggingLevel =
            objectPreference(Level.class, "rootLoggingLevel", Level.INFO, Level::name, Level::valueOf);

    public final ObservablePreference<Boolean> redirectStdOutToLogs = booleanPreference("redirectStdOutToLogging", true);

    public final ObservablePreference<Path> logFilesLocation =
            pathPreference("logFilesLocation", Path.of(System.getProperty("java.io.tmpdir") + "/binjr"));

    public final ObservablePreference<Number> maxLogFilesToKeep = integerPreference("maxLogFilesToKeep", 10);

    public final ObservablePreference<Boolean> persistLogsToFile = booleanPreference("persistLogsToFile", true);

    public final ObservablePreference<ScalingFactor> snapshotOutputScale =
            enumPreference(ScalingFactor.class, "snapshotOutputScale", ScalingFactor.AUTO);

    public final ObservablePreference<DownSamplingAlgorithm> downSamplingAlgorithm =
            enumPreference(DownSamplingAlgorithm.class, "downSamplingAlgorithm", DownSamplingAlgorithm.AUTO);

    public final ObservablePreference<BuiltInChartColorPalettes> chartColorPalette =
            enumPreference(BuiltInChartColorPalettes.class, "chartColorPalette", BuiltInChartColorPalettes.VIBRANT);

    public final ObservablePreference<BuiltInChartColorPalettes> logFilesColorPalette =
            enumPreference(BuiltInChartColorPalettes.class, "logFilesColorPalette", BuiltInChartColorPalettes.GRAY_SCALE);

    public final ObservablePreference<LocalDateTime> lastCheckForUpdate =
            localDateTimePreference("lastCheckForUpdate", LocalDateTime.MIN);

    public final ObservablePreference<Number> searchFieldInputDelayMs = integerPreference("searchFieldInputDelayMs", 600);

    public final ObservablePreference<Number> hitsPerPage = integerPreference("hitsPerPage", 10000);

    public final ObservablePreference<IndexDirectoryLocation> indexLocation =
            enumPreference(IndexDirectoryLocation.class, "indexLocation", IndexDirectoryLocation.FILES_SYSTEM);

    public final ObservablePreference<Number> parsingThreadNumber = integerPreference("parsingThreadNumber", 0);

    public final ObservablePreference<Number> blockingQueueCapacity = integerPreference("blockingQueueCapacity", 10000);

    public final ObservablePreference<Number> parsingThreadDrainSize = integerPreference("parsingThreadDrainSize", 512);

    public final ObservablePreference<Boolean> preventFoldingAllSourcePanes = booleanPreference("preventFoldingAllSourcePanes", false);

    public final ObservablePreference<Boolean> expandSuggestTreeOnMatch = booleanPreference("expandSuggestTreeOnMatch", false);

    public final ObservablePreference<Boolean> consoleAlwaysOnTop = booleanPreference("consoleAlwaysOnTop", false);

    public final ObservablePreference<Number> minChartHeight = doublePreference("minChartHeight", 150.0);

    public final ObservablePreference<Number> lowerStackedChartHeight = doublePreference("lowerStackedChartHeight", 80.0);

    public final ObservablePreference<Number> lowerOverlaidChartHeight = doublePreference("lowerOverlaidChartHeight", 250.0);

    public final ObservablePreference<Number> upperChartHeight = doublePreference("upperChartHeight", 600.0);

    public final ObservablePreference<Number> chartZoomFactor = doublePreference("chartZoomFactor", 200.0);

    public final ObservablePreference<Number> chartZoomTriggerDelayMs = doublePreference("chartZoomTriggerDelayMs", 500.0);

    public final ObservablePreference<Number> logHeatmapNbBuckets = integerPreference("logHeatmapNbBuckets", 150);

    public final ObservablePreference<Boolean> logFilterBarVisible = booleanPreference("logFilterBarVisible", true);

    public final ObservablePreference<Boolean> logFindBarVisible = booleanPreference("logFindBarVisible", false);

    public final ObservablePreference<Boolean> logHeatmapVisible = booleanPreference("logHeatmapVisible", true);

    public final ObservablePreference<Boolean> doNotWarnOnChartClose = booleanPreference("doNotWarnOnChartClose", false);

    public final ObservablePreference<Boolean> doNotWarnOnTabClose = booleanPreference("doNotWarnOnTabClose", false);

    public final ObservablePreference<Color> invalidInputColor =
            objectPreference(Color.class, "invalidInputColor", Color.valueOf("0xff646440"), Color::toString, Color::valueOf);

    public final ObservablePreference<Number> facetResultCacheEntries = integerPreference("facetResultCacheEntries", 150);

    public final ObservablePreference<Number> hitResultCacheMaxSizeMiB = integerPreference("hitResultCacheMaxSizeMiB", 64);

    public final ObservablePreference<Boolean> enableHttpProxy = booleanPreference("enableHttpProxy", false);

    public final ObservablePreference<String> httpProxyHost = stringPreference("httpProxyHost", "");

    public final ObservablePreference<Number> httpProxyPort = integerPreference("httpProxyPort", 0);

    public final ObservablePreference<Boolean> useHttpProxyAuth = booleanPreference("useHttpProxyAuth", false);

    public final ObservablePreference<String> httpProxyLogin = stringPreference("httpProxyLogin", "");

    public final ObservablePreference<ObfuscatedString> httpProxyPassword = obfuscatedStringPreference("httpProxyPassword", "", obfuscator);

    public final ObservablePreference<Number> maxSnapshotSnippetHeight = integerPreference("maxSnapshotSnippetHeight", 400);

    public final ObservablePreference<Number> maxSnapshotSnippetWidth = integerPreference("maxSnapshotSnippetWidth", 300);

    public final ObservablePreference<Number> httpSocketTimeoutMs = integerPreference("httpSocketTimeoutMs", 30000);

    public final ObservablePreference<Number> httpConnectionTimeoutMs = integerPreference("httpConnectionTimeoutMs", 30000);

    public final ObservablePreference<Number> httpResponseTimeoutMs = integerPreference("httpResponseTimeoutMs", 30000);

    public final ObservablePreference<Number> httpSSLConnectionTTLMs = integerPreference("httpSSLConnectionTTLMs", 120000);

    public final ObservablePreference<Number> dataAdapterFetchCacheMaxSizeMiB = integerPreference("dataAdapterFetchCacheMaxSizeMiB", 32);

    public final ObservablePreference<Path> temporaryFilesRoot = pathPreference("temporaryFilesRoot", Path.of(System.getProperty("java.io.tmpdir")));

    public final ObservablePreference<Number> maxLinesFileTestPreview = integerPreference("maxLinesFileTestPreview", 20);

    public final ObservablePreference<ScalingFactor> uiScalingFactor = enumPreference(ScalingFactor.class, "uiScalingFactor", ScalingFactor.AUTO);

    public final ObservablePreference<Boolean> javaFxVerbose = booleanPreference("javaFxVerbose", false);

    public final ObservablePreference<Boolean> autoAttemptBasicAuth = booleanPreference("autoAttemptBasicAuth", true);

    /**
     * A list of user defined {@link ParsingProfile} for parsing log events
     */
    public final ObservablePreference<ParsingProfile[]> userLogEventsParsingProfiles =
            objectPreference(ParsingProfile[].class,
                    "userParsingProfiles",
                    new ParsingProfile[0],
                    s -> GSON.toJson(s),
                    s -> GSON.fromJson(s, CustomParsingProfile[].class)
            );

    public final ObservablePreference<DateFormat> labelDateFormat = enumPreference(DateFormat.class, "labelDateFormat", DateFormat.RFC_1123);

    public final ObservablePreference<Number> numIdxMaxPageSize = integerPreference("numIdxMaxPageSize", 200000);

    public final ObservablePreference<Number> defaultTextViewFontSize = integerPreference("defaultFontSize", 9);

    public final ObservablePreference<DateTimeAnchor> defaultDateTimeAnchor = enumPreference(DateTimeAnchor.class, "defaultDateTimeAnchor", DateTimeAnchor.EPOCH);

    public final ObservablePreference<Boolean> alwaysUseEmbeddedFonts = booleanPreference("alwaysUseEmbeddedFonts", false);

    public final ObservablePreference<Number> logIndexNGramSize = integerPreference("logIndexNGramSize", 2);

    public final ObservablePreference<Boolean> logIndexAutoExpendShorterTerms = booleanPreference("logIndexAutoExpendShorterTerms", false);

    public final ObservablePreference<Boolean> optimizeNGramQueries = booleanPreference("optimizeNGramQueries", true);

    public final ObservablePreference<Boolean> showInlineHelpButtons = booleanPreference("showInlineHelpButtons", true);

    public final ObservablePreference<IndexingTokenizer> indexingTokenizer =
            enumPreference(IndexingTokenizer.class, "indexingTokenizer", IndexingTokenizer.NGRAMS);

    public final ObservablePreference<Boolean> defaultForceZeroInYAxisAutoRange = booleanPreference("defaultForceZeroInYAxisAutoRange", true);

    public final ObservablePreference<ChartType> defaultChartType = enumPreference(ChartType.class, "defaultChartType", ChartType.STACKED, ChartType.UNDEFINED);

    public final ObservablePreference<UnitPrefixes> defaultUnitPrefix = enumPreference(UnitPrefixes.class, "defaultUnitPrefix", UnitPrefixes.METRIC, UnitPrefixes.UNDEFINED);

    public final ObservablePreference<Boolean> forceTunnelingDisabledSchemes = booleanPreference("forceTunnelingDisabledSchemes", false);

    public final ObservablePreference<Boolean> keepFailedConnectionSourcePaneOpen = booleanPreference("keepFailedConnectionSourcePaneOpen", false);

    public final ObservablePreference<Boolean> closeEmptyTabPanes = booleanPreference("closeEmptyTabPanes", true);

    public final ObservablePreference<HardwareAccelerationSupport> hardwareAcceleration = enumPreference(HardwareAccelerationSupport.class, "hardwareAcceleration", HardwareAccelerationSupport.AUTO);

    public final ObservablePreference<String> colorNamesHashingAlgorithm = stringPreference("colorNamesHashingAlgorithm", "MD5");

    public final ObservablePreference<Number> numMinorTickDecimal = integerPreference("numMinorTickDecimal", 4);

    public final ObservablePreference<Number> numMinorTickBinary = integerPreference("numMinorTickBinary", 3);

    public final ObservablePreference<Number> singleMinTickThreshold = doublePreference("singleMinTickThreshold", 36.0);

    public final ObservablePreference<Boolean> useParallelIndexFetch = booleanPreference("useParallelIndexFetch", true);

    public final ObservablePreference<String> userAgentString = stringPreference("userAgentString", "");

    public ObservablePreference<TimelineDisplayMode> defaultTimelineDisplayMode = enumPreference(TimelineDisplayMode.class, "defaultTimelineDisplayMode", TimelineDisplayMode.DATE_TIME);

    public final ObservablePreference<SSLContextUtils.PlatformKeyStore> platformKeyStore = enumPreference(SSLContextUtils.PlatformKeyStore.class, "platformKeyStore", SSLContextUtils.PlatformKeyStore.AUTO);

    public final ObservablePreference<double[]> decimalAxisTickDividers = objectPreference(
            double[].class,
            "decimalAxisTickDividers",
            new double[]{1.0, 2.5, 5.0},
            GSON::toJson,
            s -> GSON.fromJson(s, double[].class)
    );

    public ObservablePreference<double[]> binaryAxisTickDividers = objectPreference(
            double[].class,
            "binaryAxisTickDividers",
            new double[]{1.0, 2.0, 4.0, 8.0},
            GSON::toJson,
            s -> GSON.fromJson(s, double[].class)
    );

    public final ObservablePreference<Number> defaultOpacityDurationCharts = doublePreference("defaultOpacityDurationCharts", 0.30d);

    public final ObservablePreference<Number> defaultStrokeWidthDurationCharts = doublePreference("defaultDurationWidthScatterCharts", 0.5d);

    public final ObservablePreference<Boolean> showOutlineOnDurationCharts = booleanPreference("showOutlineOnDurationCharts", true);

    public static class UserFavorites extends MruFactory {

        public final MostRecentlyUsedList<String> favoriteLogFilters =
                stringMostRecentlyUsedList("favoriteLogFilters", Integer.MAX_VALUE);

        public UserFavorites(String root) {
            super(root + "/favorites");
        }
    }

    public UserFavorites getFavorites() {
        return favorites;
    }

    @Override
    public void reset() throws BackingStoreException {
        super.reset();
        // Reset adapter preferences
        for (var di : DataAdapterFactory.getInstance().getAllAdapters()) {
            di.getPreferences().reset();
        }
        // Reset user favorites
        favorites.reset();
    }

    @Override
    public void importFromFile(Path savePath) throws IOException, InvalidPreferencesFormatException {
        super.importFromFile(savePath);
        // Reload imported settings to adapter preferences
        for (var di : DataAdapterFactory.getInstance().getAllAdapters()) {
            di.getPreferences().reload();
        }
        // Reload User favorites
        favorites.reload();
    }

    private UserPreferences() {
        super(BINJR_GLOBAL);
    }

    public static UserPreferences getInstance() {
        return UserPreferencesHolder.instance;
    }

    private static class UserPreferencesHolder {
        private final static UserPreferences instance = new UserPreferences();
    }

    private final Map<String, String> severityStyleMap = Map.ofEntries(
            Map.entry("0", "palette_15"),
            Map.entry("1", "palette_10"),
            Map.entry("2", "palette_5"),
            Map.entry("3", "palette_12"),
            Map.entry("4", "palette_13"),
            Map.entry("unknown", "unknown"),
            Map.entry("finer", "trace"),
            Map.entry("finest", "trace"),
            Map.entry("trace", "trace"),
            Map.entry("fine", "debug"),
            Map.entry("debug", "debug"),
            Map.entry("config", "perf"),
            Map.entry("from_cache", "perf"),
            Map.entry("perf", "perf"),
            Map.entry("stdout", "info"),
            Map.entry("ok", "info"),
            Map.entry("info", "info"),
            Map.entry("timeout", "warn"),
            Map.entry("warning", "warn"),
            Map.entry("warn", "warn"),
            Map.entry("severe", "error"),
            Map.entry("stderr", "error"),
            Map.entry("error", "error"),
            Map.entry("fatal", "fatal"));

    public String mapSeverityStyle(String toMap) {
        var style = severityStyleMap.get(toMap.toLowerCase(Locale.ROOT));
        return (style == null) ? "undefined" : style;
    }

    public ChartType defineChartType(ChartType chartType) {
        return chartType != ChartType.UNDEFINED ? chartType : UserPreferences.getInstance().defaultChartType.get();
    }

    public UnitPrefixes defineUnitPrefix(UnitPrefixes prefix) {
        return prefix != UnitPrefixes.UNDEFINED ? prefix : UserPreferences.getInstance().defaultUnitPrefix.get();
    }

    public boolean getDefaultChartOutlineVisibility(ChartType chartType) {
        return switch (chartType) {
            case STACKED -> showOutlineOnStackedAreaCharts.get();
            case DURATION -> showOutlineOnDurationCharts.get();
            case AREA -> showOutlineOnAreaCharts.get();
            case null, default -> true;
        };
    }

    public double getDefaultChartOpacity(ChartType chartType) {
        return switch (chartType) {
            case STACKED -> defaultOpacityStackedAreaCharts.get().doubleValue();
            case DURATION -> defaultOpacityDurationCharts.get().doubleValue();
            case AREA -> defaultOpacityAreaCharts.get().doubleValue();
            case null, default -> 0.4;
        };
    }

    public double getDefaultChartStrokeWidth(ChartType chartType) {
        return switch (chartType) {
            case SCATTER, BAR -> defaultStrokeWidthScatterCharts.get().doubleValue();
            case DURATION, EVENT -> defaultStrokeWidthDurationCharts.get().doubleValue();
            case LINE, AREA -> defaultStrokeWidthLineCharts.get().doubleValue();
            case null, default -> 1.0;
        };
    }


}
