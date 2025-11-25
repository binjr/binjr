/*
 *    Copyright 2022-2025 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser;

import eu.binjr.common.text.StringUtils;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LogEventParser implements EventParser {
    public static final int CHAR_READ_PROGRESS_STEP = 10240;
    private final BufferedReader reader;
    private final AtomicLong sequence;
    private final LogEventFormat format;
    private final LogEventIterator logEventIterator;


    private final LongProperty progress = new SimpleLongProperty(0);
    private long charRead = 0;

    LogEventParser(LogEventFormat format, InputStream ias) {
        this.reader = new BufferedReader(new InputStreamReader(ias, format.getEncoding()));
        this.sequence = new AtomicLong(0);
        this.format = format;
        this.logEventIterator = new LogEventIterator();
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
        return logEventIterator;
    }

    public class LogEventIterator implements Iterator<ParsedEvent> {
        private boolean hasNext = true;
        private ParsedEvent buffered;

        @Override
        public ParsedEvent next() {
            // The first call to yieldNextEvent will always yield
            // a null event; it is only there to prime the buffer.
            var event = yieldNextEvent();
            while (event == null && hasNext) {
                // we loop to concatenate lines that cannot be parsed as an event
                // to the text of the last successfully parsed event.
                event = yieldNextEvent();
            }
            return event;
        }

        private ParsedEvent yieldNextEvent() {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                this.hasNext = false;
                return (buffered != null) ? buffered : null;
            }
            charRead += line.length();
            if (charRead >= CHAR_READ_PROGRESS_STEP) {
                progress.set(progress.get() + charRead);
                charRead = 0;
            }
            long lineNumber = sequence.incrementAndGet();
            var parsed = parse(lineNumber, line);
            if (parsed != null) {
                var yield = buffered;
                buffered = parsed;
                return yield;
            } else if (format.getProfile().onParsingFailure() == ParsingFailureMode.CONCAT) {
                if (buffered != null) {
                    // Having to create a new event object each time we need to mutate the buffer's
                    // content isn't ideal performance-wise, but based on the assumption that most
                    // logs events should fit on a single line, it's probably not worth changing the
                    // buffer to a mutable structure. Something to keep in mind, though.
                    buffered = ParsedEvent.withTextFields(
                            buffered.getSequence(),
                            buffered.getTimestamp(),
                            buffered.getText() + "\n" + line,
                            buffered.getTextFields());
                }
            } else if (format.getProfile().onParsingFailure() == ParsingFailureMode.ABORT) {
                throw new FatalParsingEventException("Parsing aborted because of unparseable data at line " + lineNumber +
                        ": \"" + StringUtils.sanitizeNotificationMessage(line) + "\"");
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            return this.hasNext;
        }
    }

    private ParsedEvent parse(long lineNumber, String text) {
        var m = format.getProfile().getParsingRegex().matcher(text);
        if (m.find()) {
            LocalDateTime timestamp = format.getProfile().getTemporalAnchor().resolve();
            final Map<String, String> sections = new HashMap<>();
            ZoneId zoneId = format.getZoneId();
            for (Map.Entry<NamedCaptureGroup, String> entry : format.getProfile().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed = m.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                        if (temporalGroup == TemporalCaptureGroup.OFFSET) {
                            zoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(temporalGroup.parseInt(parsed)));
                        } else {
                            timestamp = timestamp.with(temporalGroup.getMapping(), temporalGroup.parseLong(parsed));
                        }
                    } else {
                        sections.put(captureGroup.name(), parsed);
                    }
                }
            }
            return ParsedEvent.withTextFields(lineNumber, ZonedDateTime.of(timestamp, zoneId), text, sections);
        }
        return null;
    }
}

