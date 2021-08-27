/*
 *    Copyright 2020-2021 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;

public class EventParser {
    private static final Logger logger = Logger.create(EventParser.class);
    private final Pattern parsingRegex;
    private final Set<String> namedGroups = new HashSet<>();
    private final ParsingProfile profile;
    private final ZoneId zoneId;
    private static final Pattern GROUP_TAG_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]{2,}");

    public EventParser(ParsingProfile profile, ZoneId zoneId) {
        this.profile = profile;
        this.zoneId = zoneId;
        var regexString = new String[]{ profile.getLineTemplateExpression()};
        var matcher = GROUP_TAG_PATTERN.matcher(regexString[0]);
        while(matcher.find()) {
            var value = matcher.group();
             profile.getCaptureGroups().entrySet().stream()
                    .filter(e->CaptureGroup.of(value).equals(e.getKey()))
                    .map(e-> String.format("(?<%s>%s)", e.getKey().name(), e.getValue()))
                    .findAny().ifPresent(r -> regexString[0] = regexString[0].replace(value, r));
        }
        logger.debug("regexString = " + regexString[0]);
        parsingRegex = Pattern.compile(regexString[0]);
    }

    private ParsedEvent doParse(long lineNumber, String text) {
        var m = parsingRegex.matcher(text);
        var timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, zoneId);
        final Map<String, String> sections = new HashMap<>();
        if (m.find()) {
            for (Map.Entry<NamedCaptureGroup, String> entry : profile.getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed = m.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                        timestamp = timestamp.with(temporalGroup.getMapping(), Long.parseLong(parsed));
                    } else {
                        sections.put(captureGroup.name(), parsed);
                    }
                }
            }
            return new ParsedEvent(lineNumber, timestamp, text, sections);
        }
        return null;
    }

    public ParsingProfile getProfile() {
        return profile;
    }

    public Pattern getParsingRegex() {
        return parsingRegex;
    }

    public EventAggregator aggregator() {
        return new EventAggregator();
    }

    public Optional<ParsedEvent> parse(long sequence, String text) {
        return Optional.ofNullable(doParse(sequence, text));
    }

    public Optional<ParsedEvent> parse(String text) {
        return Optional.ofNullable(doParse(-1, text));
    }

    public class EventAggregator {
        private ParsedEvent buffered;

        private EventAggregator() {
        }

        public Optional<ParsedEvent> yield(long sequence, String text) {
            var parsed = doParse(sequence, text);
            if (parsed != null) {
                var yield = buffered;
                buffered = parsed;
                return Optional.ofNullable(yield);
            } else {
                if (buffered != null) {
                    buffered = new ParsedEvent(
                            buffered.getSequence(),
                            buffered.getTimestamp(),
                            buffered.getText() + "\n" + text,
                            buffered.getSections());
                }
                return Optional.empty();
            }
        }

        public Optional<ParsedEvent> tail() {
            return buffered != null ? Optional.of(buffered) : Optional.empty();
        }
    }


}
