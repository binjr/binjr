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

package eu.binjr.sources.csv.data.parsers;

import com.google.gson.reflect.TypeToken;
import eu.binjr.common.javafx.controls.AlignedTableCellFactory;
import eu.binjr.common.javafx.controls.CodeAreaValidator;
import eu.binjr.common.javafx.controls.TextFieldValidator;
import eu.binjr.common.javafx.controls.ToolButtonBuilder;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.controllers.ParsingProfilesController;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;
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
import java.lang.reflect.Type;
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
    private TextField commentTextField;
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
    private CheckBox overrideParsingLocaleCheckBox;
    @FXML
    private TextField parsingLocaleTextField;
    @FXML
    private CheckBox trimCellsCheckbox;

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
    private CsvAdapterPreferences adapterPrefs;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        //    onParseFailureChoiceBox.getItems().setAll(UnparseableLinesBehavior.ABORT,UnparseableLinesBehavior.IGNORE);
        delimiterTextField.textProperty().addListener((observable) -> resetTest());
        commentTextField.textProperty().addListener(observable -> resetTest());
        timeColumnTextField.valueProperty().addListener(observable -> resetTest());
        readColumnNameCheckBox.selectedProperty().addListener(observable -> resetTest());
        overrideParsingLocaleCheckBox.selectedProperty().addListener(observable -> resetTest());
        trimCellsCheckbox.selectedProperty().addListener(observable -> resetTest());
        onParseFailureChoiceBox.getSelectionModel().selectedItemProperty().addListener(observable -> resetTest());
        TextFields.bindAutoCompletion(parsingLocaleTextField,
                Arrays.stream(Locale.getAvailableLocales()).map(Locale::toLanguageTag).toList());
        parsingLocaleTextField.disableProperty().bind(overrideParsingLocaleCheckBox.selectedProperty().not());
        delimiterTextField.setTextFormatter(new TextFormatter<>(clampToSingleChar));
        commentTextField.setTextFormatter(new TextFormatter<>(clampToSingleChar));
        quoteCharacterTextField.setTextFormatter(new TextFormatter<>(clampToSingleChar));
        try {
            this.adapterPrefs = (CsvAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(CsvFileAdapter.class.getName());
        } catch (NoAdapterFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ParsingFailureMode[] getSupportedUnparseableBehaviors() {
        return new ParsingFailureMode[]{ParsingFailureMode.ABORT, ParsingFailureMode.IGNORE};
    }

    @Override
    protected void loadParserParameters(CsvParsingProfile profile) {
        super.loadParserParameters(profile);
        this.delimiterTextField.setText(profile.getDelimiter());
        this.quoteCharacterTextField.setText(StringUtils.CharacterToString(profile.getQuoteCharacter()));
        this.timeColumnTextField.setValueFactory(new ColumnPositionFactory(-1, 999999, profile.getTimestampColumn()));
        this.readColumnNameCheckBox.setSelected(profile.isReadColumnNames());
        this.overrideParsingLocaleCheckBox.setSelected(profile.isOverrideParsingLocale());
        this.parsingLocaleTextField.setText(profile.getNumberFormattingLocale().toLanguageTag());
        this.trimCellsCheckbox.setSelected(profile.isTrimCellValues());
        this.onParseFailureChoiceBox.getSelectionModel().select(profile.onParsingFailure());
        this.commentTextField.setText(StringUtils.CharacterToString(profile.getCommentMarker()));
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
        CsvAdapterPreferences prefs = (CsvAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(CsvFileAdapter.class.getName());
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
    protected List<CsvParsingProfile> deSerializeProfiles(String profileString) {
        Type profileListType = new TypeToken<ArrayList<CustomCsvParsingProfile>>() {
        }.getType();
        return GSON.fromJson(profileString, profileListType);
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
                var tsColNum = format.getProfile().getTimestampColumn();
                // Add a column for line numbers
                var lineNbColumn = makeColumn(colMap, TextAlignment.CENTER, -1, tsColNum, "#");
                lineNbColumn.setCellValueFactory(param ->
                        new SimpleStringProperty(Long.toString(param.getValue().getSequence())));
                lineNbColumn.getStyleClass().add("line-number-column");
                testResultTable.getColumns().add(lineNbColumn);
                for (int i = 0; i < headers.size(); i++) {
                    String name = headers.get(i);
                    TableColumn<ParsedEvent, String> col = makeColumn(colMap, TextAlignment.RIGHT, i, tsColNum, name);
                    if (i == tsColNum) {
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(param.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]"))));
                    } else {
                        col.setCellValueFactory(param ->
                                new SimpleStringProperty(formatToDouble(param.getValue().getTextFields().get(colMap.get(param.getTableColumn())))));
                    }
                    testResultTable.getColumns().add(col);
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

    private TableColumn<ParsedEvent, String> makeColumn(Map<TableColumn, String> colMap,
                                                        TextAlignment alignment,
                                                        int i,
                                                        int tsColumn,
                                                        String name) {
        var col = new TableColumn<ParsedEvent, String>(name);
        col.setStyle("-fx-font-weight: normal;");
        var isTimeColCtrl = new ToolButtonBuilder<ToggleButton>()
                .setText("")
                .setTooltip("Extract timestamp from this column")
                .setStyleClass("dialog-button")
                .setAction(event -> {
                    var btn = (ToggleButton) event.getSource();
                    if (btn.getUserData() instanceof ColumnPosition pos) {
                        this.timeColumnTextField.setValueFactory(new ColumnPositionFactory(-1, 999999, pos.index()));
                        handleOnRunTest(null);
                    }
                })
                .setIconStyleClass("time-icon", "small-icon")
                .build(ToggleButton::new);
        isTimeColCtrl.setUserData(new ColumnPosition(i));
        isTimeColCtrl.setSelected(i == tsColumn);
        col.setGraphic(isTimeColCtrl);
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
    protected Optional<CsvParsingProfile> updateProfile(String profileName,
                                                        String profileId,
                                                        Map<NamedCaptureGroup, String> groups,
                                                        String lineExpression,
                                                        ParsingFailureMode onParsingFailure) {
        List<String> errors = new ArrayList<>();
        if (this.lineTemplateExpression.getText().isBlank()) {
            CodeAreaValidator.fail(lineTemplateExpression, true);
            errors.add("Timestamp pattern cannot be empty");
        }
        if (this.delimiterTextField.getText().isEmpty()) {
            TextFieldValidator.fail(delimiterTextField, true);
            errors.add("Delimiting character for CSV parsing cannot be empty");
        }
        try {
            var parsingLocale = Locale.forLanguageTag((parsingLocaleTextField.getText()));
            this.numberFormat = NumberFormat.getNumberInstance(parsingLocale);
            this.numberFormat.setMaximumFractionDigits(adapterPrefs.NumberFormatMaxFactionDigits.get().intValue());
        } catch (IllformedLocaleException e) {
            errors.add("The locale for number parsing is invalid: " + e.getMessage());
        }
        if (!errors.isEmpty()) {
            notifyError(String.join("\n", errors));
            return Optional.empty();
        }

        return Optional.of(new CustomCsvParsingProfile(profileName,
                profileId,
                groups,
                lineExpression,
                StringUtils.stringToEscapeSequence(this.delimiterTextField.getText()),
                StringUtils.stringToCharacter(this.quoteCharacterTextField.getText()),
                this.timeColumnTextField.getValue().index(),
                new int[0],
                this.readColumnNameCheckBox.isSelected(),
                this.overrideParsingLocaleCheckBox.isSelected(),
                Locale.forLanguageTag(parsingLocaleTextField.getText()),
                this.trimCellsCheckbox.isSelected(),
                onParsingFailure,
                StringUtils.stringToCharacter(this.commentTextField.getText())));
    }

    private String formatToDouble(String value) {
        if (value != null) {
            try {
                if (overrideParsingLocaleCheckBox.isSelected()) {
                    var parsingLocale = Locale.forLanguageTag((parsingLocaleTextField.getText()));
                    return numberFormat.format(numberFormat.parse(value.toUpperCase(parsingLocale)));
                } else {
                    return Double.valueOf(value).toString();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        return "NaN";
    }

}
