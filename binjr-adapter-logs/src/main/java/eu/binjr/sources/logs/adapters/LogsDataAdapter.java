/*
 *    Copyright 2020-2022 Frederic Thevenet
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

package eu.binjr.sources.logs.adapters;


import com.google.gson.Gson;
import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.indexes.*;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.LogEventFormat;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.LogFileSeriesInfo;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserHistory;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link DataAdapter} implementation to retrieve data from a text file.
 *
 * @author Frederic Thevenet
 */
public class LogsDataAdapter extends BaseDataAdapter<SearchHit> implements ProgressAdapter<SearchHit> {
    private static final Logger logger = Logger.create(LogsDataAdapter.class);
    private static final Gson gson = new Gson();
    private static final String DEFAULT_PREFIX = "[Logs]";
    private static final String ZONE_ID_PARAM_NAME = "zoneId";
    private static final String ROOT_PATH_PARAM_NAME = "rootPath";
    private static final String FOLDER_FILTERS_PARAM_NAME = "folderFilters";
    private static final String EXTENSIONS_FILTERS_PARAM_NAME = "fileExtensionsFilters";
    private static final String PARSING_PROFILE_PARAM_NAME = "parsingProfile";
    private static final String LOG_FILE_ENCODING = "LOG_FILE_ENCODING";
    private static final Property<IndexingStatus> INDEXING_OK = new ReadOnlyObjectWrapper(new SimpleObjectProperty<>(IndexingStatus.OK));

    private final String sourceNamePrefix;
    private final Map<String, IndexingStatus> indexedFiles = new HashMap<>();
    private final BinaryPrefixFormatter binaryPrefixFormatter = new BinaryPrefixFormatter("###,###.## ");
    private final MostRecentlyUsedList<String> defaultParsingProfiles =
            UserHistory.getInstance().stringMostRecentlyUsedList("defaultParsingProfiles", 100);
    private final MostRecentlyUsedList<String> userParsingProfiles =
            UserHistory.getInstance().stringMostRecentlyUsedList("userParsingProfiles", 100);
    private final Charset encoding;
    private Path rootPath;
    private Indexable index;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private ParsingProfile parsingProfile;
    private EventFormat parser;
    private ZoneId zoneId;

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public LogsDataAdapter() throws DataAdapterException {
        super();
        zoneId = ZoneId.systemDefault();
        encoding = StandardCharsets.UTF_8;
        sourceNamePrefix = DEFAULT_PREFIX;
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param rootPath              the {@link Path} from which to load content.
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions to inspect for content.
     * @param profile               the parsing profile to use.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(Path rootPath,
                           ZoneId zoneId,
                           String[] folderFilters,
                           String[] fileExtensionsFilters,
                           ParsingProfile profile) throws DataAdapterException {
        this(DEFAULT_PREFIX, rootPath, zoneId, StandardCharsets.UTF_8, folderFilters, fileExtensionsFilters, profile);
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param sourcePrefix          the name to prepend the source with.
     * @param rootPath              the {@link Path} from which to load content.
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions to inspect for content.
     * @param profile               the parsing profile to use.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(String sourcePrefix,
                           Path rootPath,
                           ZoneId zoneId,
                           String[] folderFilters,
                           String[] fileExtensionsFilters,
                           ParsingProfile profile) throws DataAdapterException {
        this(sourcePrefix, rootPath, zoneId, StandardCharsets.UTF_8, folderFilters, fileExtensionsFilters, profile);
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param rootPath              the {@link Path} from which to load content.
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions to inspect for content.
     * @param profile               the parsing profile to use.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(Path rootPath,
                           ZoneId zoneId,
                           Charset encoding,
                           String[] folderFilters,
                           String[] fileExtensionsFilters,
                           ParsingProfile profile) throws DataAdapterException {
        this(DEFAULT_PREFIX, rootPath, zoneId, encoding, folderFilters, fileExtensionsFilters, profile);
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param sourcePrefix          the name to prepend the source with.
     * @param rootPath              the {@link Path} from which to load content.
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions to inspect for content.
     * @param profile               the parsing profile to use.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(String sourcePrefix,
                           Path rootPath,
                           ZoneId zoneId,
                           Charset encoding,
                           String[] folderFilters,
                           String[] fileExtensionsFilters,
                           ParsingProfile profile) throws DataAdapterException {
        super();
        this.sourceNamePrefix = sourcePrefix;
        this.rootPath = rootPath;
        this.encoding = encoding;
        Map<String, String> params = new HashMap<>();
        initParams(rootPath, zoneId, folderFilters, fileExtensionsFilters, profile);
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(LOG_FILE_ENCODING, getEncoding());
        params.put(ROOT_PATH_PARAM_NAME, rootPath.toString());
        params.put(ZONE_ID_PARAM_NAME, zoneId.toString());
        params.put(FOLDER_FILTERS_PARAM_NAME, gson.toJson(folderFilters));
        params.put(EXTENSIONS_FILTERS_PARAM_NAME, gson.toJson(fileExtensionsFilters));
        params.put(PARSING_PROFILE_PARAM_NAME, gson.toJson(CustomParsingProfile.of(parsingProfile)));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (logger.isDebugEnabled()) {
            logger.debug(() -> "LogsDataAdapter params:");
            params.forEach((s, s2) -> logger.debug(() -> "key=" + s + ", value=" + s2));
        }
        initParams(Paths.get(validateParameterNullity(params, ROOT_PATH_PARAM_NAME)),
                validateParameter(params, ZONE_ID_PARAM_NAME,
                        s -> {
                            if (s == null) {
                                logger.warn("Parameter " + ZONE_ID_PARAM_NAME + " is missing in adapter " + getSourceName());
                                return ZoneId.systemDefault();
                            }
                            return ZoneId.of(s);
                        }),
                gson.fromJson(validateParameterNullity(params, FOLDER_FILTERS_PARAM_NAME), String[].class),
                gson.fromJson(validateParameterNullity(params, EXTENSIONS_FILTERS_PARAM_NAME), String[].class),
                gson.fromJson(validateParameterNullity(params, PARSING_PROFILE_PARAM_NAME), CustomParsingProfile.class));
    }

    private void initParams(Path rootPath,
                            ZoneId zoneId,
                            String[] folderFilters,
                            String[] fileExtensionsFilters,
                            ParsingProfile parsingProfile) throws DataAdapterException {
        this.rootPath = rootPath;
        this.zoneId = zoneId;
        this.folderFilters = folderFilters;
        this.fileExtensionsFilters = fileExtensionsFilters;
        this.parsingProfile = parsingProfile;
        this.parser = new LogEventFormat(parsingProfile, getTimeZoneId(), encoding);
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(rootPath);
            this.index = Indexes.LOG_FILES.acquire();
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> configNode = new FilterableTreeItem<>(
                new LogFilesBinding.Builder()
                        .withLabel(getSourceName())
                        .withAdapter(this)
                        .build());
        attachNodes(configNode);
        return configNode;
    }

    private void attachNodes(FilterableTreeItem<SourceBinding> root) throws DataAdapterException {
        try (var p = Profiler.start("Building log files binding tree", logger::perf)) {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toInternalPath("/"), root);
            for (var fsEntry : fileBrowser.listEntries(path -> path.getFileName() != null &&
                    Arrays.stream(folderFilters)
                            .map(folder -> folder.equalsIgnoreCase("*") || path.startsWith(fileBrowser.toInternalPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                    Arrays.stream(fileExtensionsFilters)
                            .map(f -> path.getFileName().toString().matches(("\\Q" + f + "\\E")
                                    .replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q")))
                            .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = fsEntry.getPath().getFileName().toString();
                var attachTo = root;
                if (fsEntry.getPath().getParent() != null) {
                    attachTo = nodeDict.get(fsEntry.getPath().getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, fsEntry.getPath().getParent(), root);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(fileName + " (" + binaryPrefixFormatter.format(fsEntry.getSize()) + "B)")
                                .withPath(getId() + "/" + fsEntry.getPath().toString())
                                .withParent(attachTo.getValue())
                                .withParsingProfile(parsingProfile)
                                .withAdapter(this)
                                .build());
                attachTo.getInternalChildren().add(filenode);
            }
            TreeViewUtils.sortFromBranch(root);
        } catch (Exception e) {
            Dialogs.notifyException("Error while enumerating files: " + e.getMessage(), e);
        }
    }

    private FilterableTreeItem<SourceBinding> makeBranchNode(Map<Path, FilterableTreeItem<SourceBinding>> nodeDict,
                                                             Path path,
                                                             FilterableTreeItem<SourceBinding> root) {
        var parent = root;
        var rootPath = path.isAbsolute() ? path.getRoot() : path.getName(0);
        for (int i = 0; i < path.getNameCount(); i++) {
            Path current = rootPath.resolve(path.getName(i));
            FilterableTreeItem<SourceBinding> filenode = nodeDict.get(current);
            if (filenode == null) {
                filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(current.getFileName().toString())
                                .withPath(getId() + "/" + path.toString())
                                .withParent(parent.getValue())
                                .withParsingProfile(parsingProfile)
                                .withAdapter(this)
                                .build());
                nodeDict.put(current, filenode);
                parent.getInternalChildren().add(filenode);
            }
            parent = filenode;
            rootPath = current;
        }
        return parent;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<SearchHit>> seriesInfo) throws DataAdapterException {
        try {
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(this::getPathFacetValue).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }


    @Deprecated
    @Override
    public Map<TimeSeriesInfo<SearchHit>, TimeSeriesProcessor<SearchHit>> fetchData(String path,
                                                                                    Instant begin,
                                                                                    Instant end,
                                                                                    List<TimeSeriesInfo<SearchHit>> seriesInfo,
                                                                                    boolean bypassCache) throws DataAdapterException {
        return loadSeries(path, seriesInfo, bypassCache ? ReloadPolicy.ALL : ReloadPolicy.UNLOADED, null, INDEXING_OK);
    }

    @Override
    public Map<TimeSeriesInfo<SearchHit>, TimeSeriesProcessor<SearchHit>> loadSeries(String path,
                                                                                     List<TimeSeriesInfo<SearchHit>> seriesInfo,
                                                                                     ReloadPolicy reloadPolicy,
                                                                                     DoubleProperty progress,
                                                                                     Property<IndexingStatus> indexingStatus) throws DataAdapterException {
        Map<TimeSeriesInfo<SearchHit>, TimeSeriesProcessor<SearchHit>> data = new HashMap<>();
        try {
            ensureIndexed(seriesInfo.stream()
                            .filter(s -> s instanceof LogFileSeriesInfo)
                            .map(s -> (LogFileSeriesInfo) s)
                            .toList(),
                    progress,
                    reloadPolicy,
                    indexingStatus);
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching logs from " + path, e);
        }
        return data;
    }

    private synchronized void ensureIndexed(List<LogFileSeriesInfo> seriesInfo,
                                            DoubleProperty progress,
                                            ReloadPolicy reloadPolicy,
                                            Property<IndexingStatus> indexingStatus) throws IOException {
        final var toDo = seriesInfo.stream()
                .filter(p -> switch (reloadPolicy) {
                    case ALL -> true;
                    case UNLOADED -> !indexedFiles.containsKey(getPathFacetValue(p));
                    case INCOMPLETE ->
                            indexedFiles.getOrDefault(getPathFacetValue(p), IndexingStatus.CANCELED) == IndexingStatus.CANCELED;
                })
                .toList();
        if (toDo.size() > 0) {
            final long totalSizeInBytes = toDo.stream()
                    .map(CheckedLambdas.wrap((CheckedFunction<LogFileSeriesInfo, Long, IOException>)
                            e -> fileBrowser.getEntry(e.getBinding().getPath().replace(getId() + "/", "")).getSize()))
                    .reduce(Long::sum).orElse(0L);

            final ChangeListener<Number> progressListener = (observable, oldValue, newValue) -> {
                if (newValue != null && totalSizeInBytes > 0) {
                    var oldProgress = (oldValue.longValue() * 100 / totalSizeInBytes) / 100.0;
                    var newProgress = (newValue.longValue() * 100 / totalSizeInBytes) / 100.0;
                    if (progress != null && oldProgress != newProgress) {
                        Dialogs.runOnFXThread(() -> progress.setValue(newProgress));
                    }
                }
            };

            final LongProperty charRead = new SimpleLongProperty(0);
            charRead.addListener(progressListener);
            try {
                for (int i = 0; i < toDo.size(); i++) {
                    var tsInfo = toDo.get(i);
                    String path = tsInfo.getBinding().getPath();
                    var parser = getEventParser(tsInfo);
                    var key = getPathFacetValue(tsInfo);
                    index.add(key,
                            fileBrowser.getData(path.replace(getId() + "/", "")),
                            (i == toDo.size() - 1), // commit if last file
                            parser,
                            charRead,
                            indexingStatus);
                    indexedFiles.put(key, indexingStatus.getValue());
                }
            } finally {
                // remove listener
                charRead.removeListener(progressListener);
                if (progress != null) {
                    Dialogs.runOnFXThread(() -> progress.setValue(-1));
                }
                // reset cancellation request
                indexingStatus.setValue(IndexingStatus.OK);
            }
        }
        // Update loading status for series
        for (var series : seriesInfo) {
            series.setIndexingStatus(indexedFiles.get(getPathFacetValue(series)));
        }
    }

    private String readTextFile(String path) throws IOException {
        try (Profiler ignored = Profiler.start("Extracting text from file " + path, logger::perf)) {
            try (var reader = new BufferedReader(new InputStreamReader(fileBrowser.getData(path), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private String getPathFacetValue(TimeSeriesInfo<?> p) {
        if (p instanceof LogFileSeriesInfo lfsi && lfsi.getParsingProfile() != null) {
            return lfsi.getPathFacetValue();
        }
        return LogFileSeriesInfo.makePathFacetValue(parsingProfile, p);
    }

    private EventFormat getEventParser(TimeSeriesInfo<?> p) {
        if (p instanceof LogFileSeriesInfo lfsi && lfsi.getParsingProfile() != null) {
            return new LogEventFormat(lfsi.getParsingProfile(), getTimeZoneId(), encoding);
        }
        return parser;
    }

    @Override
    public String getEncoding() {
        return encoding.name();
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public String getSourceName() {
        return String.format("%s %s", sourceNamePrefix, rootPath != null ? rootPath.getFileName() : "???");
    }

    @Override
    public void close() {
        try {
            Indexes.LOG_FILES.release();
        } catch (Exception e) {
            logger.error("An error occurred while releasing index " + Indexes.LOG_FILES.name() + ": " + e.getMessage());
            logger.debug("Stack Trace:", e);
        }
        IOUtils.close(fileBrowser);
        super.close();
    }
}
