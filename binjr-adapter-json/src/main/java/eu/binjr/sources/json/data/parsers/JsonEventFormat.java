/*
 *    Copyright 2025 Frederic Thevenet
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

package eu.binjr.sources.json.data.parsers;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;

import java.io.*;
import java.nio.charset.Charset;
import java.time.ZoneId;

public class JsonEventFormat implements EventFormat<InputStream> {
    private static final Logger logger = Logger.create(JsonEventFormat.class);
    private final JsonParsingProfile profile;
    private final ZoneId zoneId;
    private final Charset encoding;

    public JsonEventFormat(JsonParsingProfile profile, ZoneId zoneId, Charset encoding) {
        this.profile = profile;
        this.zoneId = zoneId;
        this.encoding = encoding;
    }

    @Override
    public JsonParsingProfile getProfile() {
        return profile;
    }

    @Override
    public EventParser parse(InputStream ias) {
        return new JsonEventParser(this, ias);
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
