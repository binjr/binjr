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

import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;

import java.util.Map;
import java.util.UUID;

public class CustomCsvParsingProfile extends CustomParsingProfile implements CsvParsingProfile {
    private final char delimiter;
    private final int timestampColumn;
    private final int[] excludedColumns;

    private CustomCsvParsingProfile() {
        super();
        delimiter = ',';
        timestampColumn = 0;
        excludedColumns = new int[0];
    }

    private CustomCsvParsingProfile(String profileName,
                                    Map<NamedCaptureGroup, String> captureGroups,
                                    String lineTemplateExpression,
                                    char delimiter,
                                    int timestampColumn,
                                    int[] excludedColumns) {
        this(profileName,
                UUID.randomUUID().toString(),
                captureGroups,
                lineTemplateExpression,
                delimiter,
                timestampColumn,
                excludedColumns);
    }

    private CustomCsvParsingProfile(String profileName,
                                    String profileId,
                                    Map<NamedCaptureGroup, String> captureGroups,
                                    String lineTemplateExpression,
                                    char delimiter,
                                    int timestampColumn,
                                    int[] excludedColumns) {
        super(profileName, profileId, captureGroups, lineTemplateExpression);
        this.delimiter = delimiter;
        this.timestampColumn = timestampColumn;
        this.excludedColumns = excludedColumns;
    }


    public static CustomCsvParsingProfile of(CsvParsingProfile profile) {
        if (profile == null) {
            return null;
        }
        return new CustomCsvParsingProfile(profile.getProfileName(),
                profile.getProfileId(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression(),
                profile.getDelimiter(),
                profile.getTimestampColumn(),
                profile.getExcludedColumns());
    }

    @Override
    public char getDelimiter() {
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
}
