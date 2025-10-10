/*
 *    Copyright 2022-2025 Frederic Thevenet
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

import com.google.gson.reflect.TypeToken;
import eu.binjr.core.data.indexes.parser.LogEventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LogParsingProfilesController extends ParsingProfilesController<ParsingProfile> {

    public LogParsingProfilesController(ParsingProfile[] builtinParsingProfiles,
                                        ParsingProfile[] userParsingProfiles,
                                        ParsingProfile defaultProfile,
                                        ParsingProfile selectedProfile,
                                        Charset defaultCharset,
                                        ZoneId defaultZoneId) {
        super(builtinParsingProfiles,
                userParsingProfiles,
                defaultProfile,
                selectedProfile,
                defaultCharset,
                defaultZoneId);
    }

    public LogParsingProfilesController(ParsingProfile[] builtinParsingProfiles,
                                        ParsingProfile[] userParsingProfiles,
                                        ParsingProfile defaultProfile,
                                        ParsingProfile selectedProfile,
                                        boolean allowTemporalCaptureGroupsOnly,
                                        Charset defaultCharset,
                                        ZoneId defaultZoneId) {
        super(builtinParsingProfiles,
                userParsingProfiles,
                defaultProfile,
                selectedProfile,
                allowTemporalCaptureGroupsOnly,
                defaultCharset,
                defaultZoneId);
    }

    @Override
    protected ParsingFailureMode[] getSupportedUnparseableBehaviors() {
        return  ParsingFailureMode.values();
    }

    @Override
    protected List<ParsingProfile> deSerializeProfiles(String profileString) {
        Type profileListType = new TypeToken<ArrayList<CustomParsingProfile>>() {
        }.getType();
        return GSON.fromJson(profileString, profileListType);
    }

    @Override
    protected void doTest() throws Exception {
        var format = new LogEventFormat(profileComboBox.getValue(), getDefaultZoneId(),getDefaultCharset());
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(getDefaultCharset()))) {
            var eventParser = format.parse(in);
            var events = new ArrayList<ParsedEvent>();
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
    protected Optional<ParsingProfile> updateProfile(String profileName,
                                                     String profileId,
                                                     Map groups,
                                                     String lineExpression,
                                                     ParsingFailureMode onParsingFailure) {
        return Optional.of(new CustomParsingProfile(profileName, profileId, groups, lineExpression, onParsingFailure));
    }
}
