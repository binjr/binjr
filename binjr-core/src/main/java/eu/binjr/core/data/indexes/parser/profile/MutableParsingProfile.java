/*
 *    Copyright 2020-2022 Frederic Thevenet
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

import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class MutableParsingProfile extends CustomParsingProfile implements ParsingProfile {

    private MutableParsingProfile() {
       super();
    }

    public MutableParsingProfile(String profileName,
                                 Map<NamedCaptureGroup, String> captureGroups,
                                 String lineTemplateExpression) {
        this(profileName, UUID.randomUUID().toString(), captureGroups, lineTemplateExpression);
    }

    public MutableParsingProfile(String profileName,
                                 String profileId,
                                 Map<NamedCaptureGroup,
                                        String> captureGroups,
                                 String lineTemplateExpression) {
        super(profileName, profileId, captureGroups, lineTemplateExpression);
    }

    public static MutableParsingProfile empty() {
        return new MutableParsingProfile("New profile", new HashMap<>(), "");
    }

    public static MutableParsingProfile of(ParsingProfile profile) {
        if (profile == null) {
            return null;
        }
        return new MutableParsingProfile(profile.getProfileName(),
                profile.getProfileId(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression());
    }

    public static MutableParsingProfile copyOf(ParsingProfile profile) {
        Objects.requireNonNull(profile);
        return new MutableParsingProfile("Copy of " + profile.getProfileName(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression());
    }



    public void setCaptureGroups(Map<NamedCaptureGroup, String> captureGroups) {
        this.captureGroups = captureGroups;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void setLineTemplateExpression(String lineTemplateExpression) {
        this.lineTemplateExpression = lineTemplateExpression;
    }

    @Override
    public Pattern getParsingRegex() {
        return Pattern.compile(buildParsingRegexString());
    }
}
