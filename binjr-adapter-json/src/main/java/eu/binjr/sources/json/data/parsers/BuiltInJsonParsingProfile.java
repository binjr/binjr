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
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public enum BuiltInJsonParsingProfile implements JsonParsingProfile {
    ISO("ISO timestamps",
            "BUILTIN_ISO",
            List.of(new JsonSeriesDefinition("foo", "path", ChartType.STACKED,"bytes", UnitPrefixes.BINARY,
                            List.of(new JsonSamplesDefinition("yule", "path", Color.RED),
                                    new JsonSamplesDefinition("romi", "path", Color.BLUEVIOLET),
                                    new JsonSamplesDefinition("pote", "path", Color.GREEN),
                                new JsonSamplesDefinition("marm", "path", Color.YELLOW))),
                    new JsonSeriesDefinition("bar", "path", ChartType.LINE, "meters", UnitPrefixes.METRIC,
                            List.of(new JsonSamplesDefinition("olar", "path", null),
                                    new JsonSamplesDefinition("piri", "path", null),
                                    new JsonSamplesDefinition("ulto", "path", null),
                                    new JsonSamplesDefinition("nuga", "path", null))),
                    new JsonSeriesDefinition("baz", "path", ChartType.SCATTER, "", UnitPrefixes.NONE,
                            List.of(new JsonSamplesDefinition("vipo", "path", null),
                                    new JsonSamplesDefinition("sari", "path", null),
                                    new JsonSamplesDefinition("xobi", "path", null),
                                    new JsonSamplesDefinition("zemu", "path", null)))),
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
            List.of(),
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+"),
            "$EPOCH",
            Locale.US,
            false),
    EPOCH_MS("Milliseconds since 01/01/1970",
            "EPOCH_MS",
            List.of(),
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
    private final List<JsonSeriesDefinition> definitions;
    private final boolean abortOnTimestampParsingFailure;

    BuiltInJsonParsingProfile(String profileName,
                              String id,
                              List<JsonSeriesDefinition> definitions,
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
    public List<JsonSeriesDefinition> getSeriesDefinitions() {
        return this.definitions;
    }

    @Override
    public Locale getNumberFormattingLocale() {
        return numberFormattingLocale;
    }

    @Override
    public boolean isContinueOnTimestampParsingFailure() {
        return this.abortOnTimestampParsingFailure;
    }
}
