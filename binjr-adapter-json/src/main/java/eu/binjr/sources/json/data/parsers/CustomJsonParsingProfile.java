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

import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;

import java.util.*;

public class CustomJsonParsingProfile extends CustomParsingProfile implements JsonParsingProfile {
    private final JsonDefinition jsonDefinition;

    public CustomJsonParsingProfile() {
        this("", UUID.randomUUID().toString(), new HashMap<>(), "", ParsingFailureMode.ABORT, null);
    }


    public static JsonParsingProfile of(JsonParsingProfile parsingProfile) {
        return new CustomJsonParsingProfile(parsingProfile.getProfileName(),
                parsingProfile.getProfileId(),
                parsingProfile.getCaptureGroups(),
                parsingProfile.getLineTemplateExpression(),
                parsingProfile.onParsingFailure(),
                parsingProfile.getJsonDefinition()
        );
    }


    public CustomJsonParsingProfile(String profileName,
                                    String profileId,
                                    Map<NamedCaptureGroup, String> captureGroups,
                                    String lineTemplateExpression,
                                    ParsingFailureMode onParsingFailure,
                                    JsonDefinition jsonDefinition) {
        super(profileName, profileId, captureGroups, lineTemplateExpression, onParsingFailure);
        this.jsonDefinition = jsonDefinition;
    }


    @Override
    public JsonDefinition getJsonDefinition() {
        return this.jsonDefinition;
    }

}
