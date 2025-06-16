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

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import org.jsfr.json.JacksonParser;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.compiler.JsonPathCompiler;

import org.jsfr.json.provider.JacksonProvider;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class JsonEventParser implements EventParser {
    private static final Logger logger = Logger.create(JsonEventParser.class);
    private final AtomicLong sequence;
    private final JsonEventFormat format;
    private final JsonEventIterator eventIterator;
    private final LongProperty progress = new SimpleLongProperty(0);
    private final Iterator<Object> jsonIterator;

    JsonEventParser(JsonEventFormat format, InputStream ias) {
        this.sequence = new AtomicLong(0);
        this.format = format;

        var jsonSurfer = new JsonSurfer(JacksonParser.INSTANCE, JacksonProvider.INSTANCE);
        jsonSurfer.setParserCharset(format.getEncoding());
        this.jsonIterator = jsonSurfer.iterator(ias,
                JsonPathCompiler.compile(format.getProfile().getJsonDefinition().objectPath()));
        this.eventIterator = new JsonEventIterator();
    }

    @Override
    public void close() throws IOException {
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
            ObjectNode jsonObject = (ObjectNode) jsonIterator.next();
            if (jsonObject == null) {
                return null;
            }
            String dateString = jsonObject.at(format.getProfile().getJsonDefinition().timeStampsPointer()).toString();
            ZonedDateTime timestamp = format.getProfile().parseDateTime(dateString, format.getZoneId());
            if (timestamp == null) {
                if (++timestampParsingErrors >= MAX_TIMESTAMP_ERRORS) {
                    // If number of parsing errors is above max threshold,
                    // interrupt processing to avoid stack overflow
                    throw new UnsupportedOperationException("Too many time stamp parsing errors (>" +
                            MAX_TIMESTAMP_ERRORS + "): Aborting processing.");
                }
                var errMessage = "Failed to parse \"" + StringUtils.ellipsize(dateString, 100) +
                        "\" as a valid time stamp at \"" + format.getProfile().getJsonDefinition().timeStampsPointer() +
                        "\".";
                if (format.getProfile().isContinueOnTimestampParsingFailure()) {
                    logger.warn(errMessage);
                    // Ignore the current json node and jump to the next
                    return this.next();
                } else {
                    throw new UnsupportedOperationException(errMessage);
                }
            }

            Map<String, Number> values = new LinkedHashMap<>(format.getProfile().getJsonDefinition().series().size());
            for (var series : format.getProfile().getJsonDefinition().series()) {
                values.put(series.path(), jsonObject.at(series.path()).asDouble());
            }
            return ParsedEvent.withNumberFields(sequence.incrementAndGet(), timestamp, " ", values);
        }

        @Override
        public boolean hasNext() {
            return jsonIterator.hasNext();
        }
    }

}

