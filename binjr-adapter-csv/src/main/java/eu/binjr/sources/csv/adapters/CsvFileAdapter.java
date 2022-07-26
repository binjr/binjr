/*
 *    Copyright 2017-2022 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;

import com.google.gson.Gson;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.codec.csv.CsvDecoder;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.indexes.IndexingStatus;
import eu.binjr.core.data.indexes.NumSeriesIndex;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.sources.csv.data.parsers.CsvEventFormat;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local CSV formatted file.
 *
 * @author Frederic Thevenet
 */
public class CsvFileAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(CsvFileAdapter.class);
    private static final Gson gson = new Gson();
    private static final Property<IndexingStatus> INDEXING_OK = new SimpleObjectProperty<>(IndexingStatus.OK);
    private final EventFormat parser;
    private ParsingProfile dateTimePattern;
    private Path csvPath;
    private ZoneId zoneId;
    private Character delimiter;
    private String encoding;
    private CsvDecoder csvDecoder;
    private final Map<String, IndexingStatus> indexedFiles = new HashMap<>();
    private NumSeriesIndex index;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private List<String> headers;
    private long sequence = 0;

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public CsvFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", BuiltInCsvTimestampParsingProfile.ISO, ',');
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class for the provided file and time zone.
     *
     * @param csvPath the path to the csv file.
     * @param zoneId  the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String csvPath, ZoneId zoneId) throws DataAdapterException {
        this(csvPath, zoneId, "utf-8", BuiltInCsvTimestampParsingProfile.ISO, ',');
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with the provided parameters.
     *
     * @param csvPath         the path to the csv file.
     * @param zoneId          the time zone to used.
     * @param encoding        the encoding for the csv file.
     * @param dateTimePattern a pattern to decode time stamps.
     * @param delimiter       the character used by the csv file to separate cells in csv records.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String csvPath, ZoneId zoneId, String encoding, ParsingProfile dateTimePattern, char delimiter)
            throws DataAdapterException {
        super();
        this.csvPath = Paths.get(csvPath);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.dateTimePattern = dateTimePattern;
        this.parser = new CsvEventFormat(dateTimePattern, zoneId, Charset.forName(encoding), String.valueOf(delimiter));
        this.delimiter = delimiter;
        this.csvDecoder = decoderFactory(zoneId, encoding, dateTimePattern, delimiter);
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        try (InputStream in = Files.newInputStream(csvPath)) {
            this.headers = csvDecoder.getDataColumnHeaders(in);
            for (int i = 0; i < headers.size(); i++) {
                String columnIndex = Integer.toString(i + 1);
                String header = headers.get(i).isBlank() ? "Column #" + columnIndex : headers.get(i);
                var b = new TimeSeriesBinding.Builder()
                        .withLabel(columnIndex)
                        .withPath(getId() + "/" + csvPath.toString())
                        .withLegend(header)
                        .withParent(tree.getValue())
                        .withAdapter(this)
                        .build();
                tree.getInternalChildren().add(new TreeItem<>(b));
            }
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
        return tree;
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


    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path,
                                                                              Instant begin,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<Double>> seriesInfo,
                                                                              boolean bypassCache) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), bypassCache ? ReloadPolicy.ALL : ReloadPolicy.UNLOADED);
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
    public String getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public String getSourceName() {
        return new StringBuilder("[CSV] ")
                .append(csvPath != null ? csvPath.getFileName() : "???")
                .append(" (")
                .append(zoneId != null ? zoneId : "???")
                .append(")")
                .toString();
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("zoneId", zoneId.toString());
        params.put("encoding", encoding);
        params.put("delimiter", Character.toString(delimiter));
        params.put("dateTimePattern", gson.toJson(CustomParsingProfile.of(dateTimePattern)));
        params.put("csvPath", csvPath.toString());
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        zoneId = validateParameter(params, "zoneId",
                s -> {
                    if (s == null) {
                        throw new InvalidAdapterParameterException("Parameter zoneId is missing in adapter " + getSourceName());
                    }
                    return ZoneId.of(s);
                });
        String path = validateParameterNullity(params, "csvPath");
        delimiter = validateParameter(params, "delimiter", s -> {
            if (s == null || s.length() != 1) {
                throw new InvalidAdapterParameterException("Parameter 'delimiter' is missing for adapter " + this.getSourceName());
            }
            return s.charAt(0);
        });
        encoding = validateParameterNullity(params, "encoding");
        dateTimePattern = gson.fromJson(validateParameterNullity(params, "dateTimePattern"), CustomParsingProfile.class);
        this.csvPath = Paths.get(path);
        this.csvDecoder = decoderFactory(zoneId, encoding, dateTimePattern, delimiter);
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(csvPath.getParent());
            this.index = (NumSeriesIndex) Indexes.NUM_SERIES.acquire();
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

    private synchronized void ensureIndexed(Set<SourceBinding<Double>> bindings, ReloadPolicy reloadPolicy) throws IOException {
        if (reloadPolicy == ReloadPolicy.ALL) {
            bindings.stream().map(SourceBinding::getPath).forEach(indexedFiles::remove);
        }
        final LongProperty charRead = new SimpleLongProperty(0);
        int i = 0;
        for (var binding : bindings) {
            String path = binding.getPath();
            boolean isLast = true;// FIXME: bindings.size() - 1 == i++;
            indexedFiles.computeIfAbsent(path, CheckedLambdas.wrap(p -> {
                index.add(p,
                        fileBrowser.getData(path.replace(getId() + "/", "")),
                        isLast,
                        parser,
                        charRead,
                        INDEXING_OK);
                return IndexingStatus.OK;
            }));
        }
    }

    private CsvDecoder decoderFactory(ZoneId zoneId, String encoding, ParsingProfile dateTimePattern, char delimiter) {
        return new CsvDecoder(encoding, delimiter,
                DoubleTimeSeriesProcessor::new,
                s -> {
                    try {
                        return Double.parseDouble(s);
                    } catch (NumberFormatException e) {
                        logger.debug(() -> "Cannot format value as a number", e);
                        return Double.NaN;
                    }
                },
                s -> {
                    var p = parser.parse(s);
                    if (p.isPresent()) {
                        return p.get().getTimestamp();
                    }
                    return ZonedDateTime.ofInstant(Instant.ofEpochSecond(sequence++), zoneId);
                });
    }

}
