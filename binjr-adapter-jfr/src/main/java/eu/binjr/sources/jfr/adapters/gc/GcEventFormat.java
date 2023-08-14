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
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.sources.jfr.adapters.jfr.JfrEventParser;
import eu.binjr.sources.jfr.adapters.jfr.JfrRecordingFilter;
import eu.binjr.sources.jfr.adapters.jfr.events.JfrEventsAdapterPreferences;
import eu.binjr.sources.jfr.adapters.jfr.events.JfrEventsDataAdapter;
import jdk.jfr.MemoryAddress;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

public class GcEventFormat implements EventFormat<JfrRecordingFilter> {
    private final ZoneId zoneId;
    private final Charset encoding;
    private static final JfrEventsAdapterPreferences adapterPrefs;

    static {
        try {
            adapterPrefs = (JfrEventsAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JfrEventsDataAdapter.class.getName());
        } catch (NoAdapterFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public GcEventFormat(ZoneId zoneId, Charset encoding) {

        this.zoneId = zoneId;
        this.encoding = encoding;

    }

    @Override
    public ParsingProfile getProfile() {
        return BuiltInParsingProfile.NONE;
    }

    @Override
    public EventParser parse(JfrRecordingFilter source) {
        return new GcEventParser(this, source);
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getZoneId() {
        return zoneId;
    }

    public static boolean includeField(ValueDescriptor field) {
        return field.getAnnotation(Timestamp.class) == null &&
                field.getAnnotation(MemoryAddress.class) ==null &&
                Arrays.stream(adapterPrefs.includedEventsDataTypes.get()).anyMatch(s -> Objects.equals(s, field.getTypeName())) &&
                Arrays.stream(adapterPrefs.excludedEventsNames.get()).noneMatch(s -> Objects.equals(s, field.getName()));
    }

}
