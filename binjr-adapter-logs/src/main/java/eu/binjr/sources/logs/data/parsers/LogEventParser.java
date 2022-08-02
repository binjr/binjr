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

package eu.binjr.sources.logs.data.parsers;

import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public class LogEventParser implements EventParser {
    private final BufferedReader reader;
    private final AtomicLong sequence;
    private final LogEventFormat format;
    private final LogEventIterator logEventIterator;
    private ParsedEvent<String> buffered;

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
    public Iterator<ParsedEvent<?>> iterator() {
        return logEventIterator;
    }

    public class LogEventIterator implements Iterator<ParsedEvent<?>> {
        private boolean hasNext = true;

        @Override

        public ParsedEvent<String> next() {
            var event = yieldNextEvent();
            while (event == null && hasNext) {
                event = yieldNextEvent();
            }
            return event;
        }

        private ParsedEvent<String> yieldNextEvent() {
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
            var parsed = format.parse(sequence.incrementAndGet(), line);
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
}

