/*
 *    Copyright 2020-2021 Frederic Thevenet
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

package eu.binjr.sources.logs.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.dialogs.ParsingProfileDialog;
import eu.binjr.core.preferences.UserPreferences;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link LogsDataAdapterDialog}
 *
 * @author Frederic Thevenet
 */
public class LogsDataAdapterDialog extends DataAdapterDialog<Path> {
    private static final Logger logger = Logger.create(LogsDataAdapterDialog.class);
    private final TextField extensionFiltersTextField;
    private final LogsAdapterPreferences prefs;
    private final ChoiceBox<ParsingProfile> parsingChoiceBox = new ChoiceBox<>();


    private void updateProfileList(ParsingProfile[] newValue) {
        parsingChoiceBox.getItems().clear();
        parsingChoiceBox.getItems().setAll(BuiltInParsingProfile.values());
        parsingChoiceBox.getItems().addAll(newValue);
        parsingChoiceBox.getSelectionModel().select(parsingChoiceBox.getItems().stream()
                .filter(p -> p.getProfileName().equals(prefs.mostRecentlyUsedParsingProfile.get()))
                .findAny().orElse(BuiltInParsingProfile.ISO));
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     * @throws NoAdapterFoundException if an error occurs initializing the dialog.
     */
    public LogsDataAdapterDialog(Node owner) throws NoAdapterFoundException {
        super(owner, Mode.PATH, "mostRecentLogsArchives", true);
        this.prefs = (LogsAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(LogsDataAdapter.class.getName());
        setDialogHeaderText("Add a Zip Archive or Folder");
        extensionFiltersTextField = new TextField(String.join(", ", prefs.fileExtensionFilters.get()));
        var label = new Label("Extensions:");
        GridPane.setConstraints(label, 0, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        GridPane.setConstraints(extensionFiltersTextField, 1, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));

        var parsingLabel = new Label("Parsing:");
        var parsingHBox = new HBox();
        parsingHBox.setSpacing(5);
        updateProfileList(UserPreferences.getInstance().userParsingProfiles.get());
        parsingChoiceBox.setMaxWidth(Double.MAX_VALUE);
        var editParsingButton = new Button("Edit");
        editParsingButton.setOnAction(event -> {
            try {
                new ParsingProfileDialog(this.getOwner(), parsingChoiceBox.getValue()).showAndWait().ifPresent(selection -> {
                    prefs.mostRecentlyUsedParsingProfile.set(selection.getProfileName());
                    updateProfileList( UserPreferences.getInstance().userParsingProfiles.get());
                });
            } catch (Exception e) {
                Dialogs.notifyException("Failed to show parsing profile windows", e, owner);
            }
        });
        parsingHBox.getChildren().addAll(parsingChoiceBox, editParsingButton);
        HBox.setHgrow(parsingChoiceBox, Priority.ALWAYS);
        GridPane.setConstraints(parsingLabel, 0, 3, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        GridPane.setConstraints(parsingHBox, 1, 3, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));

        getParamsGridPane().getChildren().addAll(label, extensionFiltersTextField, parsingLabel, parsingHBox);
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            ContextMenu sourceMenu = new ContextMenu();
            MenuItem menuItem = new MenuItem("Zip file");
            menuItem.setOnAction(eventHandler -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Zip Archive");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip archive", "*.zip"));
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
                Dialogs.getInitialDir(getMostRecentList()).ifPresent(fileChooser::setInitialDirectory);
                File selectedFile = fileChooser.showOpenDialog(Dialogs.getStage(owner));
                if (selectedFile != null) {
                    setSourceUri(selectedFile.getPath());
                }
            });
            sourceMenu.getItems().add(menuItem);
            MenuItem folderMenuItem = new MenuItem("Folder");
            folderMenuItem.setOnAction(eventHandler -> {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Open Folder");
                Dialogs.getInitialDir(getMostRecentList()).ifPresent(dirChooser::setInitialDirectory);
                File selectedFile = dirChooser.showDialog(Dialogs.getStage(owner));
                if (selectedFile != null) {
                    setSourceUri(selectedFile.getPath());
                }
            });
            sourceMenu.getItems().add(folderMenuItem);
            sourceMenu.show(owner, Side.RIGHT, 0, 0);
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    @Override
    protected Collection<DataAdapter> getDataAdapters() throws DataAdapterException {
        Path path = Paths.get(getSourceUri());
        if (!Files.exists(path)) {
            throw new CannotInitializeDataAdapterException("Cannot find " + getSourceUri());
        }
        if (!path.isAbsolute()) {
            throw new CannotInitializeDataAdapterException("The provided path is not valid.");
        }
        getMostRecentList().push(path);
        prefs.fileExtensionFilters.set(Arrays.stream(extensionFiltersTextField.getText().split("[,;\" ]+")).filter(e -> !e.isBlank()).toArray(String[]::new));
        prefs.mostRecentlyUsedParsingProfile.set(parsingChoiceBox.getValue().getProfileName());

        return List.of(new LogsDataAdapter(path,
                ZoneId.of(getSourceTimezone()),
                prefs.folderFilters.get(),
                prefs.fileExtensionFilters.get(),
                parsingChoiceBox.getValue()));
    }
}
