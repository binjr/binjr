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

package eu.fthevenet.binjr.data.codec;

import eu.fthevenet.binjr.data.exceptions.DecodingDataFromAdapterException;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessorFactory;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.util.function.CheckedFunction;
import eu.fthevenet.util.logging.Profiler;
import javafx.scene.chart.XYChart;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides an implementation of a {@link Decoder} that decode data from a CSV formatted text stream into a {@link TimeSeriesProcessor}.
 *
 * @author Frederic Thevenet
 */
public class CsvDecoder<T extends Number> implements Decoder<T> {
    private final String encoding;
    private final char delimiter;
    private final CheckedFunction<String, T, DecodingDataFromAdapterException> numberParser;
    private final CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> dateParser;
    private final TimeSeriesProcessorFactory<T> timeSeriesFactory;
    private static final Logger logger = LogManager.getLogger(CsvDecoder.class);

    /**
     * Initializes a new instance of the {@link CsvDecoder} class.
     *
     * @param encoding          the encoding used in the CSV stream
     * @param delimiter         the character to separate columns in the CSV stream
     * @param timeSeriesFactory the factory used to fromUrl new {@link TimeSeriesProcessor} instances.
     * @param numberParser      the function used to parse numbers from the CSV stream
     * @param dateParser        the function used to parse dates from the CSV stream
     */
    public CsvDecoder(String encoding,
                      char delimiter,
                      TimeSeriesProcessorFactory<T> timeSeriesFactory,
                      CheckedFunction<String, T, DecodingDataFromAdapterException> numberParser,
                      CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> dateParser) {
        this.encoding = encoding;
        this.delimiter = delimiter;
        this.timeSeriesFactory = timeSeriesFactory;
        this.numberParser = numberParser;
        this.dateParser = dateParser;
    }

    /**
     * @param in
     * @return
     * @throws IOException
     * @throws DecodingDataFromAdapterException
     */
    public List<String> getDataColumnHeaders(InputStream in) throws IOException, DecodingDataFromAdapterException {
        try (Profiler ignored = Profiler.start("Getting hearders from csv data", logger::trace)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
                CSVFormat csvFormat = CSVFormat.DEFAULT
                        .withAllowMissingColumnNames(false)
                        .withDelimiter(delimiter);
                Iterable<CSVRecord> records = csvFormat.parse(reader);
                CSVRecord record = records.iterator().next();
                if (record == null) {
                    throw new DecodingDataFromAdapterException("CSV stream does not contains column header");
                }
                List<String> headerNames = new ArrayList<>();
                for (int i = 1; i < record.size(); i++) {
                    headerNames.add(record.get(i));
                }
                return headerNames;
            }
        }
    }

    @Override
    public Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> decode(InputStream in, List<TimeSeriesInfo<T>> seriesInfo) throws IOException, DecodingDataFromAdapterException {
        try (Profiler ignored = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
                CSVFormat csvFormat = CSVFormat.DEFAULT
                        .withAllowMissingColumnNames(false)
                        .withFirstRecordAsHeader()
                        .withSkipHeaderRecord()
                        .withDelimiter(delimiter);

                Iterable<CSVRecord> records = csvFormat.parse(reader);
                Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> series = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                for (CSVRecord record : records) {
                    nbpoints.incrementAndGet();
                    ZonedDateTime timeStamp = dateParser.apply(record.get(0));
                    for (TimeSeriesInfo<T> info : seriesInfo) {
                        T val = numberParser.apply(record.get(info.getBinding().getLabel()));
                        XYChart.Data<ZonedDateTime, T> point = new XYChart.Data<>(timeStamp, val);
                        TimeSeriesProcessor<T> l = series.computeIfAbsent(info, k -> timeSeriesFactory.create());
                        l.addSample(point);
                    }
                }
                logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)", seriesInfo.size(), nbpoints.get(), seriesInfo.size() * nbpoints.get()));
                return series;
            }

//            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
//                String header = br.readLine();
//                if (header == null || header.isEmpty()) {
//                    throw new DecodingDataFromAdapterException("CSV File is empty!");
//                }
//                String[] seriesNames = header.split(delimiter);
//                final int nbSeries = seriesNames.length - 1;
//
//                Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> series = new HashMap<>();
//                final AtomicLong nbpoints = new AtomicLong(0);
//                for (String line = br.readLine(); line != null; line = br.readLine()) {
//                    nbpoints.incrementAndGet();
//                    String[] data = line.split(delimiter);
//                    if (data.length < 2) {
//                        throw new DecodingDataFromAdapterException("Not enough columns in csv to plot a time series");
//                    }
//                    ZonedDateTime timeStamp = dateParser.apply(data[0].replace("\"", ""));
//                    for (int i = 1; i < data.length; i++) {
//                        TimeSeriesInfo<T> info = getBindingFromName(seriesInfo, seriesNames[i].replace("\"", ""));
//                        if (info != null) {
//                            T val = numberParser.apply(data[i].replace("\"", ""));
//                            XYChart.Data<ZonedDateTime, T> point = new XYChart.Data<>(timeStamp, val);
//                            TimeSeriesProcessor<T> l = series.computeIfAbsent(info, k -> timeSeriesFactory.create());
//                            l.addSample(point);
//                        }
//                    }
//                }
//                logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)", nbSeries, nbpoints.get(), nbSeries * nbpoints.get()));
//                return series;
//            }
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public CheckedFunction<String, T, DecodingDataFromAdapterException> getNumberParser() {
        return numberParser;
    }

    public CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> getDateParser() {
        return dateParser;
    }

    private TimeSeriesInfo<T> getBindingFromName(List<TimeSeriesInfo<T>> seriesInfo, String seriesName) {
        if (seriesInfo != null) {
            for (TimeSeriesInfo<T> b : seriesInfo) {
                if (b.getBinding().getLabel().equalsIgnoreCase(seriesName)) {
                    return b;
                }
            }
        }
        return null;
    }
}
