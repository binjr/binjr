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

import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.logs.parser.profile.BuiltInParsingProfile;
import eu.binjr.sources.logs.parser.profile.CustomParsingProfile;
import eu.binjr.sources.logs.parser.profile.ParsingProfile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.fxmisc.richtext.CodeArea;

import java.net.URL;
import java.util.ResourceBundle;

public class ParsingProfilesController {

    @FXML
    private Button applyButton;

    @FXML
    private CheckBox normalizeSepCheckBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane root;

    @FXML
    private VBox expressions;

    @FXML
    private HBox timeCapture;

    @FXML
    private TextField timeCaptureExpression;

    @FXML
    private Button clearTimeCapture;

    @FXML
    private Button timeCaptureHelp;

    @FXML
    private HBox separatorsToNormalize;

    @FXML
    private TextField separatorsToNormalizeExpression;

    @FXML
    private Button clearSeparatorsToNormalize;

    @FXML
    private Button separatorsToNormalizeHelp;

    @FXML
    private HBox separatorReplacement;

    @FXML
    private TextField separatorReplacementExpression;

    @FXML
    private Button clearSeparatorReplacement;

    @FXML
    private Button separatorReplacementHelp;

    @FXML
    private HBox timeParsing;

    @FXML
    private TextField timeParsingExpression;

    @FXML
    private Button clearTimeParsing;

    @FXML
    private Button timeParsingHelp;

    @FXML
    private Button testTimestamp;

    @FXML
    private Button resetTimestamp;

    @FXML
    private HBox severityCapture;

    @FXML
    private TextField severityCaptureExpression;

    @FXML
    private Button clearSeverityCapture;

    @FXML
    private Button severityCaptureHelp;

    @FXML
    private Button testSeverity;

    @FXML
    private Button resetSeverity;

    @FXML
    private HBox lineTemplate;

    @FXML
    private TextField lineTemplateExpression;

    @FXML
    private Button clearLineTemplate;

    @FXML
    private Button lineTemplateHelp;

    @FXML
    private Button testLineTemplate;

    @FXML
    private Button resetLineTemplate;

    @FXML
    private CodeArea testArea;

    @FXML
    private HBox labels;

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
    private void handleOnDelete(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnExport(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnImport(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnTestTimestamp(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnTestSeverity(ActionEvent actionEvent) {

    }

    @FXML
    private void handleOnTestLineTemplate(ActionEvent actionEvent) {

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
    private void handleOnDisplayHelp(ActionEvent actionEvent) {
        var node = (Button) actionEvent.getSource();
//        for (var t :node.getChildrenUnmodifiable()){
//            if (t instanceof Tooltip){
//
//            }
//        }
        
        if (node.getUserData() instanceof String) {
            String help = (String) node.getUserData();
            var popup = new Tooltip(help);
            popup.setAutoHide(true);
            Bounds bounds = node.localToScreen(node.getBoundsInLocal());
            popup.show(node.getScene().getWindow(), bounds.getMaxX(), bounds.getMaxY());
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
    void initialize() {
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert expressions != null : "fx:id=\"expressions\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeCapture != null : "fx:id=\"timeCapture\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeCaptureExpression != null : "fx:id=\"timeCaptureExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearTimeCapture != null : "fx:id=\"clearTimeCapture\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeCaptureHelp != null : "fx:id=\"timeCaptureHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorsToNormalize != null : "fx:id=\"separatorsToNormalize\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorsToNormalizeExpression != null : "fx:id=\"separatorsToNormalizeExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearSeparatorsToNormalize != null : "fx:id=\"clearSeparatorsToNormalize\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorsToNormalizeHelp != null : "fx:id=\"separatorsToNormalizeHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorReplacement != null : "fx:id=\"separatorReplacement\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorReplacementExpression != null : "fx:id=\"separatorReplacementExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearSeparatorReplacement != null : "fx:id=\"clearSeparatorReplacement\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert separatorReplacementHelp != null : "fx:id=\"separatorReplacementHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeParsing != null : "fx:id=\"timeParsing\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeParsingExpression != null : "fx:id=\"timeParsingExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearTimeParsing != null : "fx:id=\"clearTimeParsing\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeParsingHelp != null : "fx:id=\"timeParsingHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testTimestamp != null : "fx:id=\"testTimestamp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert resetTimestamp != null : "fx:id=\"resetTimestamp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert severityCapture != null : "fx:id=\"severityCapture\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert severityCaptureExpression != null : "fx:id=\"severityCaptureExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearSeverityCapture != null : "fx:id=\"clearSeverityCapture\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert severityCaptureHelp != null : "fx:id=\"severityCaptureHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testSeverity != null : "fx:id=\"testSeverity\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert resetSeverity != null : "fx:id=\"resetSeverity\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplate != null : "fx:id=\"lineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplateExpression != null : "fx:id=\"lineTemplateExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert clearLineTemplate != null : "fx:id=\"clearLineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplateHelp != null : "fx:id=\"lineTemplateHelp\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testLineTemplate != null : "fx:id=\"testLineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert resetLineTemplate != null : "fx:id=\"resetLineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testArea != null : "fx:id=\"testArea\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert labels != null : "fx:id=\"labels\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
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

//        this.profileComboBox.editorProperty().addListener((observable) -> {
//            if (profileComboBox.getSelectionModel().getSelectedItem() instanceof CustomParsingProfile) {
//                var currentProfile = (CustomParsingProfile) profileComboBox.getSelectionModel().getSelectedItem();
//                currentProfile.setProfileName(((TextField) observable).getText());
//            }
//        });


//        this.profileComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
//            if (profileComboBox.getSelectionModel().getSelectedItem() instanceof CustomParsingProfile) {
//                var currentProfile = (CustomParsingProfile) profileComboBox.getSelectionModel().getSelectedItem();
//                currentProfile.setProfileName(newValue);
//            } else {
//                newValue = oldValue;
//            }
//        });
        this.profileComboBox.getSelectionModel().select(BuiltInParsingProfile.BINJR);

    }

    private void loadParserParameters(ParsingProfile profile) {
        try {
            var notEditable = !(profile instanceof CustomParsingProfile);
//            this.timeCaptureExpression.setText(profile.getTimeCaptureExpression());
//            this.timeCapture.setDisable(notEditable);
//
//            this.normalizeSepCheckBox.setSelected(profile.isNormalizeSepCheckBox());
//            this.normalizeSepCheckBox.setDisable(notEditable);
//
//            this.separatorReplacementExpression.setText(profile.getSeparatorReplacementExpression());
//            this.separatorReplacement.setDisable(notEditable);
//
//            this.separatorsToNormalizeExpression.setText(profile.getSeparatorsToNormalizeExpression());
//            this.separatorsToNormalize.setDisable(notEditable);
//
//            this.timeParsingExpression.setText(profile.getTimeParsingExpression());
//            this.timeParsing.setDisable(notEditable);
//
//            this.severityCaptureExpression.setText(profile.getSeverityCaptureExpression());
//            this.severityCapture.setDisable(notEditable);

            this.lineTemplateExpression.setText(profile.getLineTemplateExpression());
            this.lineTemplate.setDisable(notEditable);

            this.resetLineTemplate.setDisable(notEditable);
            this.resetSeverity.setDisable(notEditable);
            this.resetTimestamp.setDisable(notEditable);
            this.applyButton.setDisable(notEditable);
            this.deleteProfileButton.setDisable(notEditable);
            this.profileComboBox.getEditor().setEditable(!notEditable);
        } catch (Exception e) {
            Dialogs.notifyException("Error loading profile", e, root);
        }
    }

    private void saveParserParameters(CustomParsingProfile profile) {
        try {
//            profile.setTimeCaptureExpression(this.timeCaptureExpression.getText());
//            profile.setNormalizeSepCheckBox(this.normalizeSepCheckBox.isSelected());
//            profile.setSeparatorReplacementExpression(this.separatorReplacementExpression.getText());
//            profile.setSeparatorsToNormalizeExpression(this.separatorsToNormalizeExpression.getText());
//            profile.setTimeParsingExpression(this.timeParsingExpression.getText());
//            profile.setSeverityCaptureExpression(this.severityCaptureExpression.getText());
            profile.setLineTemplateExpression(this.lineTemplateExpression.getText());
        } catch (Exception e) {
            Dialogs.notifyException("Error saving profile", e, root);
        }

    }


}

