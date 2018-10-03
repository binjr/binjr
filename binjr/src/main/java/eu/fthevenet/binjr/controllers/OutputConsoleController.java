/*
 *    Copyright 2018 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.util.logging.TextFlowAppender;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OutputConsoleController implements Initializable {
    private static final Logger logger = LogManager.getLogger(OutputConsoleController.class);
    public TextFlow textOutput;
    public ScrollPane scrollPane;
    public ChoiceBox<Level> logLevelChoice;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textOutput.getChildren().addListener(
                (ListChangeListener<Node>) ((change) -> {
                    textOutput.layout();
                    scrollPane.layout();
                    scrollPane.setVvalue(1.0f);
                }));
        TextFlowAppender.setTextFlow(textOutput);

        logLevelChoice.getItems().setAll(Level.values());
        logLevelChoice.getSelectionModel().select(Level.DEBUG);
        logLevelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Configurator.setRootLevel(newValue);
        });
    }

    public void handleClearConsole(ActionEvent actionEvent) {
        this.textOutput.getChildren().clear();
    }

    public void handleSaveConsoleOutput(ActionEvent actionEvent) {
        try {
            throw new UnsupportedOperationException();
        } catch (Exception e) {
            Dialogs.notifyException("Failed to save console output to file", e, scrollPane);
        }
    }

    public void handleCopyConsoleOutput(ActionEvent actionEvent) {
        try {
            final ClipboardContent content = new ClipboardContent();
            content.putString(textOutput.getChildren().stream().map(node -> ((Text) node).getText()).collect(Collectors.joining()));
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception e) {
            Dialogs.notifyException("Failed to copy console output to clipboard", e, scrollPane);
        }
    }
}
