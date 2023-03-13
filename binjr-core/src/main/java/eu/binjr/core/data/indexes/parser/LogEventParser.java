/*
 *    Copyright 2022-2023 Frederic Thevenet
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

import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class LogEventParser implements EventParser {
    private final BufferedReader reader;
    private final AtomicLong sequence;
    private final LogEventFormat format;
    private final LogEventIterator logEventIterator;
    private ParsedEvent buffered;

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

        @Override

        public ParsedEvent next() {
            var event = yieldNextEvent();
            while (event == null && hasNext) {
                event = yieldNextEvent();
            }
            return event;
        }

        private ParsedEvent yieldNextEvent() {
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                this.hasNext = false;
                return buffered != null ? buffered : null;
            }
            charRead += line.length();
            if (charRead >= 10240) {
                progress.set(progress.get() + charRead);
                charRead = 0;
            }
            var parsed = parse(sequence.incrementAndGet(), line);
            if (parsed.isPresent()) {
                var yield = buffered;
                buffered = parsed.get();
                return yield;
            } else {
                if (buffered != null) {
                    buffered = new ParsedEvent(
                            buffered.getSequence(),
                            buffered.getTimestamp(),
                            buffered.getText() + "\n" + line,
                            buffered.getFields());
                }
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return this.hasNext;
        }
    }

    private Optional<ParsedEvent> parse(long lineNumber, String text) {
        var m = format.getProfile().getParsingRegex().matcher(text);
        if (m.find()) {
            ZonedDateTime timestamp = ZonedDateTime.of(format.getProfile().getTemporalAnchor(), format.getZoneId());
            final Map<String, String> sections = new HashMap<>();
            for (Map.Entry<NamedCaptureGroup, String> entry : format.getProfile().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed = m.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                        timestamp = timestamp.with(temporalGroup.getMapping(), temporalGroup.parseLong(parsed));
                    } else {
                        sections.put(captureGroup.name(), parsed);
                    }
                }
            }
            return Optional.of(new ParsedEvent(lineNumber, timestamp, text, sections));
        }
        return Optional.empty();
    }
}

