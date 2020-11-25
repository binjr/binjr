/*
 *    Copyright 2020 Frederic Thevenet
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
package eu.binjr.sources.logs.controllers;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.logs.adapters.LogsAdapterPreferences;
import eu.binjr.sources.logs.adapters.LogsDataAdapter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ParsingProfilesController {
    private static final Logger logger = Logger.create(ParsingProfilesController.class);
    private static final Pattern GROUP_TAG_PATTERN = Pattern.compile("\\$[A-Z]{2,}");
    private final LogsAdapterPreferences userPrefs;

    @FXML
    private TableColumn<NameExpressionPair, String> expressionColumn;
    @FXML
    private TableColumn<NameExpressionPair, NamedCaptureGroup> nameColumn;
    @FXML
    private Label notificationLabel;
    @FXML
    private TableView<NameExpressionPair> captureGroupTable;
    @FXML
    private Button deleteGroupButton;
    @FXML
    private Button addGroupButton;
    @FXML
    private AnchorPane root;
    @FXML
    private VBox expressions;
    @FXML
    private HBox lineTemplate;
    @FXML
    private CodeArea lineTemplateExpression;
    @FXML
    private Button testLineTemplate;
    @FXML
    private CodeArea testArea;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ComboBox<ParsingProfile> profileComboBox;
    @FXML
    private Button addProfileButton;
    @FXML
    private Button deleteProfileButton;
    @FXML
    private Button cloneProfileButton;
    @FXML
    private Button importProfileButton;
    @FXML
    private Button exportProfileButton;

    @FXML
    private void handleOnCloneProfile(ActionEvent actionEvent) {
        try {
            if (this.profileComboBox.getValue() != null) {
                var p = CustomParsingProfile.of(this.profileComboBox.getValue());
                this.profileComboBox.getItems().add(p);
                this.profileComboBox.getSelectionModel().select(p);
            }
        } catch (Throwable e) {
            Dialogs.notifyException("Error cloning profile", e, root);
        }
    }

    @FXML
    private void handleOnAddProfile(ActionEvent actionEvent) {
        try {
            var p = CustomParsingProfile.empty();
            this.profileComboBox.getItems().add(p);
            this.profileComboBox.getSelectionModel().select(p);
        } catch (Throwable e) {
            Dialogs.notifyException("Error creating profile", e, root);
        }
    }

    @FXML
    void handleOnDeleteProfile(ActionEvent event) {

    }

    @FXML
    void handleOnExportProfile(ActionEvent event) {

    }

    @FXML
    void handleOnImportProfile(ActionEvent event) {

    }

    @FXML
    void handleOnTestLineTemplate(ActionEvent event) {
        try {
            resetTest();
            applyChanges();
            var parser = new EventParser(this.profileComboBox.getValue(), ZoneId.systemDefault());
            parser.parse(testArea.getText());
            var events = new ArrayList<ParsedEvent>();
            Scanner scanner = new Scanner(testArea.getText());
            while (scanner.hasNextLine()) {
                parser.parse(scanner.nextLine()).ifPresent(events::add);
            }
            if (events.size() == 0) {
                notifyWarn("No event found.");
            } else {
                notifyInfo(String.format("Found %d event(s).", events.size()));
            }
            testArea.setStyleSpans(0, highlightTextArea(parser.getParsingRegex(), testArea.getText()));
        } catch (Exception e) {
            notifyError(e.getMessage());
            logger.error("Error testing parsing rules", e);
            logger.debug(() -> "Stack Trace", e);
        }
    }

    @FXML
    private void handleOnAddGroup(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnDeleteGroup(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnClearTestArea(ActionEvent actionEvent) {
        testArea.clear();
    }

    @FXML
    private void handleOnCopyTestArea(ActionEvent actionEvent) {
        testArea.selectAll();
        testArea.copy();
    }

    @FXML
    private void handleOnPasteToTestArea(ActionEvent actionEvent) {
        testArea.selectAll();
        testArea.paste();
    }

    @FXML
    private void handleOnClearField(ActionEvent actionEvent) {
        var node = (Node) actionEvent.getSource();
        var parent = node.getParent();
        if (parent != null) {
            var field = parent.lookup(".search-field-inner");
            if (field instanceof TextField) {
                ((TextField) field).setText("");
            }
        }
    }

    @FXML
    private void handleOnOk(ActionEvent actionEvent) {
        userPrefs.userParsingProfiles.set(profileComboBox.getItems().stream()
                .filter(p -> p instanceof CustomParsingProfile)
                .collect(Collectors.toList())
                .toArray(ParsingProfile[]::new));
        userPrefs.mostRecentlyUsedParsingProfile.set(profileComboBox.getValue().getProfileName());
        Dialogs.getStage(root).close();
    }

    @FXML
    private void handleOnCancel(ActionEvent actionEvent) {
        Dialogs.getStage(root).close();
    }

    public ParsingProfilesController() {
        LogsAdapterPreferences p;
        try {
            p = (LogsAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(LogsDataAdapter.class.getName());
        } catch (NoAdapterFoundException e) {
            p = new LogsAdapterPreferences(LogsDataAdapter.class);
            e.printStackTrace();
        }
        this.userPrefs = p;
    }

    @FXML
    void initialize() {
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert expressions != null : "fx:id=\"expressions\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplate != null : "fx:id=\"lineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplateExpression != null : "fx:id=\"lineTemplateExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testLineTemplate != null : "fx:id=\"testLineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testArea != null : "fx:id=\"testArea\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert okButton != null : "fx:id=\"okButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert cancelButton != null : "fx:id=\"cancelButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert profileComboBox != null : "fx:id=\"profileComboBox\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert addProfileButton != null : "fx:id=\"importProfileButton2\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert deleteProfileButton != null : "fx:id=\"deleteProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert cloneProfileButton != null : "fx:id=\"importProfileButton1\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert importProfileButton != null : "fx:id=\"importProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert exportProfileButton != null : "fx:id=\"exportProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";

        this.profileComboBox.getItems().setAll(BuiltInParsingProfile.values());
        this.profileComboBox.getItems().addAll(userPrefs.userParsingProfiles.get());
        this.profileComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadParserParameters(newValue);
            }
        });

        this.profileComboBox.setConverter(new StringConverter<ParsingProfile>() {
            @Override
            public String toString(ParsingProfile object) {
                return object.toString();
            }

            @Override
            public ParsingProfile fromString(String string) {
                var val = profileComboBox.getValue();
                if (val instanceof CustomParsingProfile) {
                    ((CustomParsingProfile) val).setProfileName(string);
                }
                return val;
            }
        });

        lineTemplateExpression.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node source = (Node) event.getSource();
            if (event.getCode() == KeyCode.ENTER) {
                source.fireEvent(new ActionEvent(this, null));
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                Node parent = source.getParent();
                if (parent != null) {
                    var siblings = source.getParent().getChildrenUnmodifiable();
                    var pos = siblings.indexOf(source);
                    if (event.isShiftDown() && pos > 0) {
                        siblings.get(pos - 1).requestFocus();
                    } else if (!event.isShiftDown() && pos < siblings.size()) {
                        siblings.get(pos + 1).requestFocus();
                    } else {
                        parent.requestFocus();
                    }
                }
                event.consume();
            }
        });
        testArea.textProperty().addListener((obs, oldText, newText) -> {
            resetTest();
        });
        lineTemplateExpression.textProperty().addListener((obs, oldText, newText) -> {
            resetTest();
            lineTemplateExpression.setStyleSpans(0, computeParsingProfileSyntaxHighlighting(newText));
        });
        this.profileComboBox.getSelectionModel().select(BuiltInParsingProfile.BINJR);

        this.nameColumn.setCellFactory(ColoredTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(NamedCaptureGroup object) {
                return object.toString();
            }

            @Override
            public NamedCaptureGroup fromString(String string) {
                return CaptureGroup.of(string);
            }
        }, TemporalCaptureGroup.values()));
        this.nameColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setName(t.getNewValue())
        );
        this.expressionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        this.expressionColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setExpression(t.getNewValue())
        );
    }

    private void resetTest() {
        clearNotification();
        var spans = new StyleSpansBuilder<Collection<String>>();
        spans.add(Collections.emptyList(), testArea.getText().length());
        testArea.setStyleSpans(0, spans.create());
    }

   private  class ColoredTableCell extends ComboBoxTableCell<NameExpressionPair, NamedCaptureGroup>{
   //     private  class ColoredTableCell extends TextFieldTableCell<NameExpressionPair, NamedCaptureGroup>{
        @Override
        public void updateItem(NamedCaptureGroup item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEmpty()) {
                var idx = getCaptureGroupPaletteIndex(item);
                this.setStyle("-fx-font-weight: bold; -fx-text-fill:-palette-color-" + idx + ";");
                setText(item.toString());
            }
        }
    }

    private void loadParserParameters(ParsingProfile profile) {
        try {
            resetTest();
            this.captureGroupTable.getItems().clear();
            profile.getCaptureGroups().forEach((k, v) -> {
                this.captureGroupTable.getItems().add(new NameExpressionPair(k, v));
            });

            this.lineTemplateExpression.clear();
            this.lineTemplateExpression.appendText(profile.getLineTemplateExpression());

            var isEditable = (profile instanceof CustomParsingProfile);
            this.lineTemplateExpression.setDisable(!isEditable);
            this.addGroupButton.setDisable(!isEditable);
            this.deleteGroupButton.setDisable(!isEditable);
            this.deleteProfileButton.setDisable(!isEditable);

            this.captureGroupTable.setEditable(isEditable);
            this.nameColumn.setEditable(isEditable);
            this.expressionColumn.setEditable(isEditable);
            this.profileComboBox.getEditor().setEditable(isEditable);
        } catch (
                Exception e) {
            Dialogs.notifyException("Error loading profile", e, root);
        }

    }

    private void saveProfile(CustomParsingProfile profile) {
        try {
            profile.setCaptureGroups(this.captureGroupTable.getItems().stream()
                    .collect(Collectors.toMap(NameExpressionPair::getName, NameExpressionPair::getExpression)));
            profile.setLineTemplateExpression(this.lineTemplateExpression.getText());
        } catch (Exception e) {
            Dialogs.notifyException("Error saving profile", e, root);
        }

    }

    private void applyChanges() {
        if (this.profileComboBox.getValue() instanceof CustomParsingProfile) {
            var editable = (CustomParsingProfile) this.profileComboBox.getValue();
            saveProfile(editable);
        }

    }

    private final AtomicInteger paletteEntriesSequence = new AtomicInteger(0);
    private final Map<NamedCaptureGroup, Integer> paletteLookupTable = new HashMap<>();

    private int getCaptureGroupPaletteIndex(NamedCaptureGroup cg) {
        return paletteLookupTable.computeIfAbsent(cg, key -> paletteEntriesSequence.getAndIncrement() % 12);
    }

    public StyleSpans<Collection<String>> computeParsingProfileSyntaxHighlighting(String text) {
        Matcher matcher = GROUP_TAG_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            var tag = matcher.group();
            var cg = captureGroupTable.getItems().stream().filter(n -> tag.equals("$" + n.getName())).findAny();
            if (cg.isPresent()) {
                var idx = getCaptureGroupPaletteIndex(cg.get().getName());
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(List.of("capture-group", "capture-group-" + idx), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private StyleSpans<Collection<String>> highlightTextArea(Pattern parsingRegex, String text) {
        var matcher = parsingRegex.matcher(text);
        int lastMatchEnd = 0;
        StyleSpansBuilder<Collection<String>> groupSpansBuilder = new StyleSpansBuilder<>();
        StyleSpansBuilder<Collection<String>> logsSpansBuilder = new StyleSpansBuilder<>();
        logsSpansBuilder.add(Collections.emptyList(), 0);
        while (matcher.find()) {
            logsSpansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            logsSpansBuilder.add(List.of("complete-log"), matcher.end() - matcher.start());
            groupSpansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            SortedMap<Integer, NamedCaptureGroup> ordered = new TreeMap<>();
            for (Map.Entry<NamedCaptureGroup, String> entry : profileComboBox.getValue().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var idx = getCaptureGroupPaletteIndex(captureGroup);
                var parsed = matcher.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    ordered.put(matcher.start(captureGroup.name()), captureGroup);
                }
            }
            var cursor = matcher.start();
            for (var entry : ordered.entrySet()) {
                var groupPos = entry.getKey();
                var groupName = entry.getValue();
                var spanLen = groupPos - cursor;
                if (spanLen > 0) {
                    groupSpansBuilder.add(Collections.emptyList(), spanLen);
                    cursor += spanLen;
                }
                var paletteIndex = getCaptureGroupPaletteIndex(groupName);
                var len = matcher.end(groupName.name()) - matcher.start(groupName.name());
                groupSpansBuilder.add(List.of("capture-group", "capture-group-" + paletteIndex), len);
                cursor += len;
            }
            groupSpansBuilder.add(Collections.emptyList(), matcher.end() - cursor);
            lastMatchEnd = matcher.end();
        }
        groupSpansBuilder.add(Collections.emptyList(), text.length() - lastMatchEnd);
        return groupSpansBuilder.create().overlay(logsSpansBuilder.create(), (strings, strings2) -> Stream.concat(strings.stream(),
                strings2.stream()).collect(Collectors.toCollection(ArrayList<String>::new)));
    }

    private void clearNotification() {
        notificationLabel.setVisible(false);
        notificationLabel.setManaged(false);
        notificationLabel.setText("");
    }

    private void notifyInfo(String message) {
        notify(message, "notification-info");
    }

    private void notifyWarn(String message) {
        notify(message, "notification-warn");
    }

    private void notifyError(String message) {
        notify(message, "notification-error");
    }

    private void notify(String message, String styleClass) {
        notificationLabel.getStyleClass().setAll(styleClass);
        notificationLabel.setText(message);
        notificationLabel.setManaged(true);
        notificationLabel.setVisible(true);
    }

    public static class NameExpressionPair {
        private NamedCaptureGroup name;
        private String expression;

        private NameExpressionPair(NamedCaptureGroup name, String expression) {
            this.name = name;
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        public NamedCaptureGroup getName() {
            return name;
        }

        public void setName(NamedCaptureGroup name) {
            this.name = name;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }
    }


}

