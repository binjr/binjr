/*
 *    Copyright 2020-2025 Frederic Thevenet
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

import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public enum BuiltInParsingProfile implements ParsingProfile {
    NONE("--", "BUILTIN_NONE", Map.of(), "",
            ParsingFailureMode.CONCAT),
    ALL("All non empty lines",
            "BUILTIN_ALL",
            Map.of(CaptureGroup.of("LINE"), ".+"),
            "$LINE",
            ParsingFailureMode.CONCAT),
    ISO("ISO-like timestamps",
            "BUILTIN_ISO",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR[\\/-]$MONTH[\\/-]$DAY[-\\sT]$HOUR:$MINUTE:$SECOND([\\.,]$FRACTION)?\\]\\s*(\\[\\s?$SEVERITY\\s?\\])?.*",
            ParsingFailureMode.CONCAT),
    BINJR_STRICT("binjr logs",
            "BUILTIN_BJR",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL"),
            "\\[$YEAR-$MONTH-$DAY\\s$HOUR:$MINUTE:$SECOND\\.$FRACTION\\]\\s+\\[$SEVERITY\\s?\\].*",
            ParsingFailureMode.CONCAT),
    JVM("JVM Unified Logging",
            "BUILTIN_JVM",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.MILLI, "\\d{3}",
                    TemporalCaptureGroup.ELAPSEDSECONDS, "\\d+",
                    TemporalCaptureGroup.FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "(?i)TRACE|DEBUG|INFO|WARNING|ERROR"),
            "(\\[$YEAR-$MONTH-$DAY(T)$HOUR:$MINUTE:$SECOND\\.$MILLI([+-]\\d+)?\\])?(\\[($ELAPSEDSECONDS[\\.,]$FRACTION(s))\\s*\\])?(\\[$SEVERITY\\s*\\])?.*",
            ParsingFailureMode.CONCAT),
    QUARKUS("Quarkus logs",
            "BUILTIN_QRK",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.FRACTION, "\\d{3}",
                    CaptureGroup.of("SEVERITY"), "TRACE|FINEST|FINER|FINE|DEBUG|PERF|CONFIG|NOTE|INFO|WARNING|WARN|ERROR|FATAL|SEVERE"),
            "$YEAR[\\/-]$MONTH[\\/-]$DAY[-\\sT]$HOUR:$MINUTE:$SECOND([\\.,]$FRACTION)?.* $SEVERITY .*",
            ParsingFailureMode.CONCAT),
    ITW("IcedTea-Web logs",
            "BUILTIN_ITW",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    CaptureGroup.of("SEVERITY"), "(MESSAGE|WARNING|ERROR)"),
            ".*\\[$SEVERITY_(DEBUG|ALL)]\\[\\w{3}\\s$MONTH\\s$DAY\\s$HOUR:$MINUTE:$SECOND\\s.+\\s$YEAR\\].*",
            ParsingFailureMode.CONCAT),
    SYSLOGS("Syslogs",
            "BUILTIN_SYS",
            Map.of(TemporalCaptureGroup.MONTH, "(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}"),
            "$MONTH $DAY $HOUR:$MINUTE:$SECOND",
            ParsingFailureMode.CONCAT),
    HOTSPOT_JIT("Hotspot JVM JIT logs",
            "BUILTIN_HSJIT",
            Map.of(TemporalCaptureGroup.ELAPSEDMILLIS, "\\d+",
                    CaptureGroup.of("COMPID"), "\\d+",
                    CaptureGroup.of("SEVERITY"), "[0-4]",
                    CaptureGroup.of("METHOD"), ".*",
                    CaptureGroup.of("SIZE"), "(\\d+|native)"),
            "$ELAPSEDMILLIS\\s+$COMPID\\s+[%\\s!\\w]*$SEVERITY\\s*$METHOD\\s*\\($SIZE( bytes)?\\).*",
            ParsingFailureMode.CONCAT);

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String profileId;
    private final Pattern regex;
    private final ParsingFailureMode onParsingFailure;

    BuiltInParsingProfile(String profileName,
                          String id,
                          Map<NamedCaptureGroup, String> groups,
                          String lineTemplateExpression,
                          ParsingFailureMode onParsingFailure) {
        this.profileId = id;
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
        this.onParsingFailure = onParsingFailure;
        this.regex = Pattern.compile(buildParsingRegexString());
    }

    public static BuiltInParsingProfile[] editableProfiles() {
        return Arrays.stream(values()).filter(v -> v != NONE).toArray(BuiltInParsingProfile[]::new);
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
    }

    @Override
    public Pattern getParsingRegex() {
        return regex;
    }

    @Override
    public ParsingFailureMode onParsingFailure() {
        return this.onParsingFailure;
    }

    @Override
    public boolean isBuiltIn() {
        return true;
    }

    @Override
    public String getProfileId() {
        return this.profileId;
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
        return profileName;
    }



}
