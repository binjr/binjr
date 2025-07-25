/*
 *    Copyright 2020-2025 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.preferences.TemporalAnchor;
import eu.binjr.core.preferences.UserPreferences;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Pattern;

public interface ParsingProfile {
    Logger logger = Logger.create(ParsingProfile.class);
    Pattern GROUP_TAG_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]{2,}");

    boolean isBuiltIn();

    String getProfileId();

    String getProfileName();

    Map<NamedCaptureGroup, String> getCaptureGroups();

    String getLineTemplateExpression();

    Pattern getParsingRegex();

    default String buildParsingRegexString() {
        var regexString = new String[]{getLineTemplateExpression()};
        var matcher = GROUP_TAG_PATTERN.matcher(regexString[0]);
        while (matcher.find()) {
            var value = matcher.group();
            getCaptureGroups().entrySet().stream()
                    .filter(e -> CaptureGroup.of(value).equals(e.getKey()))
                    .map(e -> String.format("(?<%s>%s)", e.getKey().name(), e.getValue()))
                    .findAny().ifPresent(r -> regexString[0] = regexString[0].replace(value, r));
        }
        logger.trace(() -> "Regex string for profile " + getProfileName() + ": " + regexString[0]);
        return regexString[0];
    }

    default ZonedDateTime parseDateTime(String text, ZoneId zoneId) {
        var m = getParsingRegex().matcher(text);
        ZonedDateTime timestamp = ZonedDateTime.of(getTemporalAnchor().resolve(), zoneId);
        if (m.find()) {
            for (Map.Entry<NamedCaptureGroup, String> entry : getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var parsed = m.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    if (captureGroup instanceof TemporalCaptureGroup temporalGroup) {
                        timestamp = timestamp.with(temporalGroup.getMapping(), temporalGroup.parseLong(parsed));
                    }
                }
            }
            return timestamp;
        }
        return null;
    }

    default TemporalAnchor getTemporalAnchor() {
        return UserPreferences.getInstance().defaultDateTimeAnchor.get();
    }

}

