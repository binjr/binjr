/*
 * Copyright 2023 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.sources.jfr.adapters.gc;

import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.sources.jfr.adapters.jfr.JfrEventFormat;
import eu.binjr.sources.jfr.adapters.jfr.JfrRecordingFilter;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;


public class GcEventParser implements EventParser {
    private static final Logger logger = Logger.create(GcEventParser.class);


    // private final BufferedReader reader;
    private final AtomicLong sequence;
    private final GcEventFormat format;
    private final GcEventIterator eventIterator;
    private final LongProperty progress = new SimpleLongProperty(0);
    private final JfrRecordingFilter eventTypeFilter;

    GcEventParser(GcEventFormat format, JfrRecordingFilter filter) {
        this.sequence = new AtomicLong(0);
        this.format = format;
        this.eventIterator = new GcEventIterator();
        try (var p = Profiler.start("Initialize GC Log file", logger::perf)) {
            this.eventTypeFilter = filter;

        }
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
        return this.eventIterator;
    }

    public class GcEventIterator implements Iterator<ParsedEvent> {

        @Override
        public ParsedEvent next() {
            ParsedEvent event = null;
            while (event == null ) {
                //do something
            }
            return event;
        }




        @Override
        public boolean hasNext() {
            return false;
        }


    }

}



