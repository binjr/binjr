/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LogEventFormat implements EventFormat {
    private final ParsingProfile profile;
    private final ZoneId zoneId;
    private final Charset encoding;

    public LogEventFormat(ParsingProfile profile, ZoneId zoneId, Charset encoding) {
        this.profile = profile;
        this.zoneId = zoneId;
        this.encoding = encoding;
    }

    @Override
    public ParsingProfile getProfile() {
        return profile;
    }

    @Override
    public EventParser parse(InputStream ias) {
        return new LogEventParser(this, ias);
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
    }

    public Optional<ParsedEvent> parse(long lineNumber, String text) {
        var m = getProfile().getParsingRegex().matcher(text);
        var timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, getZoneId());
        final Map<String, String> sections = new HashMap<>();
        if (m.find()) {
            for (Map.Entry<NamedCaptureGroup, String> entry : getProfile().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed =m.group(captureGroup.name());
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
