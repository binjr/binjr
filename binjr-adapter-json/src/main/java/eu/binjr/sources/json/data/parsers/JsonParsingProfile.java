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

import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Locale;

public interface JsonParsingProfile extends ParsingProfile {

    List<JsonSeriesDefinition> getSeriesDefinitions();

    Locale getNumberFormattingLocale();

    boolean isContinueOnTimestampParsingFailure();

    record JsonSamplesDefinition(String name, String path, Color color) {
    }

    record JsonSeriesDefinition(String name,
                                String timeStampsPath,
                                ChartType graphType,
                                String unit,
                                UnitPrefixes prefix,
                                List<JsonSamplesDefinition> samples) {

    }
}
