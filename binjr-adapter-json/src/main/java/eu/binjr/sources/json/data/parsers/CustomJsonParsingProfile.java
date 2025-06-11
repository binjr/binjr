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

import com.google.gson.annotations.JsonAdapter;
import eu.binjr.common.json.adapters.LocaleJsonAdapter;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;

import java.util.*;

public class CustomJsonParsingProfile extends CustomParsingProfile implements JsonParsingProfile {

    @JsonAdapter(LocaleJsonAdapter.class)
    private final Locale formattingLocale;
    private final boolean continueOnTimestampParsingFailure;

    public CustomJsonParsingProfile() {
        this("", UUID.randomUUID().toString(), new HashMap<>(), "", Locale.getDefault(), false);
    }


    public static JsonParsingProfile of(JsonParsingProfile parsingProfile) {
        return new CustomJsonParsingProfile(parsingProfile.getProfileName(),
                parsingProfile.getProfileId(),
                parsingProfile.getCaptureGroups(),
                parsingProfile.getLineTemplateExpression(),
                parsingProfile.getNumberFormattingLocale(),
                parsingProfile.isContinueOnTimestampParsingFailure()
        );
    }


    public CustomJsonParsingProfile(String profileName,
                                    String profileId,
                                    Map<NamedCaptureGroup, String> captureGroups,
                                    String lineTemplateExpression,
                                    Locale formattingLocale,
                                    boolean continueOnTimestampParsingFailure) {
        super(profileName, profileId, captureGroups, lineTemplateExpression);
        this.formattingLocale = formattingLocale;
        this.continueOnTimestampParsingFailure = continueOnTimestampParsingFailure;
    }


    @Override
    public List<JsonSeriesDefinition> getSeriesDefinitions() {
        return List.of();
    }

    @Override
    public Locale getNumberFormattingLocale() {
        return formattingLocale;
    }


    @Override
    public boolean isContinueOnTimestampParsingFailure() {
        return this.continueOnTimestampParsingFailure;
    }
}
