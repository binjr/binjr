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

public enum BuiltInOnlyLogParsingProfile implements ReadOnlyLogParsingProfile {
    BINJR_LOGS("binjr logs",
            "\\d{4}[\\/-]\\d{2}[\\/-]\\d{2}[\\-\\s]\\d{2}:\\d{2}:\\d{2}[\\.,]\\d{3}",
            true,
            " ",
            "[\\/-T_:\\.]",
            "yyyy MM dd HH mm ss SSS",
            "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL",
            "\\[(?<time>$TIMESTAMP)\\]\\s+\\[\\s?(?<severity>$SEVERITY)\\s?\\]\\s(?<message>$MESSAGE)"
    );


    private final String profileName;
    private final String timeCaptureExpression;
    private final boolean normalizeSepCheckBox;
    private final String separatorReplacementExpression;
    private final String separatorsToNormalizeExpression;
    private final String timeParsingExpression;
    private final String severityCaptureExpression;
    private final String lineTemplateExpression;

    BuiltInOnlyLogParsingProfile(String profileName,
            String timeCaptureExpression,
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
    public String getTimeCaptureExpression() {
        return timeCaptureExpression;
    }


    @Override
    public boolean isNormalizeSepCheckBox() {
        return normalizeSepCheckBox;
    }

    @Override
    public String getSeparatorReplacementExpression() {
        return separatorReplacementExpression;
    }

    @Override
    public String getSeparatorsToNormalizeExpression() {
        return separatorsToNormalizeExpression;
    }

    @Override
    public String getTimeParsingExpression() {
        return timeParsingExpression;
    }

    @Override
    public String getSeverityCaptureExpression() {
        return severityCaptureExpression;
    }

    @Override
    public String getLineTemplateExpression() {
        return lineTemplateExpression;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public String toString() {
        return "[Built-in] " + profileName;
    }
}
