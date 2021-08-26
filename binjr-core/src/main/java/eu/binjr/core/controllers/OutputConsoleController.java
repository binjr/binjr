/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.core.controllers;

import eu.binjr.common.diagnostic.DiagnosticCommand;
import eu.binjr.common.diagnostic.DiagnosticException;
import eu.binjr.common.javafx.controls.ExtendedPropertyEditorFactory;
import eu.binjr.common.logging.Log4j2Level;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.Binjr;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.JvmImplementation;
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.Level;
import org.controlsfx.control.PropertySheet;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.ReadOnlyStyledDocumentBuilder;
import org.fxmisc.richtext.model.SegmentOps;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static eu.binjr.core.Binjr.DEBUG_CONSOLE_APPENDER;

/**
 * The controller for the output console window.
 *
 * @author Frederic Thevenet
 */
public class OutputConsoleController implements Initializable {
    private static final Logger logger = Logger.create(OutputConsoleController.class);
    public TextField consoleMaxLinesText;
    public VBox root;
    public PropertySheet preferenceEditor;
    @FXML
    private MenuButton debugMenuButton;
    @FXML
    private CodeArea textOutput;
    @FXML
    private ChoiceBox<Level> logLevelChoice;
    @FXML
    private ToggleButton alwaysOnTopToggle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateDebugCommandMenu();
        textOutput.setEditable(false);
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter());
        consoleMaxLinesText.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(UserPreferences.getInstance().consoleMaxLineCapacity.property());
        if (DEBUG_CONSOLE_APPENDER == null) {
            String log = "<ERROR: The debug console appender is unavailable!>\n";
            textOutput.append(log, ".styled-text-area .error");
        } else {
            DEBUG_CONSOLE_APPENDER.setRenderTextDelegate(msgSet -> {
                textOutput.clear();
                var docBuilder = new ReadOnlyStyledDocumentBuilder<Collection<String>, String, Collection<String>>(
                        SegmentOps.styledTextOps(),
                        Collections.emptyList());
                msgSet.forEach(l -> {
                    docBuilder.addParagraph(
                            l.getMessage(),
                            List.of(l.getStyleClass()),
                            Collections.emptyList());
                });
                docBuilder.addParagraph("", Collections.emptyList(), Collections.emptyList());
                textOutput.replace(docBuilder.build());
            });
        }
        Platform.runLater(() -> {
            var l = Arrays.stream(Log4j2Level.values())
                    .map(Log4j2Level::getLevel)
                    .sorted(Level::compareTo)
                    .collect(Collectors.toList());
            l.add(Logger.PERF);
            l.sort(Level::compareTo);
            logLevelChoice.getItems().setAll(l);
            logLevelChoice.getSelectionModel().select(UserPreferences.getInstance().rootLoggingLevel.get());
            UserPreferences.getInstance().rootLoggingLevel.property().addListener((observable, oldValue, newValue) -> {
                logLevelChoice.getSelectionModel().select(newValue);
            });
            logLevelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                UserPreferences.getInstance().rootLoggingLevel.set(newValue);
            });
        });
        preferenceEditor.setPropertyEditorFactory(new ExtendedPropertyEditorFactory());
        preferenceEditor.getItems().addAll(UserPreferences.getInstance()
                .getAll().values()
                .stream()
                .map(ObservablePreference::asPropertyItem)
                .collect(Collectors.toList()));
        UserPreferences.getInstance().getAll().addListener((MapChangeListener<String, ObservablePreference<?>>) c -> {
            if (c.wasAdded()) {
                preferenceEditor.getItems().add(c.getValueAdded().asPropertyItem());
            }
        });
        DataAdapterFactory.getInstance().getAllAdapters().forEach(di -> {
            preferenceEditor.getItems().addAll(di.getPreferences()
                    .getAll().values()
                    .stream()
                    .map(ObservablePreference::asPropertyItem)
                    .collect(Collectors.toList()));

            di.getPreferences().getAll().addListener((MapChangeListener<String, ObservablePreference<?>>) c -> {
                if (c.wasAdded()) {
                    preferenceEditor.getItems().add(c.getValueAdded().asPropertyItem());
                }
            });
        });
    }

    /**
     * Returns the alwaysOnTop toggle button.
     *
     * @return the alwaysOnTop toggle button.
     */
    public ToggleButton getAlwaysOnTopToggle() {
        return alwaysOnTopToggle;
    }

    @FXML
    private void handleClearConsole(ActionEvent actionEvent) {
        if (DEBUG_CONSOLE_APPENDER != null) {
            DEBUG_CONSOLE_APPENDER.clearBuffer();
        }
    }

    @FXML
    private void handleSaveConsoleOutput(ActionEvent actionEvent) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save console ouptut");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
            Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
            fileChooser.setInitialFileName("binjr_console_output.txt");
            File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(textOutput));
            if (selectedFile != null) {
                try (Writer writer = new BufferedWriter(new FileWriter(selectedFile))) {
                    writer.write(textOutput.getText());
                } catch (IOException e) {
                    Dialogs.notifyException("Error writing log message to file", e, textOutput);
                }
                UserHistory.getInstance().mostRecentSaveFolders.push(selectedFile.toPath());
            }
        } catch (Exception e) {
            Dialogs.notifyException("Failed to save console output to file", e, textOutput);
        }
    }

    @FXML
    private void handleCopyConsoleOutput(ActionEvent actionEvent) {
        try {
            final ClipboardContent content = new ClipboardContent();
            content.putString(textOutput.getText());
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception e) {
            Dialogs.notifyException("Failed to copy console output to clipboard", e, textOutput);
        }
    }

    public void handleDebugForceGC(ActionEvent actionEvent) {
        try (Profiler p = Profiler.start("Force GC", e -> Binjr.runtimeDebuggingFeatures.debug(e.toString() + " - " + getJvmHeapStats()))) {
            System.gc();
        }
    }

    public void handleDebugRunFinalization(ActionEvent actionEvent) {
        try (Profiler p = Profiler.start("Force runFinalization", Binjr.runtimeDebuggingFeatures::debug)) {
            System.runFinalization();
        }
    }

    public void handleDebugDumpHeapStats(ActionEvent actionEvent) {
        Binjr.runtimeDebuggingFeatures.debug(this::getJvmHeapStats);
    }

    public void handleDebugDumpThreadsStacks(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug(DiagnosticCommand.dumpThreadStacks());
        } catch (DiagnosticException e) {
            Dialogs.notifyException("Error running diagnostic command", e, root);
        }
    }

    public void handleDebugDumpVmSystemProperties(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug(DiagnosticCommand.dumpVmSystemProperties());
        } catch (DiagnosticException e) {
            Dialogs.notifyException("Error running diagnostic command", e, root);
        }
    }

    public void handleDebugDumpClassHistogram(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug("\n" + DiagnosticCommand.dumpClassHistogram());
        } catch (DiagnosticException e) {
            Dialogs.notifyException("Error running diagnostic command", e, root);
        }
    }

    private String getJvmHeapStats() {
        Runtime rt = Runtime.getRuntime();
        double maxMB = rt.maxMemory() / 1024.0 / 1024.0;
        double committedMB = (double) rt.totalMemory() / 1024.0 / 1024.0;
        double usedMB = ((double) rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0;
        double percentCommitted = (((double) rt.totalMemory() - rt.freeMemory()) / rt.totalMemory()) * 100;
        double percentMax = (((double) rt.totalMemory() - rt.freeMemory()) / rt.maxMemory()) * 100;
        return String.format(
                "JVM Heap: Max=%.0fMB, Committed=%.0fMB, Used=%.0fMB (%.2f%% of committed, %.2f%% of max)",
                maxMB,
                committedMB,
                usedMB,
                percentCommitted,
                percentMax
        );
    }

    public void handleDebugDumpVmFlags(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug(DiagnosticCommand.dumpVmFlags());
        } catch (DiagnosticException e) {
            Dialogs.notifyException("Error running diagnostic command", e, root);
        }
    }

    public void handleDebugDumpVmCommandLine(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug(DiagnosticCommand.dumpVmCommandLine());
        } catch (DiagnosticException e) {
            Dialogs.notifyException("Error running diagnostic command", e, root);
        }
    }

    public void handleImportUserHistory(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import User History");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr user history", "*.xml"));
        File importPath = fileChooser.showOpenDialog(Dialogs.getStage(root));
        if (importPath != null) {
            try {
                UserHistory.getInstance().importFromFile(importPath.toPath());
                Binjr.runtimeDebuggingFeatures.debug("User history successfully imported from " + importPath.toString());
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while importing user history: " + e.getMessage(), e, root);
            }
        }
    }

    public void handleExportUserHistory(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export User History");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr user history", "*.xml"));
        fileChooser.setInitialFileName("binjr_history.xml");
        var exportPath = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (exportPath != null) {
            try {
                Files.deleteIfExists(exportPath.toPath());
                UserHistory.getInstance().exportToFile(exportPath.toPath());
                Binjr.runtimeDebuggingFeatures.debug("User history successfully exported to " + exportPath.toString());
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while exporting user history: " + e.getMessage(), e, root);
            }
        }
    }

    public void handleListHotspotVmOptions(ActionEvent actionEvent) {
        try {
            Binjr.runtimeDebuggingFeatures.debug(DiagnosticCommand.dumpVmOptions());
        } catch (Exception e) {
            Dialogs.notifyException("Error attempting to list Hotspot VM options: "+ e.getMessage(), e);
        }
    }

    public void handleDebugDumpHeap(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Dump Heap");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("heap dump", "*.hprof"));
        fileChooser.setInitialFileName(String.format("binjr_jvm_pid%d.hprof", ProcessHandle.current().pid()));
        var exportPath = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (exportPath != null) {
            try {
                Files.deleteIfExists(exportPath.toPath());
                DiagnosticCommand.dumpHeap(exportPath.toPath());
                Binjr.runtimeDebuggingFeatures.debug("JVM's heap successfully dumped to " + exportPath.toString());
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while dumping JVM's heap: " + e.getMessage(), e, root);
            }
        }
    }

    private void populateDebugCommandMenu() {
        addMenuItem(debugMenuButton, "Dump Heap Stats", "debug-low-icon", this::handleDebugDumpHeapStats, false);
        addMenuItem(debugMenuButton, "Dump VM Flags", "debug-low-icon", this::handleDebugDumpVmFlags, true);
        addMenuItem(debugMenuButton, "Dump VM Command Line", "debug-low-icon", this::handleDebugDumpVmCommandLine, true);
        addMenuItem(debugMenuButton, "Dump VM System Properties", "debug-low-icon", this::handleDebugDumpVmSystemProperties, true);

        addMenuItem(debugMenuButton, "Run GC", "debug-med-icon", this::handleDebugForceGC, false);
        addMenuItem(debugMenuButton, "Run Finalization", "debug-med-icon", this::handleDebugRunFinalization, false);
        addMenuItem(debugMenuButton, "List Hotspot VM Options", "debug-med-icon", this::handleListHotspotVmOptions, true);
        addMenuItem(debugMenuButton, "Export User History", "debug-med-icon", this::handleExportUserHistory, false);
        addMenuItem(debugMenuButton, "Import User History", "debug-med-icon", this::handleImportUserHistory, false);

        addMenuItem(debugMenuButton, "Dump Threads Stacks", "debug-high-icon", this::handleDebugDumpThreadsStacks, true);
        addMenuItem(debugMenuButton, "Dump GC Class Histogram", "debug-high-icon", this::handleDebugDumpClassHistogram, true);
        addMenuItem(debugMenuButton, "Dump Heap", "debug-high-icon", this::handleDebugDumpHeap, true);
    }

    private void addMenuItem(MenuButton menu, String text, String iconclass, EventHandler<ActionEvent> actionHandler, boolean hotspotOnly) {
        if (!hotspotOnly || AppEnvironment.getInstance().getRunningJvm() == JvmImplementation.HOTSPOT) {
            var m = new MenuItem();
            var r = new Region();
            r.getStyleClass().add(iconclass);
            var h = new HBox(r);
            h.getStyleClass().add("icon-container");
            m.setGraphic(h);
            m.setText(text);
            m.setOnAction(actionHandler);
            menu.getItems().add(m);
        }
    }
}
