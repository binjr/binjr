/*
 *    Copyright 2022-2025 Frederic Thevenet
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
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;

import java.util.Locale;
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
                    TemporalCaptureGroup.OFFSET, "Z|[+-]\\d{2}:?(\\d{2})?"),
            "$YEAR[\\s\\/-]$MONTH[\\s\\/-]$DAY([-\\sT]$HOUR:$MINUTE:$SECOND)?([\\.,]$MILLI)?$OFFSET?",
            ",",
            '"',
            0,
            new int[0],
            true,
            Locale.US,
            false,
            ParsingFailureMode.ABORT,
            '#'),
    EPOCH("Seconds since 01/01/1970",
            "EPOCH",
            Map.of(TemporalCaptureGroup.EPOCHSECONDS, "\\d+"),
            "$EPOCH",
            ",",
            '"',
            0,
            new int[0],
            true,
            Locale.US,
            false,
            ParsingFailureMode.ABORT,
            '#'),
    EPOCH_MS("Milliseconds since 01/01/1970 UTC",
            "EPOCH_MS",
            Map.of(TemporalCaptureGroup.EPOCHMILLIS, "\\d+"),
            "$EPOCHMILLIS",
            ",",
            '"',
            0,
            new int[0],
            true,
            Locale.US,
            false,
            ParsingFailureMode.ABORT,
            '#');

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String profileId;
    private final Pattern regex;
    private final String delimiter;
    private final int timestampColumn;
    private final int[] excludedColumns;
    private final Locale numberFormattingLocale;
    private final boolean readColumnNames;
    private final Character quoteCharacter;
    private final boolean trimCellValues;
    private final ParsingFailureMode onParsingFailure;
    private final Character commentMarker;

    BuiltInCsvParsingProfile(String profileName,
                             String id,
                             Map<NamedCaptureGroup, String> groups,
                             String lineTemplateExpression,
                             String delimiter,
                             Character quoteChar,
                             int timestampColumn,
                             int[] excludedColumns,
                             boolean readColumnNames,
                             Locale numberFormattingLocale,
                             boolean trimCellValues,
                             ParsingFailureMode onParsingFailure,
                             Character commentMarker) {
        this.profileId = id;
        this.profileName = profileName;
        this.captureGroups = groups;
        this.lineTemplateExpression = lineTemplateExpression;
        this.commentMarker = commentMarker;
        this.regex = Pattern.compile(buildParsingRegexString());
        this.delimiter = delimiter;
        this.quoteCharacter = quoteChar;
        this.timestampColumn = timestampColumn;
        this.excludedColumns = excludedColumns;
        this.readColumnNames = readColumnNames;
        this.numberFormattingLocale = numberFormattingLocale;
        this.trimCellValues = trimCellValues;
        this.onParsingFailure = onParsingFailure;
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
    public String getDelimiter() {
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

    @Override
    public boolean isReadColumnNames() {
        return readColumnNames;
    }

    @Override
    public Locale getNumberFormattingLocale() {
        return numberFormattingLocale;
    }

    @Override
    public Character getQuoteCharacter() {
        return quoteCharacter;
    }

    @Override
    public boolean isTrimCellValues() {
        return trimCellValues;
    }

    @Override
    public Character getCommentMarker() {
        return commentMarker;
    }

    @Override
    public ParsingFailureMode onParsingFailure() {
        return onParsingFailure;
    }
}
