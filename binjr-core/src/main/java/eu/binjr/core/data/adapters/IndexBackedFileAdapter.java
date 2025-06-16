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

import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class IndexBackedFileAdapter<F extends EventFormat<InputStream>, P extends ParsingProfile> extends BaseDataAdapter<Double> implements Reloadable<Double> {
    protected static final String ZONE_ID = "zoneId";
    protected static final String ENCODING = "encoding";
    protected static final String PARSING_PROFILE = "parsingProfile";
    protected static final String PATH = "filePath";
    private static final Logger logger = Logger.create(IndexBackedFileAdapter.class);
    public static final Property<ReloadStatus> INDEXING_OK = new SimpleObjectProperty<>(ReloadStatus.OK);
    protected final Map<String, ReloadStatus> indexedFiles = new HashMap<>();
    protected F parser;
    protected P parsingProfile;
    protected Path filePath;
    protected ZoneId zoneId;
    protected String encoding;
    protected Index index;
    protected FileSystemBrowser fileBrowser;

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(ts -> ts.getBinding().getPath()).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
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
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(filePath.getParent());
            this.index = Indexes.NUM_SERIES.acquire();
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

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
                            parser,
                            this::mapEventToDocument,
                            charRead,
                            INDEXING_OK);
                    return ReloadStatus.OK;
                }
            }));
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

    protected abstract Document mapEventToDocument(Document doc, ParsedEvent event);
}
