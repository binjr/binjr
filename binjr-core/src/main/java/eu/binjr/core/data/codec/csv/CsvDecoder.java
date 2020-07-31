/*
 *    Copyright 2019-2020 Frederic Thevenet
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

package eu.binjr.core.data.codec.csv;

import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.codec.Decoder;
import eu.binjr.core.data.exceptions.DecodingDataFromAdapterException;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessorFactory;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.scene.chart.XYChart;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
import java.util.function.Consumer;

/**
 * This class provides an implementation of a {@link Decoder} that decode data from a CSV formatted text stream into a {@link TimeSeriesProcessor}.
 *
 * @author Frederic Thevenet
 */
public class CsvDecoder implements Decoder<Double> {
    private final String encoding;
    private final char delimiter;
    private final CheckedFunction<String, Double, DecodingDataFromAdapterException> numberParser;
    private final CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> dateParser;
    private final TimeSeriesProcessorFactory<Double> timeSeriesFactory;
    private static final Logger logger = Logger.create(CsvDecoder.class);

    /**
     * Initializes a new instance of the {@link CsvDecoder} class using the default number parsing function.
     *
     * @param encoding          the encoding used in the CSV stream
     * @param delimiter         the character to separate columns in the CSV stream
     * @param timeSeriesFactory the factory used to fromUrl new {@link TimeSeriesProcessor} instances.
     * @param dateParser        the function used to parse dates from the CSV stream
     */
    public CsvDecoder(String encoding,
                      char delimiter,
                      TimeSeriesProcessorFactory<Double> timeSeriesFactory,
                      CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> dateParser) {
        this(encoding, delimiter, timeSeriesFactory, null, dateParser);
    }

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
                      TimeSeriesProcessorFactory<Double> timeSeriesFactory,
                      CheckedFunction<String, Double, DecodingDataFromAdapterException> numberParser,
                      CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> dateParser) {
        this.encoding = encoding;
        this.delimiter = delimiter;
        this.timeSeriesFactory = timeSeriesFactory;
        if (numberParser == null) {
            numberParser = s -> {
                if (s == null || s.isBlank() || s.equalsIgnoreCase("null")) {
                    return Double.NaN;
                }
                return Double.parseDouble(s);
            };
        }
        this.numberParser = numberParser;
        this.dateParser = dateParser;
    }

    /**
     * Returns the columns headers of the CSV file.
     *
     * @param in an input stream for the CSV file.
     * @return the columns headers of the CSV file.
     * @throws IOException                      in the event of an I/O error.
     * @throws DecodingDataFromAdapterException if an error occurred while decoding the CSV file.
     */
    public List<String> getDataColumnHeaders(InputStream in) throws IOException, DecodingDataFromAdapterException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .withAllowMissingColumnNames(false)
                    .withDelimiter(delimiter);
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            return this.parseColumnHeaders(records.iterator().next());
        }
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> decode(InputStream in, List<TimeSeriesInfo<Double>> seriesInfo) throws IOException, DecodingDataFromAdapterException {
        try (Profiler ignored = Profiler.start("Building time series from csv data", logger::perf)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
                CSVFormat csvFormat = CSVFormat.DEFAULT
                        .withAllowMissingColumnNames(false)
                        .withFirstRecordAsHeader()
                        .withSkipHeaderRecord()
                        .withDelimiter(delimiter);
                Iterable<CSVRecord> records = csvFormat.parse(reader);
                Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                for (CSVRecord csvRecord : records) {
                    nbpoints.incrementAndGet();
                    ZonedDateTime timeStamp = dateParser.apply(csvRecord.get(0));
                    for (TimeSeriesInfo<Double> info : seriesInfo) {
                        Double val = numberParser.apply(csvRecord.get(info.getBinding().getLabel()));
                        XYChart.Data<ZonedDateTime, Double> point = new XYChart.Data<>(timeStamp, val);
                        TimeSeriesProcessor<Double> l = series.computeIfAbsent(info, k -> timeSeriesFactory.create());
                        l.addSample(point);
                    }
                }
                logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)", seriesInfo.size(), nbpoints.get(), seriesInfo.size() * nbpoints.get()));
                return series;
            }
        }
    }

    /**
     * Decodes data from the provided stream and invoke the provided {@link Consumer} for each decoded record.
     *
     * @param in          the {@link InputStream} for the CSV file
     * @param headers     a list of the headers to keep from decoded records
     * @param mapToResult the function to invoke for reach decoded record
     * @throws IOException                      in the event of an I/O error.
     * @throws DecodingDataFromAdapterException if an error occurred while decoding the CSV file.
     */
    public void decode(InputStream in, List<String> headers, Consumer<DataSample> mapToResult) throws IOException, DecodingDataFromAdapterException {
        try (Profiler ignored = Profiler.start("Building time series from csv data", logger::perf)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {
                CSVFormat csvFormat = CSVFormat.DEFAULT
                        .withAllowMissingColumnNames(false)
                        .withFirstRecordAsHeader()
                        .withSkipHeaderRecord()
                        .withDelimiter(delimiter);
                Iterable<CSVRecord> records = csvFormat.parse(reader);
                for (CSVRecord csvRecord : records) {
                    ZonedDateTime timeStamp = dateParser.apply(csvRecord.get(0));
                    DataSample tRecord = new DataSample(timeStamp);
                    for (int i = 1; i < csvRecord.size(); i++) {
                        tRecord.getCells().put(Integer.toString(i), numberParser.apply(csvRecord.get(i)));
                    }
                    mapToResult.accept(tRecord);
                }
            }
        }
    }

    /**
     * Returns the encoding for the CSV file.
     *
     * @return the encoding for the CSV file.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns the delimiter character used in the CSV file.
     *
     * @return the delimiter character used in the CSV file.
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Returns the function used to parse numeric fields.
     *
     * @return the function used to parse numeric fields.
     */
    public CheckedFunction<String, Double, DecodingDataFromAdapterException> getNumberParser() {
        return numberParser;
    }

    /**
     * Returns the function used to parse date fields.
     *
     * @return the function used to parse date fields.
     */
    public CheckedFunction<String, ZonedDateTime, DecodingDataFromAdapterException> getDateParser() {
        return dateParser;
    }

    private List<String> parseColumnHeaders(CSVRecord record) throws IOException, DecodingDataFromAdapterException {
        try (Profiler ignored = Profiler.start("Getting hearders from csv data", logger::perf)) {
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

    private TimeSeriesInfo<Double> getBindingFromName(List<TimeSeriesInfo<Double>> seriesInfo, String seriesName) {
        if (seriesInfo != null) {
            for (TimeSeriesInfo<Double> b : seriesInfo) {
                if (b.getBinding().getLabel().equalsIgnoreCase(seriesName)) {
                    return b;
                }
            }
        }
        return null;
    }
}
