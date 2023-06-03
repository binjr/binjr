/*
 *    Copyright 2023 Frederic Thevenet
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

package eu.binjr.sources.jfr.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


public class JfrEventParser implements EventParser {
    private static final Logger logger = Logger.create(JfrEventParser.class);


    // private final BufferedReader reader;
    private final AtomicLong sequence;
    private final JfrEventFormat format;
    private final JfrEventIterator eventIterator;
    private final RecordingFile recordingFile;
    private final LongProperty progress = new SimpleLongProperty(0);

    JfrEventParser(JfrEventFormat format, Path ias) {
        this.sequence = new AtomicLong(0);
        this.format = format;
        try {
            this.recordingFile = new RecordingFile(ias);
            this.eventIterator = new JfrEventIterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        recordingFile.close();
    }

    @Override
    public LongProperty progressIndicator() {
        return progress;
    }

    @Override
    public Iterator<ParsedEvent> iterator() {
        return eventIterator;
    }

    public class JfrEventIterator implements Iterator<ParsedEvent> {

        @Override
        public ParsedEvent next() {
            ParsedEvent event = null;
            while (event == null && recordingFile.hasMoreEvents()) {
                event = readNextJrfEvent();
            }
            return event;
        }

        private ParsedEvent readNextJrfEvent() {
            RecordedEvent jfrEvent = null;
            try {
                jfrEvent = recordingFile.readEvent();
                ZonedDateTime timestamp = ZonedDateTime.ofInstant(jfrEvent.getStartTime(), format.getZoneId());
                Map<String, Number> numFields = new LinkedHashMap<>();
                var categories = String.join("/", jfrEvent.getEventType().getCategoryNames()) +
                        "/" + jfrEvent.getEventType().getLabel();
                for (var field : jfrEvent.getFields()) {
                    if (JfrEventFormat.includeField(field)) {
                        numFields.put(field.getLabel(), jfrEvent.getValue(field.getName()));
                    }
                }
                if (jfrEvent.getEventType().getName().equals("jdk.YoungGenerationConfiguration")) {
                    var foo = jfrEvent.toString();
                }

                return new ParsedEvent(
                        sequence.incrementAndGet(),
                        timestamp,
                        "",//jfrEvent.toString(),
                        Map.of(JfrEventFormat.CATEGORIES, categories),
                        numFields);
            } catch (Exception e) {
                logger.error("Error parsing JFR event [" + (jfrEvent == null ? "unknown" : jfrEvent.getEventType().getName()) + "]: " + e.getMessage());
                logger.debug("Stack trace", e);
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return recordingFile.hasMoreEvents();
        }


    }

}



