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

import eu.binjr.core.controllers.AbstractParsingProfilesController;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

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
    @FXML
    private CheckBox readColumnNameCheckBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        delimiterTextField.textProperty().addListener((observable) -> resetTest());
        timeColumnTextField.valueProperty().addListener(observable -> resetTest());
        readColumnNameCheckBox.selectedProperty().addListener(observable -> resetTest());
    }

    @Override
    protected void loadParserParameters(CsvParsingProfile profile) {
        super.loadParserParameters(profile);
        this.delimiterTextField.setText(profile.getDelimiter());
        this.timeColumnTextField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999999, profile.getTimestampColumn() + 1));
        this.readColumnNameCheckBox.setSelected(profile.isReadColumnNames());
        this.readColumnNameCheckBox.setDisable(profile.isBuiltIn());
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
    protected void handleOnRunTest(ActionEvent event) {
        super.handleOnRunTest(event);
        testTabPane.getSelectionModel().select(resultTab);
    }

    @Override
    protected void doTest() throws Exception {
        var format = new CsvEventFormat(profileComboBox.getValue(), ZoneId.systemDefault(), StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(StandardCharsets.UTF_8))) {
            var headers = format.getDataColumnHeaders(in);
            if (headers.size() == 0) {
                notifyWarn("No record found.");
            } else {
                Map<TableColumn, String> colMap = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String name  = headers.get(i);
                    var col = new TableColumn<ParsedEvent, String>(name);
                    colMap.put(col, Integer.toString(i));
                    if (i == format.getProfile().getTimestampColumn()){
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(param.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]"))));
                    }else {
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(formatToDouble(param.getValue().getFields().get(colMap.get(param.getTableColumn())))));
                    }
                    testResultTable.getColumns().add(col);
                }
            }
        }
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(StandardCharsets.UTF_8))) {
            var eventParser = format.parse(in);
            for (var parsed : eventParser) {
                testResultTable.getItems().add(parsed);
            }
            notifyInfo(String.format("Found %d record(s).", testResultTable.getItems().size()));
        }
    }

    @Override
    protected void resetTest() {
        super.resetTest();
        this.testResultTable.getItems().clear();
        this.testResultTable.getColumns().clear();
        testTabPane.getSelectionModel().select(inputTab);
    }

    @Override
    protected CsvParsingProfile updateProfile(String profileName, String profileId, Map<NamedCaptureGroup, String> groups, String lineExpression) {
        return new CustomCsvParsingProfile(profileName,
                profileId,
                groups,
                lineExpression,
                this.delimiterTextField.getText(),
                this.timeColumnTextField.getValue() - 1,
                new int[0],
                this.readColumnNameCheckBox.isSelected());
    }

    private String formatToDouble(String value) {
        try {
            return Double.toString(Double.parseDouble(value));
        } catch (Exception e) {
            return "NaN";
        }
    }
}
