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
import eu.binjr.common.javafx.controls.ToolButtonBuilder;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.controllers.ParsingProfilesController;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.sources.csv.adapters.CsvAdapterPreferences;
import eu.binjr.sources.csv.adapters.CsvFileAdapter;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import org.controlsfx.control.textfield.TextFields;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;

public class CsvParsingProfilesController extends ParsingProfilesController<CsvParsingProfile> {

    @FXML
    private Spinner<ColumnPosition> timeColumnTextField;
    @FXML
    private TextField delimiterTextField;
    @FXML
    private TextField quoteCharacterTextField;
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
    private final CsvAdapterPreferences prefs;

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
        this.timeColumnTextField.setValueFactory(new ColumnPositionFactory(-1, 999999, profile.getTimestampColumn()));
        this.readColumnNameCheckBox.setSelected(profile.isReadColumnNames());
        this.parsingLocaleTextField.setText(profile.getNumberFormattingLocale().toLanguageTag());
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

    public CsvParsingProfilesController(CsvParsingProfile[] builtinParsingProfiles,
                                        CsvParsingProfile[] userParsingProfiles,
                                        CsvParsingProfile defaultProfile,
                                        CsvParsingProfile selectedProfile,
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

    public CsvParsingProfilesController(CsvParsingProfile[] builtinParsingProfiles,
                                        CsvParsingProfile[] userParsingProfiles,
                                        CsvParsingProfile defaultProfile,
                                        CsvParsingProfile selectedProfile,
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
        this.prefs = (CsvAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(CsvFileAdapter.class.getName());
    }

    @Override
    protected void handleOnRunTest(ActionEvent event) {
        super.handleOnRunTest(event);
        testTabPane.getSelectionModel().select(resultTab);
    }

    @Override
    protected Optional<List<FileChooser.ExtensionFilter>> additionalExtensions() {
        return Optional.of(List.of(new FileChooser.ExtensionFilter("Comma-separated values files", "*.csv")));
    }

    @Override
    protected void doTest() throws Exception {
        var format = new CsvEventFormat(profileComboBox.getValue(), getDefaultZoneId(), getDefaultCharset());
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(getDefaultCharset()))) {
            var headers = format.getDataColumnHeaders(in);
            if (headers.size() == 0) {
                notifyWarn("No record found.");
            } else {
                Map<TableColumn, String> colMap = new HashMap<>();
                var cellFactory = new AlignedTableCellFactory<ParsedEvent, String>();
                cellFactory.setAlignment(TextAlignment.RIGHT);
                for (int i = 0; i < headers.size(); i++) {
                    String name = headers.get(i);
                    var col = new TableColumn<ParsedEvent, String>(name);
                    col.setStyle("-fx-font-weight: normal;");
                    var isTimeColCtrl = new ToolButtonBuilder<ToggleButton>()
                            .setText("")
                            .setTooltip("Extract timestamp from this column")
                            .setStyleClass("dialog-button")
                            .setIconStyleClass("time-icon", "small-icon")
                            .build(ToggleButton::new);
                    isTimeColCtrl.setUserData(new ColumnPosition(i));
                    col.setGraphic(isTimeColCtrl);
                    col.setSortable(false);
                    col.setReorderable(false);
                    colMap.put(col, Integer.toString(i));
                    if (i == format.getProfile().getTimestampColumn()) {
                        isTimeColCtrl.setSelected(true);
                        col.setCellFactory(cellFactory);
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(param.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]"))));
                    } else {
                        col.setCellFactory(cellFactory);
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(formatToDouble(param.getValue().getFields().get(colMap.get(param.getTableColumn())))));
                    }
                    testResultTable.getColumns().add(col);
                    isTimeColCtrl.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            if (isTimeColCtrl.getUserData() instanceof ColumnPosition pos) {
                                this.timeColumnTextField.setValueFactory(new ColumnPositionFactory(-1, 999999, pos.index()));
                                handleOnRunTest(null);
                            }
                        }else {
                            this.timeColumnTextField.setValueFactory(new ColumnPositionFactory(-1, 999999, -1));
                            handleOnRunTest(null);
                        }
                    });
                }
            }
        }
        try (InputStream in = new ByteArrayInputStream(testArea.getText().getBytes(getDefaultCharset()))) {
            EventParser eventParser = format.parse(in);
            for (ParsedEvent parsed : eventParser) {
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
                this.timeColumnTextField.getValue().index(),
                new int[0],
                this.readColumnNameCheckBox.isSelected(),
                Locale.forLanguageTag(parsingLocaleTextField.getText())));
    }

    private String formatToDouble(String value) {
        if (value != null) {
            try {
                return numberFormat.format(numberFormat.parse(value));
            } catch (Exception e) {
                // Do nothing
            }
        }
        return "NaN";
    }

}
