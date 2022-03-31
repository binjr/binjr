/*
 *    Copyright 2018-2021 Frederic Thevenet
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

package eu.binjr.sources.rrd4j.adapters;

import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A dialog box that returns a {@link Rrd4jFileAdapterDialog} built according to user inputs.
 *
 * @author Frederic Thevenet
 */
public class Rrd4jFileAdapterDialog extends Dialog<Collection<DataAdapter>> {
    private static final Logger logger = Logger.create(Rrd4jFileAdapterDialog.class);
    private static final String BINJR_SOURCES = "binjr/sources";
    private Collection<DataAdapter> result = null;
    private final TextField pathsField;
    private final MostRecentlyUsedList<Path> mostRecentRrdFiles =
            UserHistory.getInstance().pathMostRecentlyUsedList("mostRecentRrdFiles", 20, false);

    /**
     * Initializes a new instance of the {@link Rrd4jFileAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public Rrd4jFileAdapterDialog(Node owner) {
        if (owner != null) {
            this.initOwner(NodeUtils.getStage(owner));
        }
        this.setTitle("Source");
        Button browseButton = new Button("Browse");
        pathsField = new TextField();
        HBox pathHBox = new HBox();
        pathHBox.setSpacing(10);
        pathHBox.setAlignment(Pos.CENTER);
        pathHBox.getChildren().addAll(pathsField, browseButton);
        browseButton.setPrefWidth(-1);
        pathsField.setPrefWidth(400);
        DialogPane dialogPane = new DialogPane();
        dialogPane.setHeaderText("Add RRD file(s)");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setGraphic(new Region());
        dialogPane.getGraphic().getStyleClass().addAll("source-icon", "dialog-icon");
        dialogPane.setContent(pathHBox);
        this.setDialogPane(dialogPane);

        browseButton.setOnAction(event -> {
            File selectedFile = displayFileChooser((Node) event.getSource());
            if (selectedFile != null) {
                pathsField.setText(selectedFile.getPath());
            }
        });

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Platform.runLater(pathsField::requestFocus);

        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                result = List.of(getDataAdapter());
            } catch (CannotInitializeDataAdapterException e) {
                Dialogs.notifyError("Error initializing adapter to source", e, Pos.CENTER, pathsField);
                ae.consume();
            } catch (DataAdapterException e) {
                Dialogs.notifyError("Error with the adapter to source", e, Pos.CENTER, pathsField);
                ae.consume();
            } catch (Throwable e) {
                Dialogs.notifyError("Unexpected error while retrieving data adapter", e, Pos.CENTER, pathsField);
                ae.consume();
            }
        });
        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        return result;
                    }
                    return null;
                }
        );
        // Workaround JDK-8179073 (ref: https://bugs.openjdk.java.net/browse/JDK-8179073)
        this.setResizable(AppEnvironment.getInstance().isResizableDialogs());
    }

    private File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Rrd4j File(s)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RRD binary files", "*.rrd"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RRD XML dumps", "*.xml"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
            Dialogs.getInitialDir(mostRecentRrdFiles).ifPresent(fileChooser::setInitialDirectory);
            List<File> rrdFiles = fileChooser.showOpenMultipleDialog(NodeUtils.getStage(owner));
            if (rrdFiles != null) {
                pathsField.setText(rrdFiles.stream().map(File::getPath).collect(Collectors.joining(";")));
            }
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    /**
     * Returns an instance of {@link Rrd4jFileAdapter}
     *
     * @return an instance of {@link Rrd4jFileAdapter}
     * @throws DataAdapterException if the provided parameters are invalid
     */
    private DataAdapter<Double> getDataAdapter() throws DataAdapterException {
        if (pathsField.getText().isBlank()){
            throw new CannotInitializeDataAdapterException("Path cannot be blank");
        }
        List<Path> rrdFiles = Arrays.stream(pathsField.getText().split(";")).map(Paths::get).collect(Collectors.toList());
        rrdFiles.forEach(mostRecentRrdFiles::push);
        return new Rrd4jFileAdapter(rrdFiles);

    }

}
