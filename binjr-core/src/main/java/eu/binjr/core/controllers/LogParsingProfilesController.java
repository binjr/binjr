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

package eu.binjr.core.controllers;

import eu.binjr.core.data.indexes.parser.LogEventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;

public class LogParsingProfilesController extends ParsingProfilesController<ParsingProfile> {

    public LogParsingProfilesController(ParsingProfile[] builtinParsingProfiles,
                                        ParsingProfile[] userParsingProfiles,
                                        ParsingProfile defaultProfile,
                                        ParsingProfile selectedProfile) {
        super(builtinParsingProfiles, userParsingProfiles, defaultProfile, selectedProfile);
    }

    public LogParsingProfilesController(ParsingProfile[] builtinParsingProfiles,
                                        ParsingProfile[] userParsingProfiles,
                                        ParsingProfile defaultProfile,
                                        ParsingProfile selectedProfile,
                                        boolean allowTemporalCaptureGroupsOnly) {
        super(builtinParsingProfiles, userParsingProfiles, defaultProfile, selectedProfile, allowTemporalCaptureGroupsOnly);
    }

    @Override
    protected void doTest() throws Exception {
        var format = new LogEventFormat(profileComboBox.getValue(), ZoneId.systemDefault(), StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(StandardCharsets.UTF_8))) {
            var eventParser = format.parse(in);
            var events = new ArrayList<ParsedEvent<String>>();
            for (var parsed : eventParser) {
                if (parsed != null) {
                    events.add(parsed);
                }
            }
            if (events.size() == 0) {
                notifyWarn("No event found.");
            } else {
                notifyInfo(String.format("Found %d event(s).", events.size()));
            }
        }
        var hilitePattern = format.getProfile().getParsingRegex();
        testArea.setStyleSpans(0, highlightTextArea(hilitePattern, testArea.getText()));
    }


    @Override
    protected ParsingProfile updateProfile(String profileName, String profileId, Map groups, String lineExpression) {
        return new CustomParsingProfile(profileName, profileId, groups, lineExpression);
    }
}
