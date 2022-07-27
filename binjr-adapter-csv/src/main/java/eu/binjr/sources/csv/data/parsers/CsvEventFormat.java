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
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CsvEventFormat implements EventFormat {
    private static final Logger logger = Logger.create(CsvEventFormat.class);
    private final ParsingProfile profile;
    private final ZoneId zoneId;
    private final String delimiter;
    private final Charset encoding;
    private final int timestampPosition;


    public CsvEventFormat(ParsingProfile profile, ZoneId zoneId, Charset encoding, String delimiter, int timestampPosition) {
        this.profile = profile;
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.delimiter = delimiter;
        this.timestampPosition = timestampPosition;
    }

    @Override
    public ParsingProfile getProfile() {
        return profile;
    }

    @Override
    public EventParser parse(InputStream ias) {
        return new CsvEventParser(this, ias);
    }

    @Override
    public Optional<ParsedEvent> parse(String text) {
        return parse(-1, text);
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public int getTimestampPosition() {
        return timestampPosition;
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
                    .setDelimiter(delimiter)
                    .build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            CSVRecord record = records.iterator().next();
            if (record == null) {
                throw new DecodingDataFromAdapterException("CSV stream does not contains column header");
            }
            List<String> headerNames = new ArrayList<>();
            for (int i = 0; i < record.size(); i++) {
                if (i != getTimestampPosition()) { // skip timestamp column
                    headerNames.add(record.get(i));
                }
            }
            return headerNames;
        }
    }

}
