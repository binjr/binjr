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

package eu.binjr.sources.logs.data.parsers;

import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.ZoneId;
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
    public Optional<ParsedEvent> parse(String text) {
        return parse(-1, text);
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
    }
}
