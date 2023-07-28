/*
 *    Copyright 2023 Frederic Thevenet
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

package eu.binjr.sources.jfr.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import jdk.jfr.MemoryAddress;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

public class JfrEventFormat implements EventFormat<JfrRecordingFilter> {
    public static final String EVENT_TYPE_NAME = "eventTypeName";
    public static final String JDK_CPULOAD = "jdk.CPULoad";
    public static final String JVM_SYSTEM = "jvmSystem";
    public static final String JVM_USER = "jvmUser";
    public static final String MACHINE_TOTAL = "machineTotal";
    private static final Logger logger = Logger.create(JfrEventParser.class);
    public static final String CATEGORIES = "categories";
    public static final String HAS_NUM_FIELDS = "hasNumFields";
    public static final String GCREF_TYPE_FIELD = "type";
    public static final String JDK_GCREFERENCE_STATISTICS = "jdk.GCReferenceStatistics";
    public static final String GCREF_COUNT_FIELD = "count";
    public static final String JDK_TYPES_THREAD_GROUP = "jdk.types.ThreadGroup";
    public static final String GCREF_FINAL_REFERENCE = "Final reference";
    public static final String GCREF_SOFT_REFERENCE = "Soft reference";
    public static final String GCREF_WEAK_REFERENCE = "Weak reference";
    public static final String GCREF_PHANTOM_REFERENCE = "Phantom reference";
    public static final String GCREF_TOTAL_COUNT = "Total Count";
    public static final String JDK_TYPES_STACK_TRACE = "jdk.types.StackTrace";
    public static final String JDK_GARBAGE_COLLECTION = "jdk.GarbageCollection";
    private final ZoneId zoneId;
    private final Charset encoding;
    private static final JfrAdapterPreferences adapterPrefs;

    static {
        try {
            adapterPrefs = (JfrAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JfrDataAdapter.class.getName());
        } catch (NoAdapterFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public JfrEventFormat(ZoneId zoneId, Charset encoding) {

        this.zoneId = zoneId;
        this.encoding = encoding;

    }

    @Override
    public ParsingProfile getProfile() {
        return BuiltInParsingProfile.NONE;
    }

    @Override
    public EventParser parse(JfrRecordingFilter source) {
        return new JfrEventParser(this, source);
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
