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

import com.google.gson.annotations.JsonAdapter;
import eu.binjr.common.json.adapters.PatternJsonAdapter;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class CustomParsingProfile implements ParsingProfile {
    protected Map<NamedCaptureGroup, String> captureGroups;
    protected String lineTemplateExpression;
    protected String profileName;
    protected final String profileId;
    @JsonAdapter(PatternJsonAdapter.class)
    private final Pattern regex;

    public CustomParsingProfile() {
        this("", UUID.randomUUID().toString(), new HashMap<>(), "");
    }

    public CustomParsingProfile(String profileName,
                                String profileId,
                                Map<NamedCaptureGroup, String> captureGroups,
                                String lineTemplateExpression) {
        this.profileName = profileName;
        this.profileId = profileId;
        this.captureGroups = captureGroups;
        this.lineTemplateExpression = lineTemplateExpression;
        this.regex = Pattern.compile(buildParsingRegexString());
    }

    public static ParsingProfile of(ParsingProfile parsingProfile) {
        return new CustomParsingProfile(parsingProfile.getProfileName(),
                parsingProfile.getProfileId(),
                parsingProfile.getCaptureGroups(),
                parsingProfile.getLineTemplateExpression());
    }

    @Override
    public boolean isBuiltIn() {
        return false;
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
    public Pattern getParsingRegex() {
        return regex;
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomParsingProfile that = (CustomParsingProfile) o;
        return Objects.equals(profileId, that.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId);
    }

    @Override
    public String toString() {
        return profileName;
    }

}
