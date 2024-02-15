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

import com.google.gson.annotations.JsonAdapter;
import eu.binjr.common.json.adapters.LocaleJsonAdapter;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CustomCsvParsingProfile extends CustomParsingProfile implements CsvParsingProfile {
    private final String delimiter;
    private final char quoteCharacter;
    private final int timestampColumn;
    private final int[] excludedColumns;
    private final boolean readColumnNames;
    @JsonAdapter(LocaleJsonAdapter.class)
    private final Locale formattingLocale;
    private final boolean trimCellValues;

    public CustomCsvParsingProfile() {
        this("", UUID.randomUUID().toString(), new HashMap<>(), "", ",", '"', 0, new int[0], true, Locale.getDefault(), false);
    }


    public static CsvParsingProfile of(CsvParsingProfile parsingProfile) {
        return new CustomCsvParsingProfile(parsingProfile.getProfileName(),
                parsingProfile.getProfileId(),
                parsingProfile.getCaptureGroups(),
                parsingProfile.getLineTemplateExpression(),
                parsingProfile.getDelimiter(),
                parsingProfile.getQuoteCharacter(),
                parsingProfile.getTimestampColumn(),
                parsingProfile.getExcludedColumns(),
                parsingProfile.isReadColumnNames(),
                parsingProfile.getNumberFormattingLocale(),
                parsingProfile.isTrimCellValues());
    }


    public CustomCsvParsingProfile(String profileName,
                                   String profileId,
                                   Map<NamedCaptureGroup, String> captureGroups,
                                   String lineTemplateExpression,
                                   String delimiter,
                                   char quoteCharacter, int timestampColumn,
                                   int[] excludedColumns, boolean readColumnNames,
                                   Locale formattingLocale, boolean trimCellValues) {
        super(profileName, profileId, captureGroups, lineTemplateExpression);
        this.delimiter = delimiter;
        this.quoteCharacter = quoteCharacter;
        this.timestampColumn = timestampColumn;
        this.excludedColumns = excludedColumns;
        this.readColumnNames = readColumnNames;
        this.formattingLocale = formattingLocale;
        this.trimCellValues = trimCellValues;
    }

    @Override
    public String getDelimiter() {
        return this.delimiter;
    }

    @Override
    public int getTimestampColumn() {
        return this.timestampColumn;
    }

    @Override
    public int[] getExcludedColumns() {
        return this.excludedColumns;
    }

    @Override
    public boolean isReadColumnNames() {
        return readColumnNames;
    }

    @Override
    public Locale getNumberFormattingLocale() {
        return formattingLocale;
    }

    @Override
    public char getQuoteCharacter() {
        return quoteCharacter;
    }

    @Override
    public boolean isTrimCellValues() {
        return trimCellValues;
    }
}
