/*
 *    Copyright 2020 Frederic Thevenet
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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public class ParsedEvent {
    private final ZonedDateTime timestamp;
    private final long lineNumber;
    private final Map<String, String> sections;
    private String text;

    public ParsedEvent(long lineNumber, ZonedDateTime timestamp, String text, Map<String, String> sections) {
        this.lineNumber = lineNumber;
        this.text = text;
        this.timestamp = timestamp;
        this.sections = sections;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getText() {
        return text;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getSections() {
        return sections;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ParsedEvent{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", sections=").append(sections);
        sb.append(", text='").append(text).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class LogEventBuilder {
        private final EventParser parser;
        private ParsedEvent previous = null;
        private long lineNumber;
        private ZonedDateTime timestamp;
        private StringBuilder textBuilder = new StringBuilder();

        public LogEventBuilder(EventParser parser) {
            this.parser = parser;
        }

        public Optional<ParsedEvent> build(long lineNumber, String text) {
             var res = parser.parse(lineNumber, text);
             if (res.isPresent()) {
                 previous = res.get();
             }else {
                 if (previous != null) {
                     previous.text += "\n" + text;
                 }
             }
             return res;
        }

        public Optional<ParsedEvent> getLast() {
            return previous != null ? Optional.of(previous) : Optional.empty();
        }
    }
}
