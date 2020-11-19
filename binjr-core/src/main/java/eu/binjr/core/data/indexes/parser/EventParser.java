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

import eu.binjr.common.logging.Logger;
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
        return parse(0, text);
    }

    public Optional<ParsedEvent> parse(long number, String text) {
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
            return Optional.of(new ParsedEvent(number, timestamp, text, sections));
        }
        return Optional.empty();
    }

}
