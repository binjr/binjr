/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.sources.csv.adapters;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.codec.CsvDecoder;
import eu.fthevenet.binjr.data.codec.DataSample;
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.exceptions.FetchingDataFromAdapterException;
import eu.fthevenet.binjr.data.exceptions.InvalidAdapterParameterException;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeriesProcessor;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import eu.fthevenet.util.logging.Profiler;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TreeItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link DataAdapter} implementation used to {@link eu.fthevenet.binjr.data.workspace.Worksheet} instances
 * with  data from a local CSV formatted file.
 */
public class CsvFileAdapter extends DataAdapter<Double, CsvDecoder<Double>> {
    private static final Logger logger = LogManager.getLogger(CsvFileAdapter.class);
    private String dateTimePattern;
    private Path csvPath;
    private ZoneId zoneId;
    private Character delimiter;
    private String encoding;
    private CsvDecoder<Double> decoder;
    private SortedMap<Long, DataSample<Double>> sortedDataStore;
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
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
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
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public CsvFileAdapter(String csvPath, ZoneId zoneId, String encoding, String dateTimePattern, char delimiter) throws DataAdapterException {
        super();
        this.csvPath = Paths.get(csvPath);
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.dateTimePattern = dateTimePattern;
        this.delimiter = delimiter;
    }

    @Override
    public TreeItem<TimeSeriesBinding<Double>> getBindingTree() throws DataAdapterException {
        TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(
                new TimeSeriesBinding<>(
                        "",
                        "/",
                        null,
                        getSourceName(),
                        UnitPrefixes.METRIC,
                        ChartType.STACKED,
                        "-",
                        "/" + getSourceName(), this));
        try (InputStream in = Files.newInputStream(csvPath)) {
            this.headers = getDecoder().getDataColumnHeaders(in);
            for (String header : headers) {
                TimeSeriesBinding<Double> b = new TimeSeriesBinding<>(
                        header,
                        header,
                        null,
                        header,
                        UnitPrefixes.METRIC,
                        ChartType.STACKED,
                        "-",
                        "/" + getSourceName() + "/" + header,
                        this);
                tree.getChildren().add(new TreeItem<>(b));
            }
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
        return tree;
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchDecodedData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
        Map<String, TimeSeriesInfo<Double>> rDict = new HashMap<>();
        for (TimeSeriesInfo<Double> info : seriesInfo) {
            rDict.put(info.getBinding().getLabel(), info);
            series.put(info, new DoubleTimeSeriesProcessor());
        }

        for (DataSample<Double> sample : getDataStore().subMap(begin.getEpochSecond(), end.getEpochSecond()).values()) {
            for (String n : sample.getCells().keySet()) {
                TimeSeriesInfo<Double> i = rDict.get(n);
                if (i != null) {
                    series.get(i).addSample(new XYChart.Data<>(sample.getTimeStamp(), sample.getCells().get(n)));
                }
            }
        }
        return series;
    }

    @Override
    public InputStream fetchRawData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException {
        throw new UnsupportedOperationException("Recovery of raw data is not supported for this data source.");
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
    public CsvDecoder<Double> getDecoder() {
        if (decoder == null) {
            // setup the CSV decoder
            this.decoder = new CsvDecoder<>(getEncoding(), delimiter,
                    DoubleTimeSeriesProcessor::new,
                    s -> {
                        try {
                            Double val = Double.parseDouble(s);
                            return val.isNaN() ? 0 : val;
                        } catch (NumberFormatException e) {
                            logger.debug(() -> "Cannot format value as a number", e);
                            return 0.0;
                        }
                    },
                    s -> ZonedDateTime.parse(s, DateTimeFormatter.ofPattern(dateTimePattern).withZone(getTimeZoneId())));
        }
        return decoder;
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
                        throw new InvalidAdapterParameterException("Parameter zoneId is missing in adpater " + getSourceName());
                    }
                    return ZoneId.of(s);
                });
        String path = validateParameterNullity(params, "csvPath");
        delimiter = validateParameter(params, "delimiter", s -> {
            if (s == null || s.isEmpty() || s.length() > 1) {
                throw new InvalidAdapterParameterException("Parameter 'delimiter' is missing for adapter " + this.getSourceName());
            }
            return s.charAt(0);
        });
        encoding = validateParameterNullity(params, "encoding");
        dateTimePattern = validateParameterNullity(params, "dateTimePattern");
        this.csvPath = Paths.get(path);
    }

    @Override
    public boolean ping() {
        return Files.exists(csvPath);
    }

    @Override
    public void close() {
        if (sortedDataStore != null) {
            sortedDataStore.clear();
        }
        super.close();
    }

    protected SortedMap<Long, DataSample<Double>> getDataStore() throws DataAdapterException {
        if (sortedDataStore == null) {
            try (InputStream in = Files.newInputStream(csvPath)) {
                this.sortedDataStore = buildSortedDataStore(in);
            } catch (IOException e) {
                throw new DataAdapterException(e);
            }
        }
        return sortedDataStore;
    }

    private SortedMap<Long, DataSample<Double>> buildSortedDataStore(InputStream in) throws IOException, DataAdapterException {
        SortedMap<Long, DataSample<Double>> dataStore = new ConcurrentSkipListMap<>();

        try (Profiler ignored = Profiler.start("Building seekable datastore for csv file", logger::trace)) {
            getDecoder().decode(in, headers, sample -> dataStore.put(sample.getTimeStamp().toEpochSecond(), sample));
        }
        return dataStore;
    }

}
