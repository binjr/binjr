/*
 *    Copyright 2020 Frederic Thevenet
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

public class CustomParsingProfile implements ParsingProfile {
    private  Map<NamedCaptureGroup, String> captureGroups = new HashMap<>();
    private String lineTemplateExpression;
    private String profileName;

    public CustomParsingProfile(String profileName,
                                Map<NamedCaptureGroup, String> captureGroups,
                                String lineTemplateExpression) {
        this.profileName = profileName;

        this.captureGroups.putAll(captureGroups);
        this.lineTemplateExpression = lineTemplateExpression;
    }

    public static CustomParsingProfile empty() {
        return new CustomParsingProfile("New profile", new HashMap<>(), "");
    }

    public static CustomParsingProfile of(ParsingProfile profile) {
        return new CustomParsingProfile("Copy of " + profile.getProfileName(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression());
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
    public String toString() {
        return profileName;
    }
}
