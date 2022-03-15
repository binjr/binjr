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

import com.google.gson.Gson;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CustomParsingProfile implements ParsingProfile {
    private Map<NamedCaptureGroup, String> captureGroups = new HashMap<>();
    private String lineTemplateExpression;
    private String profileName;
    private static final Gson GSON = new Gson();
    private final String profileId;

    private CustomParsingProfile() {
        profileId = "";
    }

    public CustomParsingProfile(String profileName,
                                Map<NamedCaptureGroup, String> captureGroups,
                                String lineTemplateExpression) {
        this(profileName, UUID.randomUUID().toString(), captureGroups, lineTemplateExpression);
    }

    public CustomParsingProfile(String profileName,
                                String profileId,
                                Map<NamedCaptureGroup,
                                        String> captureGroups,
                                String lineTemplateExpression) {
        this.profileName = profileName;
        this.profileId = profileId;
        this.captureGroups.putAll(captureGroups);
        this.lineTemplateExpression = lineTemplateExpression;
    }

    public static CustomParsingProfile empty() {
        return new CustomParsingProfile("New profile", new HashMap<>(), "");
    }

    public static CustomParsingProfile of(ParsingProfile profile) {
        if (profile == null) {
            return null;
        }
        return new CustomParsingProfile(profile.getProfileName(),
                profile.getProfileId(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression());
    }

    public static CustomParsingProfile copyOf(ParsingProfile profile) {
        Objects.requireNonNull(profile);
        return new CustomParsingProfile("Copy of " + profile.getProfileName(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression());
    }

    public static CustomParsingProfile fromJson(String jsonString) {
        return GSON.fromJson(jsonString, CustomParsingProfile.class);
    }

    public String toJson() {
        return GSON.toJson(this);
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

    public void setCaptureGroups(Map<NamedCaptureGroup, String> captureGroups) {
        this.captureGroups = captureGroups;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
    }

    public void setLineTemplateExpression(String lineTemplateExpression) {
        this.lineTemplateExpression = lineTemplateExpression;
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
