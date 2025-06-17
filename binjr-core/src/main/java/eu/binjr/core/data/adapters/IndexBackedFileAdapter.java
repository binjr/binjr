/*
 * Copyright 2025 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.core.data.adapters;

import com.google.gson.Gson;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.Index;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.beans.property.*;
import org.apache.lucene.document.Document;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class IndexBackedFileAdapter<F extends EventFormat<InputStream>, P extends ParsingProfile> extends BaseDataAdapter<Double> implements Reloadable<Double> {
    private static final Logger logger = Logger.create(IndexBackedFileAdapter.class);
    private static final Gson GSON = new Gson();
    protected static final String ZONE_ID = "zoneId";
    protected static final String ENCODING = "encoding";
    protected static final String PARSING_PROFILE = "parsingProfile";
    private static final String FOLDER_FILTERS_PARAM_NAME = "folderFilters";
    private static final String EXTENSIONS_FILTERS_PARAM_NAME = "fileExtensionsFilters";
    protected static final String PATH = "sourcePath";
    public static final Property<ReloadStatus> INDEXING_OK = new SimpleObjectProperty<>(ReloadStatus.OK);
    protected final Map<String, ReloadStatus> indexedFiles = new HashMap<>();
    protected F eventFormat;
    protected P parsingProfile;
    protected Path filePath;
    protected ZoneId zoneId;
    protected String encoding;
    protected Index index;
    protected String[] folderFilters;
    protected String[] fileExtensionsFilters;
    protected FileSystemBrowser fileBrowser;
    private Path workspaceRootPath;

    protected IndexBackedFileAdapter(String filePath,
                                     ZoneId zoneId,
                                     String encoding,
                                     P parsingProfile,
                                     String[] folderFilters,
                                     String[] fileExtensionsFilters) {
        this.filePath = Path.of(filePath);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.parsingProfile = parsingProfile;
        this.fileExtensionsFilters = fileExtensionsFilters;
        this.folderFilters = folderFilters;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(ts -> ts.getBinding().getPath()).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }


    private final BinaryPrefixFormatter binaryPrefixFormatter = new BinaryPrefixFormatter("###,###.## ");

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> root = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        try {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toInternalPath("/"), root);
            for (var fsEntry : fileBrowser.listEntries(folderFilters, fileExtensionsFilters)) {

                String fileName = fsEntry.getPath().getFileName().toString() + " (" + binaryPrefixFormatter.format(fsEntry.getSize()) + "B)";
                var attachTo = root;
                if (fsEntry.getPath().getParent() != null) {
                    attachTo = nodeDict.get(fsEntry.getPath().getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, fsEntry.getPath().getParent(), root);
                    }
                }
                FilterableTreeItem<SourceBinding> fileBranch = new FilterableTreeItem<>(
                        new TimeSeriesBinding.Builder()
                                .withLabel(fileName)
                                .withPath(getId() + "/" + fsEntry.getPath().toString())
                                .withAdapter(this)
                                .build());

                attachLeafNode(fsEntry, fileBranch);
                attachTo.getInternalChildren().add(fileBranch);
            }
        } catch (Exception ex) {
            throw new DataAdapterException("Error building treeview for adapter " + this.getAdapterInfo().getName() + ": " + ex.getMessage(), ex);
        }
        return root;
    }

    protected abstract void attachLeafNode(FileSystemBrowser.FileSystemEntry fsEntry,
                                           FilterableTreeItem<SourceBinding> fileBranch) throws DataAdapterException;

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
                        new TimeSeriesBinding.Builder()
                                .withLabel(current.getFileName().toString())
                                .withPath(getId() + "/" + path)
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
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path,
                                                                              Instant begin,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<Double>> seriesInfo,
                                                                              boolean bypassCache) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
            for (TimeSeriesInfo<Double> info : seriesInfo) {
                series.put(info, new DoubleTimeSeriesProcessor());
            }
            var nbHits = index.search(
                    begin.toEpochMilli(),
                    end.toEpochMilli(),
                    series,
                    zoneId,
                    bypassCache);
            logger.debug(() -> "Retrieved " + nbHits + " hits");
            return series;
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching data from " + path, e);
        }
    }

    @Override
    public void reload(String path, List<TimeSeriesInfo<Double>> seriesInfo, ReloadPolicy reloadPolicy, DoubleProperty progress, Property<ReloadStatus> reloadStatus) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), reloadPolicy);
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching data from " + path, e);
        }
    }

    private synchronized void ensureIndexed(Set<SourceBinding<Double>> bindings, ReloadPolicy reloadPolicy) throws IOException {
        if (reloadPolicy == ReloadPolicy.ALL) {
            bindings.stream().map(SourceBinding::getPath).forEach(indexedFiles::remove);
        }
        final LongProperty charRead = new SimpleLongProperty(0);
        for (var binding : bindings) {
            String path = binding.getPath();
            indexedFiles.computeIfAbsent(path, CheckedLambdas.wrap(p -> {
                try (var inputStream = fileBrowser.getData(p.replace(getId() + "/", ""))) {
                    index.add(p,
                            inputStream,
                            true,
                            eventFormat,
                            this::mapEventToDocument,
                            charRead,
                            INDEXING_OK);
                    return ReloadStatus.OK;
                }
            }));
        }
    }

    protected abstract Document mapEventToDocument(Document doc, ParsedEvent event);

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(filePath);
            this.index = Indexes.NUM_SERIES.acquire();
            this.eventFormat = supplyEventFormat(parsingProfile, zoneId, Charset.forName(encoding));
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

    protected abstract F supplyEventFormat(P parsingProfile, ZoneId zoneId, Charset charset);

    @Override
    public void close() {
        try {
            Indexes.NUM_SERIES.release();
        } catch (Exception e) {
            logger.error("An error occurred while releasing index " + Indexes.NUM_SERIES.name() + ": " + e.getMessage());
            logger.debug("Stack Trace:", e);
        }
        IOUtils.close(fileBrowser);
        super.close();
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PATH, filePath.toString());
        params.put(FOLDER_FILTERS_PARAM_NAME, GSON.toJson(folderFilters));
        params.put(EXTENSIONS_FILTERS_PARAM_NAME, GSON.toJson(fileExtensionsFilters));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params, LoadingContext context) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        this.zoneId = mapParameter(params, ZONE_ID, ZoneId::of);
        this.filePath = mapParameter(params, PATH, Path::of);
        this.encoding = mapParameter(params, ENCODING);
        this.folderFilters = mapParameter(params, FOLDER_FILTERS_PARAM_NAME, p -> GSON.fromJson(p, String[].class));
        this.fileExtensionsFilters = mapParameter(params, EXTENSIONS_FILTERS_PARAM_NAME, p -> GSON.fromJson(p, String[].class));
        this.workspaceRootPath = context.savedWorkspacePath() != null ? context.savedWorkspacePath().getParent() : this.filePath.getRoot();
        if (workspaceRootPath != null) {
            this.filePath = workspaceRootPath.resolve(filePath);
        }
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public String getSourceName() {
        return "[" + getAdapterInfo().getName() + "] " +
                (filePath != null ? filePath.getFileName() : "???") +
                " (" +
                (zoneId != null ? zoneId : "???") +
                ")";
    }
}
