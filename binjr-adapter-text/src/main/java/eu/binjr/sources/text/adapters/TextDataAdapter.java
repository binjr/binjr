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

package eu.binjr.sources.text.adapters;


import com.google.gson.Gson;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TextFilesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.TextProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.dialogs.Dialogs;
import javafx.scene.chart.XYChart;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation to retrieve data from a text file.
 *
 * @author Frederic Thevenet
 */
public class TextDataAdapter extends BaseDataAdapter<String> {
    private static final Logger logger = Logger.create(TextDataAdapter.class);
    private static final Gson gson = new Gson();
    private final BinaryPrefixFormatter binaryPrefixFormatter = new BinaryPrefixFormatter("###,###.## ");
    private Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;

    /**
     * Initializes a new instance of the {@link TextDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public TextDataAdapter() throws DataAdapterException {
        super();
    }

    /**
     * Initializes a new instance of the {@link TextDataAdapter} class from the provided {@link Path}
     *
     * @param rootPath              the {@link Path} from which to load content.
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions to inspect for content.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public TextDataAdapter(Path rootPath, String[] folderFilters, String[] fileExtensionsFilters) throws DataAdapterException {
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
            logger.debug(() -> "TextDataAdapter params:");
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
        try {
            this.fileBrowser = FileSystemBrowser.of(rootPath);
        } catch (IOException e) {
            throw new DataAdapterException("Could not create file system browser instance", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> rootNode = new FilterableTreeItem<>(
                new TextFilesBinding.Builder()
                        .withLabel(getSourceName())
                        .withAdapter(this)
                        .build());
        attachTextFilesTree(rootNode);
        return rootNode;
    }

    private void attachTextFilesTree(FilterableTreeItem<SourceBinding> rootNode) throws DataAdapterException {
        try (var p = Profiler.start("Building text binding tree", logger::perf)) {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toInternalPath("/"), rootNode);
            for (var fsEntry : fileBrowser.listEntries(path ->
                    Arrays.stream(folderFilters)
                            .map(folder -> folder.equalsIgnoreCase("*") || path.startsWith(fileBrowser.toInternalPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                            Arrays.stream(fileExtensionsFilters)
                                    .map(f -> path.getFileName().toString().matches(("\\Q" + f + "\\E").replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q")))
                                    .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = fsEntry.getPath().getFileName().toString();
                var attachTo = rootNode;
                if (fsEntry.getPath().getParent() != null) {
                    attachTo = nodeDict.get(fsEntry.getPath().getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, fsEntry.getPath().getParent(), rootNode);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new TextFilesBinding.Builder()
                                .withLabel(fileName + " (" + binaryPrefixFormatter.format(fsEntry.getSize()) + "B)")
                                .withPath(fsEntry.getPath().toString())
                                .withParent(attachTo.getValue())
                                .withAdapter(this)
                                .build());
                attachTo.getInternalChildren().add(filenode);
            }
            TreeViewUtils.sortFromBranch(rootNode);
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
                        new TextFilesBinding.Builder()
                                .withLabel(current.getFileName().toString())
                                .withPath(path.toString())
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
    public Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> fetchData(String path,
                                                                              Instant start,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<String>> seriesInfos,
                                                                              boolean bypassCache) throws DataAdapterException {
        Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> data = new HashMap<>();
        for (var info : seriesInfos) {
            try {
                var proc = new TextProcessor();
                proc.setData(List.of(new XYChart.Data<>(ZonedDateTime.now(), readTextFile(info.getBinding().getPath()))));
                data.put(info, proc);
            } catch (IOException e) {
                throw new DataAdapterException("Error fetching text from " + info.getBinding().getPath(), e);
            }
        }
        return data;
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
        return "[Text] " + (rootPath != null ? rootPath.getFileName() : "???");
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
    }

    @Override
    public void close() {
        if (fileBrowser != null) {
            try {
                fileBrowser.close();
            } catch (IOException e) {
                logger.error("An error occurred while closing file system browser instance: " + e.getMessage());
                logger.debug(e);
            }
        }
        super.close();
    }

    /**
     * Read the content of the text file located at the provided path
     *
     * @param path the path of the text file to read
     * @return The content of the text file, as a String
     * @throws IOException if an error occurs while reading the file.
     */
    public String readTextFile(String path) throws IOException {
        try (Profiler ignored = Profiler.start("Extracting text from file " + path, logger::perf)) {
            try (var reader = new BufferedReader(new InputStreamReader(fileBrowser.getData(path), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

}
