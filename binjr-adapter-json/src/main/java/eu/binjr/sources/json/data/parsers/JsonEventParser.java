/*
 *    Copyright 2025 Frederic Thevenet
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

package eu.binjr.sources.json.data.parsers;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class JsonEventParser implements EventParser {
    private static final Logger logger = Logger.create(JsonEventParser.class);
    private final BufferedReader reader;
    private final AtomicLong sequence;
    private final JsonEventFormat format;
    private final JsonEventIterator eventIterator;
    private final LongProperty progress = new SimpleLongProperty(0);

    JsonEventParser(JsonEventFormat format, InputStream ias) {
        this.reader = new BufferedReader(new InputStreamReader(ias, format.getEncoding()));
        this.sequence = new AtomicLong(0);
        this.format = format;
        this.eventIterator = new JsonEventIterator();
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
    public Iterator<ParsedEvent> iterator() {
        return eventIterator;
    }

    public class JsonEventIterator implements Iterator<ParsedEvent> {
        private static final int MAX_TIMESTAMP_ERRORS = 1000;
        private int timestampParsingErrors = 0;

        @Override
        public ParsedEvent next() {
            //TODO
            ZonedDateTime timestamp = ZonedDateTime.of(
                    format.getProfile().getTemporalAnchor().resolve().plusSeconds(sequence.get()),
                    format.getZoneId());
            Map<String, String> values = new LinkedHashMap<>();
            return ParsedEvent.withTextFields(sequence.incrementAndGet(), timestamp, " ", values);
        }

        @Override
        public boolean hasNext() {
            // TODO
            return false;
        }

        private ZonedDateTime parseDateTime(String text) {
            var m = format.getProfile().getParsingRegex().matcher(text);
            ZonedDateTime timestamp = ZonedDateTime.of(format.getProfile().getTemporalAnchor().resolve(), format.getZoneId());
            if (m.find()) {
                for (Map.Entry<NamedCaptureGroup, String> entry : format.getProfile().getCaptureGroups().entrySet()) {
                    var captureGroup = entry.getKey();
                    var parsed = m.group(captureGroup.name());
                    if (parsed != null && !parsed.isBlank()) {
                        if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                            timestamp = timestamp.with(temporalGroup.getMapping(), temporalGroup.parseLong(parsed));
                        }
                    }
                }
                return timestamp;
            }
            return null;
        }
    }

}

