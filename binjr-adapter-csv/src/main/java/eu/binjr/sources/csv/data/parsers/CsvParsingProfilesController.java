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

import eu.binjr.common.javafx.controls.AlignedTableCellFactory;
import eu.binjr.common.javafx.controls.TextFieldValidator;
import eu.binjr.core.controllers.ParsingProfilesController;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;
import org.controlsfx.control.textfield.TextFields;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;

public class CsvParsingProfilesController extends ParsingProfilesController<CsvParsingProfile> {

    @FXML
    private Spinner<Integer> timeColumnTextField;
    @FXML
    private TextField delimiterTextField;
    @FXML
    private TextField quoteCharacterTextField;
    @FXML
    private TableView<ParsedEvent<Double>> testResultTable;
    @FXML
    private TabPane testTabPane;
    @FXML
    private Tab inputTab;
    @FXML
    private Tab resultTab;
    @FXML
    private CheckBox readColumnNameCheckBox;
    @FXML
    private TextField parsingLocaleTextField;

    private final UnaryOperator<TextFormatter.Change> clampToSingleChar = c -> {
        if (c.isContentChange()) {
            int newLength = c.getControlNewText().length();
            if (newLength > 1) {
                String tail = c.getControlNewText().substring(newLength - 1, newLength);
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
        delimiterTextField.textProperty().addListener((observable) -> resetTest());
        timeColumnTextField.valueProperty().addListener(observable -> resetTest());
        readColumnNameCheckBox.selectedProperty().addListener(observable -> resetTest());
        TextFields.bindAutoCompletion(parsingLocaleTextField,
                Arrays.stream(Locale.getAvailableLocales()).map(Locale::toLanguageTag).toList());
        delimiterTextField.setTextFormatter(new TextFormatter<>(clampToSingleChar));
        quoteCharacterTextField.setTextFormatter(new TextFormatter<>(clampToSingleChar));
    }

    @Override
    protected void loadParserParameters(CsvParsingProfile profile) {
        super.loadParserParameters(profile);
        this.delimiterTextField.setText(profile.getDelimiter());
        this.quoteCharacterTextField.setText(String.valueOf(profile.getQuoteCharacter()));
        this.timeColumnTextField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999999, profile.getTimestampColumn() + 1));
        this.readColumnNameCheckBox.setSelected(profile.isReadColumnNames());
        this.parsingLocaleTextField.setText(profile.getNumberFormattingLocale().toLanguageTag());
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
                var cellFactory = new AlignedTableCellFactory<ParsedEvent<Double>, String>();
                cellFactory.setAlignment(TextAlignment.RIGHT);
                for (int i = 0; i < headers.size(); i++) {
                    String name = headers.get(i);
                    var col = new TableColumn<ParsedEvent<Double>, String>(name);
                    colMap.put(col, Integer.toString(i));
                    if (i == format.getProfile().getTimestampColumn()) {
                        col.setCellFactory(cellFactory);
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(param.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]"))));
                    } else {
                        col.setCellFactory(cellFactory);
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
    protected Optional<CsvParsingProfile> updateProfile(String profileName, String profileId, Map<NamedCaptureGroup, String> groups, String lineExpression) {
        List<String> errors = new ArrayList<>();
        if (this.lineTemplateExpression.getText().isBlank()) {
            TextFieldValidator.fail(delimiterTextField, true);
            errors.add("Timestamp pattern cannot be empty");
        }
        if (this.delimiterTextField.getText().isEmpty()) {
            TextFieldValidator.fail(delimiterTextField, true);
            errors.add("Delimiting character for CSV parsing cannot be empty");
        }
        if (this.quoteCharacterTextField.getText().isBlank()) {
            TextFieldValidator.fail(quoteCharacterTextField, true);
            errors.add("Quote character for CSV parsing cannot be empty");
        }

        try {
            var bld = new Locale.Builder();
            bld.setLanguageTag(parsingLocaleTextField.getText());
            var parsingLocale = bld.build();
            this.numberFormat = NumberFormat.getNumberInstance(parsingLocale);
        } catch (IllformedLocaleException e) {
            errors.add("The locale for number parsing is invalid: " + e.getMessage());
        }
        if (errors.size() > 0) {
            notifyError(String.join("\n", errors));
            return Optional.empty();
        }

        return Optional.of(new CustomCsvParsingProfile(profileName,
                profileId,
                groups,
                lineExpression,
                this.delimiterTextField.getText(),
                this.quoteCharacterTextField.getText().charAt(0),
                this.timeColumnTextField.getValue() - 1,
                new int[0],
                this.readColumnNameCheckBox.isSelected(),
                Locale.forLanguageTag(parsingLocaleTextField.getText())));
    }

    private String formatToDouble(Double value) {
        if (value != null) {
            try {
                return numberFormat.format(value);
            } catch (Exception e) {
                // Do nothing
            }
        }
        return "NaN";
    }
}
