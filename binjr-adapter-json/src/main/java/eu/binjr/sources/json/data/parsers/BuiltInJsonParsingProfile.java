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

import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public enum BuiltInJsonParsingProfile implements JsonParsingProfile {
    ISO("[TEST] Collector",
            "BUILTIN_COLLECTOR",
            new JsonDefinition("root", "$[*]", "/created_at",
                    List.of(new JsonSeriesDefinition("/total_classes_stats/classes", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/total_classes_stats/fields", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/total_classes_stats/methods", null, "#", UnitPrefixes.METRIC, ChartType.LINE),

                            new JsonSeriesDefinition("/image_size_stats/total_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/code_cache_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/heap_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/resources_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/other_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/debuginfo_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/image_size_stats/resources_count", null, "#", UnitPrefixes.METRIC, ChartType.LINE),

                            new JsonSeriesDefinition("/reflection_stats/classes", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/reflection_stats/fields", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/reflection_stats/methods", null, "#", UnitPrefixes.METRIC, ChartType.LINE),

                            new JsonSeriesDefinition("/build_perf_stats/total_build_time_sec", null, "seconds", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/build_perf_stats/gc_time_sec", null, "seconds", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/build_perf_stats/num_cpu_cores", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/build_perf_stats/total_machine_memory", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/build_perf_stats/peak_rss_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/build_perf_stats/cpu_load", null, "#", UnitPrefixes.METRIC, ChartType.LINE),

                            new JsonSeriesDefinition("/reachability_stats/classes", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/reachability_stats/fields", null, "#", UnitPrefixes.METRIC, ChartType.LINE),
                            new JsonSeriesDefinition("/reachability_stats/methods", null, "#", UnitPrefixes.METRIC, ChartType.LINE),

                            new JsonSeriesDefinition("/runner_info/memory_size_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA),
                            new JsonSeriesDefinition("/runner_info/memory_available_bytes", null, "bytes", UnitPrefixes.BINARY, ChartType.AREA)
                    )),
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.MILLI, "\\d{3}",
                    CaptureGroup.of("TIMEZONE"), "(Z|[+-]\\d{2}:?(\\d{2})?)"),
            "$YEAR[\\s\\/-]$MONTH[\\s\\/-]$DAY([-\\sT]$HOUR:$MINUTE:$SECOND)?([\\.,]$MILLI)?$TIMEZONE?",
            Locale.US,
            false),

    EPOCH("Seconds since 01/01/1970",
            "EPOCH",
            null,
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+"),
            "$EPOCH",
            Locale.US,
            false),

    EPOCH_MS("Milliseconds since 01/01/1970",
            "EPOCH_MS",
            null,
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+",
                    TemporalCaptureGroup.MILLI, "\\d{3}"),
            "$EPOCH$MILLI",
            Locale.US,
            false);

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String profileId;
    private final Pattern regex;
    private final Locale numberFormattingLocale;
    private final JsonDefinition definitions;
    private final boolean abortOnTimestampParsingFailure;

    BuiltInJsonParsingProfile(String profileName,
                              String id,
                              JsonDefinition definitions,
                              Map<NamedCaptureGroup, String> groups,
                              String lineTemplateExpression,
                              Locale numberFormattingLocale,
                              boolean abortOnTimestampParsingFailure) {
        this.profileId = id;
        this.definitions = definitions;
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
        this.regex = Pattern.compile(buildParsingRegexString());
        this.numberFormattingLocale = numberFormattingLocale;
        this.abortOnTimestampParsingFailure = abortOnTimestampParsingFailure;
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

    @Override
    public JsonDefinition getJsonDefinition() {
        return this.definitions;
    }

    @Override
    public boolean isContinueOnTimestampParsingFailure() {
        return this.abortOnTimestampParsingFailure;
    }
}
