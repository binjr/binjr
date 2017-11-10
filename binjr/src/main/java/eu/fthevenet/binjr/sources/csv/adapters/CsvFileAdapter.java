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
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.exceptions.FetchingDataFromAdapterException;
import eu.fthevenet.binjr.data.exceptions.InvalidAdapterParameterException;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeriesProcessor;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import eu.fthevenet.util.logging.Profiler;
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

public class CsvFileAdapter extends DataAdapter<Double, CsvDecoder<Double>> {
    private static final Logger logger = LogManager.getLogger(CsvFileAdapter.class);
    private String dateTimePattern;
    private Path csvPath;
    private ZoneId zoneId;
    private Character delimiter;
    private String encoding;
    private CsvDecoder<Double> decoder;
    private SortedMap<Long, String> sortedDataStore;
    //  private String headerLine;

    public CsvFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", "yyyy-MM-dd HH:mm:ss", ',');
    }

    public CsvFileAdapter(String csvPath, ZoneId zoneId) throws DataAdapterException {
        this(csvPath, zoneId, "utf-8", "yyyy-MM-dd HH:mm:ss", ',');
    }

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
            for (String header : getDecoder().getDataColumnHeaders(in)) {
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
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> getDecodedData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    @Override
    public InputStream getRawData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException {
        throw new UnsupportedOperationException("Recovery of raw data is not supported for this data source.");
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        try {
//            byte[] cr = "\n".getBytes(encoding);
//            out.write(headerLine.getBytes(encoding));
//            out.write(cr);
//            Collection<String> data = getDataStore().subMap(begin.getEpochSecond(), end.getEpochSecond()).values();
//            for (String s : data) {
//                out.write(s.getBytes(encoding));
//                out.write(cr);
//            }
//            return new ByteArrayInputStream(out.toByteArray());
//        } catch (IOException e) {
//            throw new FetchingDataFromAdapterException(e);
//        }
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
            throw new IllegalStateException("The CsvDecoder has not been properly initialized");
        }
        return decoder;
    }


    @Override
    public String getSourceName() {
        return new StringBuilder("[CSV] ")
                .append(csvPath != null ? csvPath.getFileName() : "???")
                .append(" (")
                .append(zoneId != null ? zoneId : "???")
                .append(")").toString();
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
    public void initialize(Map<String, String> params) throws DataAdapterException {
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
            if (s == null || s.isEmpty()) {
                throw new InvalidAdapterParameterException("Parameter 'delimiter' is missing for adapter " + this.getSourceName());
            }
            return s.charAt(0);
        });
        encoding = validateParameterNullity(params, "encoding");
        dateTimePattern = validateParameterNullity(params, "dateTimePattern");
        this.csvPath = Paths.get(path);

        // setup the CSV decoder
        this.decoder = new CsvDecoder<>(getEncoding(), delimiter,
                DoubleTimeSeriesProcessor::new,
                s -> {
                    try {
                        Double val = Double.parseDouble(s);
                        return val.isNaN() ? 0 : val;
                    } catch (NumberFormatException e) {
                        logger.debug("Cannot format value as a number", e);
                        return Double.NaN;
                    }
                },
                s -> ZonedDateTime.parse(s, DateTimeFormatter.ofPattern(dateTimePattern).withZone(getTimeZoneId())));
    }

    @Override
    public boolean ping() {
        return Files.exists(csvPath);
    }

    @Override
    public void close() throws Exception {

    }

    protected SortedMap<Long, String> getDataStore() throws DataAdapterException {
        if (sortedDataStore == null) {
            try (InputStream in = Files.newInputStream(csvPath)) {
                this.sortedDataStore = buildSortedDataStore(in);
            } catch (IOException e) {
                throw new DataAdapterException(e);
            }
        }
        return sortedDataStore;
    }

    private SortedMap<Long, String> buildSortedDataStore(InputStream in) throws IOException, DataAdapterException {
        SortedMap<Long, String> dataStore = new ConcurrentSkipListMap<>();
        try (Profiler ignored = Profiler.start("Building seek index for csv file", logger::trace)) {


//            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
//                //ignore first line (headers)
//                br.readLine();
//                for (String line = br.readLine(); line != null; line = br.readLine()) {
//                    String[] data = line.split(delimiter.toString());
//                    if (data.length < 2) {
//                        throw new DecodingDataFromAdapterException("Not enough columns in csv to plot a time series");
//                    }
//                    ZonedDateTime timeStamp = getDecoder().getDateParser().apply(data[0].replace("\"", ""));
//                    dataStore.put(timeStamp.toEpochSecond(), line);
//                }
//            }
        }
        return dataStore;
    }

}
