/*
 *    Copyright 2020 Frederic Thevenet
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
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.LogFilesBinding;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.index.IndexManager;
import eu.binjr.core.data.index.LogFileIndex;
import eu.binjr.core.data.index.LogParserParameters;
import eu.binjr.core.data.timeseries.LogEvent;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.dialogs.Dialogs;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class LogsDataAdapter extends BaseDataAdapter<LogEvent> {
    private static final Logger logger = Logger.create(LogsDataAdapter.class);
    private static final Gson gson = new Gson();

    public static final String LOG_FILE_INDEX = "logFileIndex";
    protected final LogsAdapterPreferences prefs = (LogsAdapterPreferences) getAdapterInfo().getPreferences();
    private LogFileIndex index;
    protected Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private final Set<String> indexedFiles = new HashSet<>();
    private final BinaryPrefixFormatter binaryPrefixFormatter = new BinaryPrefixFormatter("###,###.## ");
    private LogParserParameters logParserParameters;

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public LogsDataAdapter() throws DataAdapterException {
        super();
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param rootPath the {@link Path} from which to load content.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(Path rootPath, String[] folderFilters, String[] fileExtensionsFilters) throws DataAdapterException {
        super();
        this.rootPath = rootPath;
        Map<String, String> params = new HashMap<>();
        initParams(rootPath, folderFilters, fileExtensionsFilters);
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("rootPath", rootPath.toString());
        params.put("folderFilters", gson.toJson(folderFilters));
        params.put("fileExtensionsFilters", gson.toJson(fileExtensionsFilters));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (logger.isDebugEnabled()) {
            logger.debug(() -> "LogsDataAdapter params:");
            params.forEach((s, s2) -> logger.debug(() -> "key=" + s + ", value=" + s2));
        }
        initParams(Paths.get(validateParameterNullity(params, "rootPath")),
                gson.fromJson(validateParameterNullity(params, "folderFilters"), String[].class),
                gson.fromJson(validateParameterNullity(params, "fileExtensionsFilters"), String[].class));
    }

    private void initParams(Path rootPath, String[] folderFilters, String[] fileExtensionsFilters) throws DataAdapterException {
        this.rootPath = rootPath;
        this.folderFilters = folderFilters;
        this.fileExtensionsFilters = fileExtensionsFilters;
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(rootPath);
            this.index = IndexManager.LOG_INDEX.acquire();
            this.logParserParameters = new LogParserParameters(prefs.timestampPattern.get(),
                    String.format("\\[\\s?(?<severity>%s)\\s?\\]\\s+\\[(?<thread>%s)\\]\\s+\\[(?<logger>%s)\\]",
                            prefs.severityPattern.get(),
                            prefs.threadPattern.get(),
                            prefs.loggerPattern.get()
                    ),
                    "yyyy MM dd HH mm ss SSS",
                    getTimeZoneId());
            logger.debug(() -> "Log parsing params: " + this.logParserParameters.toString());
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> configNode = new FilterableTreeItem<>(
                new LogFilesBinding.Builder()
                        .withLabel("Log Files")
                        .withAdapter(this)
                        .build());
        attachNodes(configNode);
        return configNode;
    }

    private void attachNodes(FilterableTreeItem<SourceBinding> configNode) throws DataAdapterException {
        try (var p = Profiler.start("Building log files binding tree", logger::perf)) {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toPath("/"), configNode);
            for (var fsEntry : fileBrowser.listEntries(configPath ->
                    Arrays.stream(folderFilters)
                            .map(folder -> folder.equalsIgnoreCase("*") || configPath.startsWith(fileBrowser.toPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                            Arrays.stream(fileExtensionsFilters)
                                    .map(ext -> ext.equalsIgnoreCase("*") || configPath.getFileName().toString().toLowerCase(Locale.US).endsWith(ext))
                                    .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = fsEntry.getPath().getFileName().toString();
                var attachTo = configNode;
                if (fsEntry.getPath().getParent() != null) {
                    attachTo = nodeDict.get(fsEntry.getPath().getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, fsEntry.getPath().getParent(), configNode);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(fileName + " (" + binaryPrefixFormatter.format(fsEntry.getSize()) + "B)")
                                .withPath(getId() + "/" + fsEntry.getPath().toString())
                                .withParent(attachTo.getValue())
                                .withAdapter(this)
                                .build());
                attachTo.getInternalChildren().add(filenode);
            }
            TreeViewUtils.sortFromBranch(configNode);
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
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<LogEvent>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo);
            return index.getTimeRangeBoundaries(
                    seriesInfo.stream()
                            .map(i -> i.getBinding().getPath())
                            .collect(Collectors.toList()), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }

    private synchronized void ensureIndexed(List<TimeSeriesInfo<LogEvent>> seriesInfo) throws IOException {
        var toDo = seriesInfo.stream()
                .map(s -> s.getBinding().getPath())
                .filter(p -> !indexedFiles.contains(p))
                .collect(Collectors.toList());
        for (int i = 0; i < toDo.size(); i++) {
            String path = toDo.get(i);
            index.add(path, fileBrowser.getData(path.replace(getId() + "/", "")), (i == toDo.size() - 1), getLogParser());
            indexedFiles.add(path);
        }
    }

    @Override
    public Map<TimeSeriesInfo<LogEvent>, TimeSeriesProcessor<LogEvent>> fetchData(String path,
                                                                                  Instant start,
                                                                                  Instant end,
                                                                                  List<TimeSeriesInfo<LogEvent>> seriesInfo,
                                                                                  boolean bypassCache) throws DataAdapterException {
        Map<TimeSeriesInfo<LogEvent>, TimeSeriesProcessor<LogEvent>> data = new HashMap<>();
        try {
            ensureIndexed(seriesInfo);
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching logs from " + path, e);
        }
        return data;
    }

    private String readTextFile(String path) throws IOException {
        try (Profiler ignored = Profiler.start("Extracting text from file " + path, logger::perf)) {
            try (var reader = new BufferedReader(new InputStreamReader(fileBrowser.getData(path), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @Override
    public String getEncoding() {
        return "utf-8";
    }

    @Override
    public ZoneId getTimeZoneId() {
        return ZoneId.systemDefault();
    }

    @Override
    public String getSourceName() {
        return "[Logs] " + rootPath.getFileName();
    }

    @Override
    public void close() {
        try {
            IndexManager.LOG_INDEX.release();
        } catch (Exception e) {
            logger.error("An error occurred while releasing index " + LOG_FILE_INDEX + ": " + e.getMessage());
            logger.debug("Stack Trace:", e);
        }
        IOUtils.close(fileBrowser);
        super.close();
    }

    public LogParserParameters getLogParser() {
        return logParserParameters;
    }


}
