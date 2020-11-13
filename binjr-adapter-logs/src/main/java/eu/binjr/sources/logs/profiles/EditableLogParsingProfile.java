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

package eu.binjr.sources.logs.profiles;

public class EditableLogParsingProfile implements ReadOnlyLogParsingProfile {

    private String timeCaptureExpression;
    private boolean normalizeSepCheckBox;
    private String separatorReplacementExpression;
    private String separatorsToNormalizeExpression;
    private String timeParsingExpression;
    private String severityCaptureExpression;
    private String lineTemplateExpression;
    private String profileName;

    public static EditableLogParsingProfile empty(){
        return new EditableLogParsingProfile("New profile", "",false,"","","","","");
    }

    public static EditableLogParsingProfile of(ReadOnlyLogParsingProfile rules) {
        return new EditableLogParsingProfile("Copy of " + rules.getProfileName(), rules.getTimeCaptureExpression(),
                rules.isNormalizeSepCheckBox(),
                rules.getSeparatorReplacementExpression(),
                rules.getSeparatorsToNormalizeExpression(),
                rules.getTimeParsingExpression(),
                rules.getSeverityCaptureExpression(),
                rules.getLineTemplateExpression());
    }

    public EditableLogParsingProfile(String profileName, String timeCaptureExpression,
                                     boolean normalizeSepCheckBox,
                                     String separatorReplacementExpression,
                                     String separatorsToNormalizeExpression,
                                     String timeParsingExpression,
                                     String severityCaptureExpression,
                                     String lineTemplateExpression) {
        this.profileName = profileName;
        this.timeCaptureExpression = timeCaptureExpression;
        this.normalizeSepCheckBox = normalizeSepCheckBox;
        this.separatorReplacementExpression = separatorReplacementExpression;
        this.separatorsToNormalizeExpression = separatorsToNormalizeExpression;
        this.timeParsingExpression = timeParsingExpression;
        this.severityCaptureExpression = severityCaptureExpression;
        this.lineTemplateExpression = lineTemplateExpression;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String getTimeCaptureExpression() {
        return timeCaptureExpression;
    }

    public void setTimeCaptureExpression(String timeCaptureExpression) {
        this.timeCaptureExpression = timeCaptureExpression;
    }

    @Override
    public boolean isNormalizeSepCheckBox() {
        return normalizeSepCheckBox;
    }

    public void setNormalizeSepCheckBox(boolean normalizeSepCheckBox) {
        this.normalizeSepCheckBox = normalizeSepCheckBox;
    }

    @Override
    public String getSeparatorReplacementExpression() {
        return separatorReplacementExpression;
    }

    public void setSeparatorReplacementExpression(String separatorReplacementExpression) {
        this.separatorReplacementExpression = separatorReplacementExpression;
    }

    @Override
    public String getSeparatorsToNormalizeExpression() {
        return separatorsToNormalizeExpression;
    }

    public void setSeparatorsToNormalizeExpression(String separatorsToNormalizeExpression) {
        this.separatorsToNormalizeExpression = separatorsToNormalizeExpression;
    }

    @Override
    public String getTimeParsingExpression() {
        return timeParsingExpression;
    }

    public void setTimeParsingExpression(String timeParsingExpression) {
        this.timeParsingExpression = timeParsingExpression;
    }

    @Override
    public String getSeverityCaptureExpression() {
        return severityCaptureExpression;
    }

    public void setSeverityCaptureExpression(String severityCaptureExpression) {
        this.severityCaptureExpression = severityCaptureExpression;
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
