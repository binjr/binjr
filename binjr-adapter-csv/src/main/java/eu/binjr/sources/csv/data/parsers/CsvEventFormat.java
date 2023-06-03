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
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.data.exceptions.DecodingDataFromAdapterException;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.*;

public class CsvEventFormat implements EventFormat<InputStream> {
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
    public EventParser parse(InputStream ias) {
        return new CsvEventParser(this, ias);
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
                    .setDelimiter(StringUtils.stringToEscapeSequence(getProfile().getDelimiter()))
                    .build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            CSVRecord record = records.iterator().next();
            if (record == null) {
                throw new DecodingDataFromAdapterException("CSV stream does not contains column header");
            }
            List<String> headerNames = new ArrayList<>();
            for (int i = 0; i < record.size(); i++) {
                String name = getProfile().isReadColumnNames() ? record.get(i) : "Column " + (i + 1);
                headerNames.add(name);
            }
            return headerNames;
        }
    }

}
