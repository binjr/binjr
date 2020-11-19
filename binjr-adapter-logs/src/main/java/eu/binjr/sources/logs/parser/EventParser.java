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

package eu.binjr.sources.logs.parser;

import eu.binjr.common.logging.Logger;
import eu.binjr.sources.logs.parser.capture.NamedCaptureGroup;
import eu.binjr.sources.logs.parser.capture.TemporalCaptureGroup;
import eu.binjr.sources.logs.parser.profile.ParsingProfile;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventParser {
    private static final Logger logger = Logger.create(EventParser.class);
    private final Pattern parsingRegex;
    private final Set<String> namedGroups = new HashSet<>();
    private final ParsingProfile profile;
    private final ZoneId zoneId;

    public EventParser(ParsingProfile profile, ZoneId zoneId) {
        this.profile = profile;
        this.zoneId = zoneId;
        String regexString = profile.getLineTemplateExpression();
        for (Map.Entry<NamedCaptureGroup, String> entry : profile.getCaptureGroups().entrySet()) {
            NamedCaptureGroup t = entry.getKey();
            String e = entry.getValue();
            regexString = regexString.replace("$" + t.name(), String.format("(?<%s>%s)", t.name(), e));
        }
        logger.debug("regexString = " + regexString);
        parsingRegex = Pattern.compile(regexString);

    }

    public ParsingProfile getProfile() {
        return profile;
    }

    public Pattern getParsingRegex() {
        return parsingRegex;
    }

    public Optional<ParsedEvent> parse(String text) {
        var m = parsingRegex.matcher(text);
        var timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, zoneId);
        final Map<String, String> sections = new HashMap<>();
        if (m.find()) {
            for (Map.Entry<NamedCaptureGroup, String> entry : profile.getCaptureGroups().entrySet()) {
                var t = entry.getKey();
                var parsed = m.group(t.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (t instanceof TemporalCaptureGroup) {
                        timestamp = timestamp.with(((TemporalCaptureGroup) t).getMapping(), Long.parseLong(parsed));
                    } else {
                        sections.put(t.name(), parsed);
                    }
                }
            }
            return Optional.of(new ParsedEvent(timestamp, sections, text));
        }
        return Optional.empty();
    }



    public static class ParsedEvent {
        private final ZonedDateTime timestamp;
        private final Map<String, String> sections;
        private final String text;

        private ParsedEvent(ZonedDateTime timestamp, Map<String, String> sections, String text) {
            this.sections = sections;
            this.text = text;
            this.timestamp = timestamp;
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
    }

}
