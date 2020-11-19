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
    BINJR("binjr logs",
            Map.of(YEAR, "\\d{4}",
                    MONTH, "\\d{2}",
                    DAY, "\\d{2}",
                    HOUR, "\\d{2}",
                    MINUTE, "\\d{2}",
                    SECOND, "\\d{2}",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR[\\/-]$MONTH[\\/-]$DAY[-\\sT]$HOUR:$MINUTE:$SECOND([\\.,]$FRACTION)?\\]\\s*(\\[\\s?$SEVERITY\\s?\\])?.*"),
    BINJR_STRICT("binjr logs (Strict)",
            Map.of(YEAR, "\\d{4}",
                    MONTH, "\\d{2}",
                    DAY, "\\d{2}",
                    HOUR, "\\d{2}",
                    MINUTE, "\\d{2}",
                    SECOND, "\\d{2}",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR-$MONTH-$DAY\\s$HOUR:$MINUTE:$SECOND\\.$FRACTION\\]\\s+\\[$SEVERITY\\s?\\].*"),
    GC("JVM GC logs",
            Map.of(EPOCH, "\\d+",
                    FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL",
                    CaptureGroup.of("PHASE"), ".*"),
            "\\[$EPOCH[\\.,]$FRACTIONs\\]\\[$SEVERITY\\]\\[$PHASE\\s*\\].*");

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;

    BuiltInParsingProfile(String profileName,
                          Map<NamedCaptureGroup, String> groups,
                          String lineTemplateExpression) {
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
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
