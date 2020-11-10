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

import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.logs.adapters.LogsAdapterPreferences;
import eu.binjr.sources.logs.adapters.LogsDataAdapter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ParsingRulesController {

    @FXML
    private AnchorPane root;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private VBox expressions;

    @FXML
    private TextField timeRegexField;

    @FXML
    private Button verifyTimeRegexButton;

    @FXML
    private TextField severityRegexField;

    @FXML
    private Button verifySeverityRegexButton;

    @FXML
    private TextField messageRegexField;

    @FXML
    private Button verifyMessageRegexButton;

    @FXML
    private TextField lineRegexField;

    @FXML
    private Button verifyLineRegexButton;

    @FXML
    private TextArea testTextArea;

    @FXML
    private HBox labels;

    @FXML
    private ComboBox<?> profileComboBox;

    @FXML
    private Button importProfileButton;

    @FXML
    private Button exportProfileButton;

    @FXML
    private Button deleteProfileButton;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    void initialize() {
        assert expressions != null : "fx:id=\"expressions\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert timeRegexField != null : "fx:id=\"timeRegexField\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert verifyTimeRegexButton != null : "fx:id=\"verifyTimeRegexButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert severityRegexField != null : "fx:id=\"severityRegexField\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert verifySeverityRegexButton != null : "fx:id=\"verifySeverityRegexButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert messageRegexField != null : "fx:id=\"messageRegexField\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert verifyMessageRegexButton != null : "fx:id=\"verifyMessageRegexButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineRegexField != null : "fx:id=\"lineRegexField\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert verifyLineRegexButton != null : "fx:id=\"verifyLineRegexButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testTextArea != null : "fx:id=\"testTextArea\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert labels != null : "fx:id=\"labels\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert profileComboBox != null : "fx:id=\"profileComboBox\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert importProfileButton != null : "fx:id=\"importProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert exportProfileButton != null : "fx:id=\"exportProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert deleteProfileButton != null : "fx:id=\"deleteProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert okButton != null : "fx:id=\"okButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert cancelButton != null : "fx:id=\"cancelButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";


        try {
            LogsAdapterPreferences prefs = (LogsAdapterPreferences) DataAdapterFactory
                    .getInstance()
                    .getAdapterPreferences(LogsDataAdapter.class.getName());
            this.timeRegexField.setText(prefs.timestampPattern.get());
            this.severityRegexField.setText(prefs.severityPattern.get());
            this.messageRegexField.setText(prefs.msgPattern.get());
            this.lineRegexField.setText(prefs.linePattern.get());
        } catch (DataAdapterException e) {
            Dialogs.notifyException("", e, root);
        }
    }

    @FXML
    private void handleOnOk(ActionEvent actionEvent) {
        Dialogs.getStage(root).close();
    }

    @FXML
    private void handleOnCancel(ActionEvent actionEvent) {
        Dialogs.getStage(root).close();
    }
}

