/*
 *    Copyright 2022 Frederic Thevenet
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

/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.sources.csv.data.parsers;

import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.util.Map;
import java.util.regex.Pattern;

public enum BuiltInCsvParsingProfile implements CsvParsingProfile {
    ISO("ISO timestamps",
            "BUILTIN_ISO",
            Map.of(TemporalCaptureGroup.YEAR, "\\d{4}",
                    TemporalCaptureGroup.MONTH, "\\d{2}",
                    TemporalCaptureGroup.DAY, "\\d{2}",
                    TemporalCaptureGroup.HOUR, "\\d{2}",
                    TemporalCaptureGroup.MINUTE, "\\d{2}",
                    TemporalCaptureGroup.SECOND, "\\d{2}",
                    TemporalCaptureGroup.MILLI, "\\d{3}",
                    CaptureGroup.of("TIMEZONE"), "(Z|[+-]\\d{2}:?(\\d{2})?)"),
            "$YEAR[\\/-]?$MONTH[\\/-]?$DAY[-\\sT]$HOUR?:?$MINUTE?:?$SECOND?([\\.,]$MILLI)?$TIMEZONE?",
            ',',
            0,
            new int[0]),
    EPOCH("Seconds since 01/01/1970",
            "EPOCH",
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+"),
            "$EPOCH",
            ',',
            0,
            new int[0]),
    EPOCH_MS("Milliseconds since 01/01/1970",
            "EPOCH_MS",
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+",
                    TemporalCaptureGroup.MILLI, "\\d{3}"),
            "$EPOCH$MILLI",
            ',',
            0,
            new int[0]);;

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String profileId;
    private final Pattern regex;
    private final char delimiter;
    private final int timestampColumn;
    private final int[] excludedColumns;

    BuiltInCsvParsingProfile(String profileName,
                             String id,
                             Map<NamedCaptureGroup, String> groups,
                             String lineTemplateExpression,
                             char delimiter,
                             int timestampColumn,
                             int[] excludedColumns) {
        this.profileId = id;
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
        this.regex = Pattern.compile(buildParsingRegexString());
        this.delimiter = delimiter;
        this.timestampColumn = timestampColumn;
        this.excludedColumns = excludedColumns;
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
    public char getDelimiter() {
        return delimiter;
    }

    @Override
    public int getTimestampColumn() {
        return timestampColumn;
    }

    @Override
    public int[] getExcludedColumns() {
        return excludedColumns;
    }
}
