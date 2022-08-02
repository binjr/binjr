/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.exceptions.DecodingDataFromAdapterException;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class CsvEventFormat implements EventFormat<Double> {
    private static final Logger logger = Logger.create(CsvEventFormat.class);
    private final CsvParsingProfile profile;
    private final ZoneId zoneId;
    private final Charset encoding;

    public CsvEventFormat(CsvParsingProfile profile, ZoneId zoneId, Charset encoding) {
        this.profile = profile;
        this.zoneId = zoneId;
        this.encoding = encoding;
    }

    @Override
    public CsvParsingProfile getProfile() {
        return profile;
    }

    @Override
    public EventParser<Double> parse(InputStream ias) {
        return new CsvEventParser(this, ias);
    }

    @Override
    public Optional<ParsedEvent<Double>> parse(String text) {
        return parse(-1, text);
    }

    @Override
    public Optional<ParsedEvent<Double>> parse(long lineNumber, String text) {
        var m = getProfile().getParsingRegex().matcher(text);
        var timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, getZoneId());
        final Map<String, Double> sections = new HashMap<>();
        if (m.find()) {
            for (Map.Entry<NamedCaptureGroup, String> entry : getProfile().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed = m.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                        timestamp = timestamp.with(temporalGroup.getMapping(), Long.parseLong(parsed));
                    } else {
                        sections.put(captureGroup.name(), parseDouble(parsed));
                    }
                }
            }
            return Optional.of(new ParsedEvent<>(lineNumber, timestamp, text, sections));
        }
        return Optional.empty();
    }

    private double parseDouble(String value) {
        try {
            return getProfile().getNumberFormat().parse(value).doubleValue();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
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
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setAllowMissingColumnNames(false)
                    .setDelimiter(getProfile().getDelimiter())
                    .build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            CSVRecord record = records.iterator().next();
            if (record == null) {
                throw new DecodingDataFromAdapterException("CSV stream does not contains column header");
            }
            List<String> headerNames = new ArrayList<>();
            for (int i = 0; i < record.size(); i++) {
                //   if (i != getProfile().getTimestampColumn()) { // skip timestamp column
                String name = getProfile().isReadColumnNames() ? record.get(i) : "Column " + (i + 1);
                headerNames.add(name);
                // }
            }
            return headerNames;
        }
    }

}
