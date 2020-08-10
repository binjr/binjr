package eu.binjr.sources.text.adapters;


import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TextFilesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation to retrieve data from a text file.
 *
 * @author Frederic Thevenet
 */
public class TextDataAdapter extends BaseDataAdapter<String> {
    private static final Logger logger = Logger.create(TextDataAdapter.class);
    private final TextAdapterPreferences prefs = (TextAdapterPreferences) getAdapterInfo().getPreferences();
    private Path rootPath;


    private FileSystemBrowser fileBrowser;

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
     * @param rootPath the {@link Path} from which to load content.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public TextDataAdapter(Path rootPath) throws DataAdapterException {
        super();
        this.rootPath = rootPath;
        Map<String, String> params = new HashMap<>();
        params.put("rootPath", rootPath.toString());

        loadParams(params);
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("rootPath", rootPath.toString());
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (logger.isDebugEnabled()) {
            logger.debug(() -> "TextDataAdapter params:");
            params.forEach((s, s2) -> logger.debug(() -> "key=" + s + ", value=" + s2));
        }
        rootPath = Paths.get(validateParameterNullity(params, "rootPath"));
        try {
            this.fileBrowser = FileSystemBrowser.of(rootPath);
        } catch (IOException e) {
            throw new DataAdapterException("Could not create file system browser instance", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> configNode = new FilterableTreeItem<>(
                new TextFilesBinding.Builder()
                        .withLabel("Configuration Files")
                        .withAdapter(this)
                        .build());
        attachConfigFilesTree(configNode);
        return configNode;
    }

    private void attachConfigFilesTree(FilterableTreeItem<SourceBinding> configNode) throws DataAdapterException {
        try (var p = Profiler.start("Building Config binding tree", logger::perf)) {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toPath("/"), configNode);
            for (Path conf : fileBrowser.listEntries(configPath ->
                    Arrays.stream(prefs.foldersToVisit.get())
                            .map(folder -> configPath.startsWith(fileBrowser.toPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                            Arrays.stream(prefs.textFileExtensions.get())
                                    .map(ext -> configPath.getFileName().toString().toLowerCase(Locale.US).endsWith(ext))
                                    .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = conf.getFileName().toString();
                var attachTo = configNode;
                if (conf.getParent() != null) {
                    attachTo = nodeDict.get(conf.getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, conf.getParent(), configNode);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new TextFilesBinding.Builder()
                                .withLabel(fileName)
                                .withPath(conf.toString())
                                .withParent(attachTo.getValue())
                                .withAdapter(this)
                                .build());
                attachTo.getInternalChildren().add(filenode);
            }
            TreeViewUtils.sortFromBranch(configNode);
        } catch (Exception e) {
            Dialogs.notifyException("Failed to list files from cvdiag: " + e.getMessage(), e);
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
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<String>> seriesInfos) throws DataAdapterException {
        return null;
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
                proc.setData(List.of(new XYChart.Data<>(ZonedDateTime.now(), readTextFileFromCvdiag(info.getBinding().getPath()))));
                data.put(info, proc);
            } catch (IOException e) {
                throw new DataAdapterException("Error fetching text from " + info.getBinding().getPath(), e);
            }
        }
        return data;
    }

    public static class TextProcessor extends TimeSeriesProcessor<String> {

        @Override
        protected String computeMinValue() {
            return null;
        }

        @Override
        protected String computeAverageValue() {
            return null;
        }

        @Override
        protected String computeMaxValue() {
            return null;
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
        return "[Config Files] " + rootPath.getFileName();
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


    public String readTextFileFromCvdiag(String path) throws IOException {
        try (Profiler ignored = Profiler.start("Extracting text from file " + path, logger::perf)) {
            try (var reader = new BufferedReader(new InputStreamReader(fileBrowser.getData(path), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }


}
