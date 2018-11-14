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
import eu.fthevenet.binjr.preferences.AppEnvironment;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.function.CheckedLambdas;
import eu.fthevenet.util.logging.TextFlowAppender;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OutputConsoleController implements Initializable {
    private static final Logger logger = LogManager.getLogger(OutputConsoleController.class);
    @FXML
    private TextFlow textOutput;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ChoiceBox<Level> logLevelChoice;
    @FXML
    private ToggleButton alwaysOnTopToggle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textOutput.getChildren().addListener(
                (ListChangeListener<Node>) ((change) -> {
                    textOutput.layout();
                    scrollPane.layout();
                    scrollPane.setVvalue(1.0f);
                }));
        initTextFlowAppender();
        Platform.runLater(() -> {
            logLevelChoice.getItems().setAll(Level.values());
            logLevelChoice.getSelectionModel().select(AppEnvironment.getInstance().getLogLevel());
            AppEnvironment.getInstance().logLevelProperty().addListener((observable, oldValue, newValue) -> {
                logLevelChoice.getSelectionModel().select(newValue);
            });
            logLevelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                AppEnvironment.getInstance().setLogLevel(newValue);
            });
        });
    }

    private synchronized void initTextFlowAppender() {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        TextFlowAppender appender = TextFlowAppender.createAppender(
                "InternalConsole",
                PatternLayout.newBuilder().withPattern("[%d{YYYY-MM-dd HH:mm:ss.SSS}] [%-5level] [%t] [%logger{36}] %msg%n").build(),
                null
        );
        TextFlowAppender.setTextFlow(textOutput);
        appender.start();
        lc.getConfiguration().addAppender(appender);
        lc.getRootLogger().addAppender(lc.getConfiguration().getAppender(appender.getName()));
        lc.updateLoggers();
    }

    public void handleClearConsole(ActionEvent actionEvent) {
        this.textOutput.getChildren().clear();
    }

    public void handleSaveConsoleOutput(ActionEvent actionEvent) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save console ouptut");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
            fileChooser.setInitialDirectory(GlobalPreferences.getInstance().getMostRecentSaveFolder().toFile());
            fileChooser.setInitialFileName("binjr_console_output.txt");
            File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(scrollPane));
            if (selectedFile != null) {
                try (Writer writer = new BufferedWriter(new FileWriter(selectedFile))) {
                    textOutput.getChildren().stream().map(node -> ((Text) node).getText()).forEach(CheckedLambdas.<String, IOException>wrap(writer::write));
                } catch (IOException e) {
                    Dialogs.notifyException("Error writing log message to file", e, scrollPane);
                }
                GlobalPreferences.getInstance().setMostRecentSaveFolder(selectedFile.toPath());

            }
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

    public ToggleButton getAlwaysOnTopToggle() {
        return alwaysOnTopToggle;
    }
}
