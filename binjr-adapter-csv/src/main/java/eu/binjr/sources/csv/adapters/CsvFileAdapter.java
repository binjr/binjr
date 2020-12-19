/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.codec.csv.CsvDecoder;
import eu.binjr.core.data.codec.csv.DataSample;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import javafx.scene.control.TreeItem;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local CSV formatted file.
 *
 * @author Frederic Thevenet
 */
public class CsvFileAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(CsvFileAdapter.class);
    private String dateTimePattern;
    private Path csvPath;
    private ZoneId zoneId;
    private Character delimiter;
    private String encoding;
    private CsvDecoder csvDecoder;
    private ConcurrentNavigableMap<Long, DataSample> sortedDataStore;
    private List<String> headers;

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public CsvFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", "yyyy-MM-dd HH:mm:ss", ',');
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class for the provided file and time zone.
     *
     * @param csvPath the path to the csv file.
     * @param zoneId  the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String csvPath, ZoneId zoneId) throws DataAdapterException {
        this(csvPath, zoneId, "utf-8", "yyyy-MM-dd HH:mm:ss", ',');
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
    public CsvFileAdapter(String csvPath, ZoneId zoneId, String encoding, String dateTimePattern, char delimiter) throws DataAdapterException {
        super();
        this.csvPath = Paths.get(csvPath);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.dateTimePattern = dateTimePattern;
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
                        .withPath(columnIndex)
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
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        return TimeRange.of(getDataStore().get(getDataStore().firstKey()).getTimeStamp(),
                getDataStore().get(getDataStore().lastKey()).getTimeStamp());
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
        Map<String, List<TimeSeriesInfo<Double>>> rDict = new HashMap<>();
        for (TimeSeriesInfo<Double> info : seriesInfo) {
            rDict.computeIfAbsent(info.getBinding().getLabel(), s -> new ArrayList<>()).add(info);
            series.put(info, new DoubleTimeSeriesProcessor());
        }
        Long fromKey = Objects.requireNonNullElse(getDataStore().floorKey(begin.toEpochMilli()), begin.toEpochMilli());
        Long toKey = Objects.requireNonNullElse(getDataStore().ceilingKey(end.toEpochMilli()), end.toEpochMilli());
        for (DataSample sample : getDataStore().subMap(fromKey, true, toKey, true).values()) {
            for (String n : sample.getCells().keySet()) {
                List<TimeSeriesInfo<Double>> timeSeriesInfoList = rDict.get(n);
                if (timeSeriesInfoList != null) {
                    for (var tsInfo : timeSeriesInfoList) {
                        series.get(tsInfo).addSample(sample.getTimeStamp(), sample.getCells().get(n));
                    }
                }
            }
        }
        return series;
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
        params.put("dateTimePattern", dateTimePattern);
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
        dateTimePattern = validateParameterNullity(params, "dateTimePattern");
        this.csvPath = Paths.get(path);
        this.csvDecoder = decoderFactory(zoneId, encoding, dateTimePattern, delimiter);
    }

    @Override
    public void close() {
        if (sortedDataStore != null) {
            sortedDataStore.clear();
        }
        super.close();
    }

    private ConcurrentNavigableMap<Long, DataSample> getDataStore() throws DataAdapterException {
        if (sortedDataStore == null) {
            try (InputStream in = Files.newInputStream(csvPath)) {
                this.sortedDataStore = buildSortedDataStore(in);
            } catch (IOException e) {
                throw new DataAdapterException(e);
            }
        }
        return sortedDataStore;
    }

    private CsvDecoder decoderFactory(ZoneId zoneId, String encoding, String dateTimePattern, char delimiter) {
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
                s -> ZonedDateTime.parse(s, DateTimeFormatter.ofPattern(dateTimePattern).withZone(zoneId)));
    }

    private ConcurrentNavigableMap<Long, DataSample> buildSortedDataStore(InputStream in) throws IOException, DataAdapterException {
        ConcurrentNavigableMap<Long, DataSample> dataStore = new ConcurrentSkipListMap<>();

        try (Profiler ignored = Profiler.start("Building seekable datastore for csv file", logger::perf)) {
            csvDecoder.decode(in, headers, sample -> dataStore.put(sample.getTimeStamp().toInstant().toEpochMilli(), sample));
        }
        return dataStore;
    }
}
