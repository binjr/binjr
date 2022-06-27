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

package eu.binjr.sources.csv.adapters;

import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.util.Map;

import static eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup.*;

public enum BuiltInCsvTimestampParsingProfile implements ParsingProfile {
    ISO("ISO timestamps",
            "BUILTIN_ISO",
            Map.of(YEAR, "\\d{4}",
                    MONTH, "\\d{2}",
                    DAY, "\\d{2}",
                    HOUR, "\\d{2}",
                    MINUTE, "\\d{2}",
                    SECOND, "\\d{2}",
                    MILLI, "\\d{3}",
                    CaptureGroup.of("TIMEZONE"), "(Z|[+-]\\d{2}:?(\\d{2})?)"),
            "$YEAR[\\/-]?$MONTH[\\/-]?$DAY[-\\sT]$HOUR?:?$MINUTE?:?$SECOND?([\\.,]$MILLI)?$TIMEZONE?"),
    EPOCH("Seconds since 01/01/1970",
            "EPOCH",
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+"),
            "$EPOCH"),
    EPOCH_MS("Milliseconds since 01/01/1970",
            "EPOCH_MS",
            Map.of(TemporalCaptureGroup.EPOCH, "\\d+",
                    MILLI, "\\d{3}"),
            "$EPOCH$MILLI");;

    private final String profileName;
    private final String lineTemplateExpression;
    private final Map<NamedCaptureGroup, String> captureGroups;
    private final String profileId;

    BuiltInCsvTimestampParsingProfile(String profileName,
                                      String id,
                                      Map<NamedCaptureGroup, String> groups,
                                      String lineTemplateExpression) {
        this.profileId = id;
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
