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

import com.google.gson.reflect.TypeToken;
import eu.binjr.common.javafx.controls.AlignedTableCellFactory;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.controllers.ParsingProfilesController;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.sources.json.adapters.JsonAdapterPreferences;
import eu.binjr.sources.json.adapters.JsonFileAdapter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.function.UnaryOperator;

public class JsonParsingProfilesController extends ParsingProfilesController<JsonParsingProfile> {


    @FXML
    private TableView<ParsedEvent> testResultTable;
    @FXML
    private TabPane testTabPane;
    @FXML
    private Tab inputTab;
    @FXML
    private Tab resultTab;
    @FXML
    private CheckBox continueOnTSErrorCheckbox;

    private final UnaryOperator<TextFormatter.Change> clampToSingleChar = c -> {
        if (c.isContentChange()) {
            String newText = c.getControlNewText();
            int newLength = newText.length();
            if (newLength > 1 && (newText.charAt(0) != '\\' || !List.of('t', 'r', 'n').contains(newText.charAt(1)) || newLength > 2)) {
                String tail = newText.substring(newLength - 1, newLength);
                c.setText(tail);
                int oldLength = c.getControlText().length();
                c.setRange(0, oldLength);
            }
        }
        return c;
    };
    private NumberFormat numberFormat = NumberFormat.getNumberInstance();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        continueOnTSErrorCheckbox.selectedProperty().addListener(observable -> resetTest());
    }

    @Override
    protected void loadParserParameters(JsonParsingProfile profile) {
        super.loadParserParameters(profile);
        this.continueOnTSErrorCheckbox.setSelected(profile.isContinueOnTimestampParsingFailure());
    }

    public record ColumnPosition(int index) {
        @Override
        public String toString() {
            return (index < 0) ? "line numbers" : StringUtils.integerToOrdinal(index + 1) + " column";
        }
    }

    public static class ColumnPositionFactory extends SpinnerValueFactory<ColumnPosition> {
        private final int minValue;
        private final int maxValue;
        private int index = 0;

        public ColumnPositionFactory(int minValue, int maxValue, int initialValue) {
            if (initialValue > maxValue) {
                throw new IllegalArgumentException("Initial value is above maximum value");
            }
            if (initialValue < minValue) {
                throw new IllegalArgumentException("Initial value is below minimum value");
            }
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.index = initialValue;
            setValue(new ColumnPosition(index));
        }

        @Override
        public void decrement(int steps) {
            int newPos = index - steps;
            if (newPos >= minValue) {
                this.index = newPos;
                setValue(new ColumnPosition(index));
            }
        }

        @Override
        public void increment(int steps) {
            int newPos = index + steps;
            if (newPos <= maxValue) {
                this.index = newPos;
                setValue(new ColumnPosition(index));
            }
        }
    }

    public JsonParsingProfilesController(JsonParsingProfile[] builtinParsingProfiles,
                                         JsonParsingProfile[] userParsingProfiles,
                                         JsonParsingProfile defaultProfile,
                                         JsonParsingProfile selectedProfile,
                                         Charset defaultCharset,
                                         ZoneId defaultZoneId) throws NoAdapterFoundException {
        this(builtinParsingProfiles,
                userParsingProfiles,
                defaultProfile,
                selectedProfile,
                true,
                defaultCharset,
                defaultZoneId);
    }

    public JsonParsingProfilesController(JsonParsingProfile[] builtinParsingProfiles,
                                         JsonParsingProfile[] userParsingProfiles,
                                         JsonParsingProfile defaultProfile,
                                         JsonParsingProfile selectedProfile,
                                         boolean allowTemporalCaptureGroupsOnly,
                                         Charset defaultCharset,
                                         ZoneId defaultZoneId) throws NoAdapterFoundException {
        super(builtinParsingProfiles,
                userParsingProfiles,
                defaultProfile,
                selectedProfile,
                allowTemporalCaptureGroupsOnly,
                defaultCharset,
                defaultZoneId);
        JsonAdapterPreferences prefs = (JsonAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JsonFileAdapter.class.getName());
    }

    @Override
    protected void handleOnRunTest(ActionEvent event) {
        super.handleOnRunTest(event);
        testTabPane.getSelectionModel().select(resultTab);
    }

    @Override
    protected Optional<List<FileChooser.ExtensionFilter>> additionalExtensions() {
        return Optional.of(List.of(new FileChooser.ExtensionFilter("JSON files", "*.json")));
    }

    @Override
    protected List<JsonParsingProfile> deSerializeProfiles(String profileString) {
        Type profileListType = new TypeToken<ArrayList<CustomJsonParsingProfile>>() {
        }.getType();
        return gson.fromJson(profileString, profileListType);
    }

    @Override
    protected void doTest() throws Exception {

    }

    private TableColumn<ParsedEvent, String> makeColumn(Map<TableColumn, String> colMap,
                                                        TextAlignment alignment,
                                                        int i,
                                                        int tsColumn,
                                                        String name) {
        var col = new TableColumn<ParsedEvent, String>(name);
        col.setStyle("-fx-font-weight: normal;");
        col.setSortable(false);
        col.setReorderable(false);
        var cellFactory = new AlignedTableCellFactory<ParsedEvent, String>();
        cellFactory.setAlignment(alignment);
        col.setCellFactory(cellFactory);
        colMap.put(col, Integer.toString(i));
        return col;
    }

    @Override
    protected void resetTest() {
        super.resetTest();
        this.testResultTable.getItems().clear();
        this.testResultTable.getColumns().clear();
        testTabPane.getSelectionModel().select(inputTab);
    }

    @Override
    protected Optional<JsonParsingProfile> updateProfile(String profileName, String profileId, Map<NamedCaptureGroup, String> groups, String lineExpression) {
        List<String> errors = new ArrayList<>();

        return Optional.of(new CustomJsonParsingProfile(profileName,
                profileId,
                groups,
                lineExpression,
                this.continueOnTSErrorCheckbox.isSelected(),
                null));
    }

}
