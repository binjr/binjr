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

package eu.binjr.core.data.indexes.parser.profile;

import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;

import java.util.Map;

import static eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup.*;

public enum BuiltInParsingProfile implements ParsingProfile {
    ISO("ISO-like timestamps",
            "BUILTIN_ISO",
            Map.of(YEAR, "\\d{4}",
                    MONTH, "\\d{2}",
                    DAY, "\\d{2}",
                    HOUR, "\\d{2}",
                    MINUTE, "\\d{2}",
                    SECOND, "\\d{2}",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR[\\/-]$MONTH[\\/-]$DAY[-\\sT]$HOUR:$MINUTE:$SECOND([\\.,]$FRACTION)?\\]\\s*(\\[\\s?$SEVERITY\\s?\\])?.*"),
    BINJR_STRICT("binjr logs",
            "BUILTIN_BJR",
            Map.of(YEAR, "\\d{4}",
                    MONTH, "\\d{2}",
                    DAY, "\\d{2}",
                    HOUR, "\\d{2}",
                    MINUTE, "\\d{2}",
                    SECOND, "\\d{2}",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR-$MONTH-$DAY\\s$HOUR:$MINUTE:$SECOND\\.$FRACTION\\]\\s+\\[$SEVERITY\\s?\\].*"),
    GC("JVM Unified Logging",
            "BUILTIN_JVM",
            Map.of(EPOCH, "\\d+",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL|STDOUT|STDERR",
                    CaptureGroup.of("TAGS"), ".*"),
            "\\[$EPOCH[\\.,]($FRACTION)s\\s*\\]\\[$SEVERITY\\s*\\]\\[$TAGS\\s*\\].*"),
    ALL("All non empty lines",
            "BUILTIN_ALL",
            Map.of(CaptureGroup.of("LINE"), ".+"),
            "$LINE");

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String id;

    BuiltInParsingProfile(String profileName,
                          String id,
                          Map<NamedCaptureGroup, String> groups,
                          String lineTemplateExpression) {
        this.id = id;
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
    }

    @Override
    public String getProfileId() {
        return this.id;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public Map<NamedCaptureGroup, String> getCaptureGroups() {
        return captureGroups;
    }

    @Override
    public String toString() {
        return "[Built-in] " + profileName;
    }

}
