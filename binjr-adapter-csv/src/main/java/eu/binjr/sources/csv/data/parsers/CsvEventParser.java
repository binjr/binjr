/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.sources.csv.data.parsers;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.ParsingEventException;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.sources.csv.adapters.CsvFileAdapter;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class CsvEventParser implements EventParser<Double> {
    private static final Logger logger = Logger.create(CsvEventParser.class);
    private final BufferedReader reader;
    private final AtomicLong sequence;
    private final CsvEventFormat format;
    private final CsvEventIterator eventIterator;
    private final CSVParser csvParser;
    private final LongProperty progress = new SimpleLongProperty(0);

    CsvEventParser(CsvEventFormat format, InputStream ias) {
        this.reader = new BufferedReader(new InputStreamReader(ias, format.getEncoding()));
        this.sequence = new AtomicLong(0);
        this.format = format;
        try {
            var builder = CSVFormat.Builder.create()
                    .setAllowMissingColumnNames(true)
                    .setSkipHeaderRecord(true)
                    .setDelimiter(format.getProfile().getDelimiter());
            if (format.getProfile().isReadColumnNames()) {
                builder.setHeader();
            }
            this.csvParser = builder.build().parse(reader);

            this.eventIterator = new CsvEventIterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }


    @Override
    public LongProperty progressIndicator() {
        return progress;
    }

    @Override
    public Iterator<ParsedEvent<Double>> iterator() {
        return eventIterator;
    }

    public class CsvEventIterator implements Iterator<ParsedEvent<Double>> {

        @Override
        public ParsedEvent<Double> next() {
            var csvRecord = csvParser.iterator().next();
            if (csvRecord == null) {
                return null;
            }
            if (format.getProfile().getTimestampColumn() > csvRecord.size() - 1) {
                throw new UnsupportedOperationException("Cannot extract time stamp in column #" +
                        (format.getProfile().getTimestampColumn() + 1) +
                        ": CSV record only has " + csvRecord.size() + " fields.");
            }
            String dateString = csvRecord.get(format.getProfile().getTimestampColumn());
            ZonedDateTime timestamp = parseDateTime(dateString);
            if (timestamp == null) {
                throw new UnsupportedOperationException("Failed to parse time stamp in column #" +
                        (format.getProfile().getTimestampColumn() + 1));
            }
            Map<String, Double> values = new LinkedHashMap<>(csvRecord.size());
            for (int i = 0; i < csvRecord.size(); i++) {
                if (i != format.getProfile().getTimestampColumn()) { // don't add the timestamp column as an attribute
                    values.put(Integer.toString(i), parseDouble(csvRecord.get(i)));
                }
            }
            return new ParsedEvent<>(sequence.incrementAndGet(), timestamp, " ", values);
        }

        @Override
        public boolean hasNext() {
            return csvParser.iterator().hasNext();
        }


        private ZonedDateTime parseDateTime(String text) {
            var m = format.getProfile().getParsingRegex().matcher(text);
            var timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, format.getZoneId());
            final Map<String, Double> sections = new HashMap<>();
            if (m.find()) {
                for (Map.Entry<NamedCaptureGroup, String> entry : format.getProfile().getCaptureGroups().entrySet()) {
                    var captureGroup = entry.getKey();
                    var parsed = m.group(captureGroup.name());
                    if (parsed != null && !parsed.isBlank()) {
                        if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                            timestamp = timestamp.with(temporalGroup.getMapping(), Long.parseLong(parsed));
                        }
                    }
                }
                return timestamp;
            }
            return null;
        }

        private double parseDouble(String value) {
            try {
                return format.getProfile().getNumberFormat().parse(value).doubleValue();
            } catch (Exception e) {
                return Double.NaN;
            }
        }
    }
}

