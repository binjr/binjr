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
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.logs.parser.capture.NamedCaptureGroup;
import eu.binjr.sources.logs.parser.profile.BuiltInParsingProfile;
import eu.binjr.sources.logs.parser.profile.CustomParsingProfile;
import eu.binjr.sources.logs.parser.profile.ParsingProfile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParsingProfilesController {
    private static final Logger logger = Logger.create(ParsingProfilesController.class);

    public TableColumn<NameExpressionPair, NamedCaptureGroup> nameColumn;
    //    public TableColumn expressionColumn;
    @FXML
    private TableView<NameExpressionPair> captureGroupTable;

    public static class NameExpressionPair {
        private final NamedCaptureGroup name;
        private final String expression;

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
    }

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
    private Button applyButton;

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

    }

    @FXML
    private void handleOnAddGroup(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnDeleteGroup(ActionEvent actionEvent) {

    }

    public void handleOnClearTestArea(ActionEvent actionEvent) {

    }


    public void handleOnCopyTestArea(ActionEvent actionEvent) {

    }

    public void handleOnPasteToTestArea(ActionEvent actionEvent) {

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
        applyChanges();
        Dialogs.getStage(root).close();
    }

    @FXML
    private void handleOnCancel(ActionEvent actionEvent) {
        Dialogs.getStage(root).close();
    }

    @FXML
    private void handleOnApply(ActionEvent actionEvent) {
        applyChanges();
    }

    private void applyChanges() {
        if (this.profileComboBox.getValue() instanceof CustomParsingProfile) {
            var editable = (CustomParsingProfile) this.profileComboBox.getValue();
            saveParserParameters(editable);
        }
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

        lineTemplateExpression.textProperty().addListener((obs, oldText, newText) -> {
            lineTemplateExpression.setStyleSpans(0, computeParsingProfileSyntaxHighlighting(newText));
        });

        this.profileComboBox.getSelectionModel().select(BuiltInParsingProfile.BINJR);

    }

    private void loadParserParameters(ParsingProfile profile) {
        try {
            this.captureGroupTable.getItems().clear();
            profile.getCaptureGroups().forEach((k, v) -> {
                this.captureGroupTable.getItems().add(new NameExpressionPair(k, v));
            });
            nameColumn.setCellFactory(new Callback<>() {
                @Override
                public TableCell<NameExpressionPair, NamedCaptureGroup> call(TableColumn<NameExpressionPair, NamedCaptureGroup> param) {
                    return new TableCell<>() {
                        @Override
                        protected void updateItem(NamedCaptureGroup item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {
                                var idx =getCaptureGroupPaletteIndex(item);
                                this.setStyle("-fx-font-weight: bold; -fx-text-fill:-palette-color-" + idx + ";");
                                setText(item.toString());
                            }
                        }
                    };
                }
            });
            this.lineTemplateExpression.clear();
            this.lineTemplateExpression.appendText(profile.getLineTemplateExpression());

            var notEditable = !(profile instanceof CustomParsingProfile);
            this.lineTemplate.setDisable(notEditable);
            this.addGroupButton.setDisable(notEditable);
            this.deleteGroupButton.setDisable(notEditable);
            this.captureGroupTable.setDisable(notEditable);
            this.applyButton.setDisable(notEditable);
            this.deleteProfileButton.setDisable(notEditable);
            this.profileComboBox.getEditor().setEditable(!notEditable);


        } catch (
                Exception e) {
            Dialogs.notifyException("Error loading profile", e, root);
        }

    }

    private void saveParserParameters(CustomParsingProfile profile) {
        try {
            profile.setLineTemplateExpression(this.lineTemplateExpression.getText());
        } catch (Exception e) {
            Dialogs.notifyException("Error saving profile", e, root);
        }

    }

    private final AtomicInteger paletteEntriesSequence = new AtomicInteger(0);
    private final Map<NamedCaptureGroup, Integer> paletteLookupTable = new HashMap<>();


    private int getCaptureGroupPaletteIndex(NamedCaptureGroup cg) {
        return paletteLookupTable.computeIfAbsent(cg, key -> paletteEntriesSequence.getAndIncrement() % 12);
    }

    public StyleSpans<Collection<String>> computeParsingProfileSyntaxHighlighting(String text) {
        Matcher matcher = Pattern.compile("\\$[A-Z]{2,}").matcher(text);
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

}

