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

package eu.binjr.sources.csv.data.parsers;

import eu.binjr.common.javafx.controls.TextFieldValidator;
import eu.binjr.core.controllers.AbstractParsingProfilesController;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.preferences.DateFormat;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class CsvParsingProfilesController extends AbstractParsingProfilesController<CsvParsingProfile> {

    @FXML
    private Spinner<Integer> timeColumnTextField;
    @FXML
    private TextField delimiterTextField;
    @FXML
    private TableView<ParsedEvent> testResultTable;
    @FXML
    private TabPane testTabPane;
    @FXML
    private Tab inputTab;
    @FXML
    private Tab resultTab;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        delimiterTextField.textProperty().addListener((obs, oldText, newText) -> {
            resetTest();
        });

//        this.timeColumnTextField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999999, 1));

//        this.timeColumnTextField.setTextFormatter(new TextFormatter<Number>(new StringConverter<>() {
//            @Override
//            public String toString(Number object) {
//                if (object == null) {
//                    return "1";
//                }
//                return object.toString();
//            }
//
//            @Override
//            public Number fromString(String string) {
//                try {
//                    int gtz = Integer.parseInt(string);
//                    if (gtz <= 0) {
//                        TextFieldValidator.fail(timeColumnTextField, "Timestamp column position cannot be less than 1", true);
//                    }
//                    return gtz;
//                } catch (Exception e) {
//                    TextFieldValidator.fail(timeColumnTextField, "Invalid timestamp column value: " + e.getMessage(), true);
//                    return 1;
//                }
//            }
//        }));
    }

    @Override
    protected void loadParserParameters(CsvParsingProfile profile) {
        super.loadParserParameters(profile);
        this.delimiterTextField.setText(profile.getDelimiter());
        this.timeColumnTextField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999999, profile.getTimestampColumn() + 1));
//        this.timeColumnTextField.(Integer.toString(profile.getTimestampColumn() + 1));
        this.delimiterTextField.setDisable(profile.isBuiltIn());
        this.timeColumnTextField.setDisable(profile.isBuiltIn());
    }

    public CsvParsingProfilesController(CsvParsingProfile[] builtinParsingProfiles,
                                        CsvParsingProfile[] userParsingProfiles,
                                        CsvParsingProfile defaultProfile,
                                        CsvParsingProfile selectedProfile) {
        super(builtinParsingProfiles, userParsingProfiles, defaultProfile, selectedProfile);
    }

    public CsvParsingProfilesController(CsvParsingProfile[] builtinParsingProfiles,
                                        CsvParsingProfile[] userParsingProfiles,
                                        CsvParsingProfile defaultProfile,
                                        CsvParsingProfile selectedProfile,
                                        boolean allowTemporalCaptureGroupsOnly) {
        super(builtinParsingProfiles, userParsingProfiles, defaultProfile, selectedProfile, allowTemporalCaptureGroupsOnly);
    }

    @Override
    protected void handleOnClearTestArea(ActionEvent actionEvent) {
        testTabPane.getSelectionModel().select(inputTab);
        super.handleOnClearTestArea(actionEvent);
    }

    @Override
    protected void handleOnCopyTestArea(ActionEvent actionEvent) {
        super.handleOnCopyTestArea(actionEvent);
    }

    @Override
    protected void handleOnPasteToTestArea(ActionEvent actionEvent) {
        testTabPane.getSelectionModel().select(inputTab);
        super.handleOnPasteToTestArea(actionEvent);
    }

    @Override
    protected void handleOnRunTest(ActionEvent event) {
        super.handleOnRunTest(event);
        testTabPane.getSelectionModel().select(resultTab);
    }

    @Override
    protected void doTest() throws Exception {
        var format = new CsvEventFormat(profileComboBox.getValue(), ZoneId.systemDefault(), StandardCharsets.UTF_8);
        var eventParser = format.parse(new ByteArrayInputStream(testArea.getText().getBytes(StandardCharsets.UTF_8)));
        var events = new ArrayList<ParsedEvent>();
        for (var parsed : eventParser) {
            events.add(parsed);
        }
        if (events.size() == 0) {
            notifyWarn("No record found.");
        } else {
            var timeCol = new TableColumn<ParsedEvent, String>("Timestamp");
            timeCol.setCellValueFactory(param ->
                    new SimpleStringProperty(param.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]"))));
            testResultTable.getColumns().add(timeCol);
            events.get(0).getFields().forEach((name, value) -> {
                var col = new TableColumn<ParsedEvent, String>(name);
                col.setCellValueFactory(param ->
                        new SimpleStringProperty(formatToDouble(param.getValue().getFields().get(param.getTableColumn().getText()))));
                testResultTable.getColumns().add(col);
            });

            testResultTable.getItems().addAll(events);
            notifyInfo(String.format("Found %d record(s).", events.size()));
        }
    }

    @Override
    protected void resetTest() {
        super.resetTest();
        this.testResultTable.getItems().clear();
        this.testResultTable.getColumns().clear();
    }

    @Override
    protected CsvParsingProfile updateProfile(String profileName, String profileId, Map<NamedCaptureGroup, String> groups, String lineExpression) {
        return new CustomCsvParsingProfile(profileName,
                profileId,
                groups,
                lineExpression,
                this.delimiterTextField.getText(),
                this.timeColumnTextField.getValue() - 1,
                new int[0]);
    }

    private String formatToDouble(String value) {
        try {
            return Double.toString(Double.parseDouble(value));
        } catch (Exception e) {
            return "NaN";
        }
    }
}
