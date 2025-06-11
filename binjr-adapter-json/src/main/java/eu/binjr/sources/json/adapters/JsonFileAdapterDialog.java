/*
 *    Copyright 2025 Frederic Thevenet
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

package eu.binjr.sources.json.adapters;

import eu.binjr.common.javafx.controls.LabelWithInlineHelp;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.json.data.parsers.BuiltInJsonParsingProfile;
import eu.binjr.sources.json.data.parsers.JsonParsingProfile;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.controlsfx.control.textfield.TextFields;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link JsonFileAdapter}
 *
 * @author Frederic Thevenet
 */
public class JsonFileAdapterDialog extends DataAdapterDialog<Path> {
    private final TextField encodingField;
    private int pos = 2;
    private final JsonAdapterPreferences prefs;
    private final ChoiceBox<JsonParsingProfile> parsingChoiceBox = new ChoiceBox<>();

    /**
     * Initializes a new instance of the {@link JsonFileAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public JsonFileAdapterDialog(Node owner) throws NoAdapterFoundException {
        super(owner, Mode.PATH, "mostRecentJsonFiles", true);
        this.prefs = (JsonAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JsonFileAdapter.class.getName());
        this.setUriLabelInlineHelp("""
              The path of the JSON file.
                """);
        this.setTimezoneLabelInlineHelp("""
                The default timezone for timestamps in the JSON file.
                """);
        this.setDialogHeaderText("Add a JSON file");
        this.encodingField = new TextField(prefs.mruEncoding.get());
        TextFields.bindAutoCompletion(encodingField, Charset.availableCharsets().keySet());
        addParamField(this.encodingField, "Encoding", """
                The text encoding of the JSON file. 
                """);
        addParsingField(owner);
    }

    private void addParsingField(Node owner) {
        var parsingLabel = new LabelWithInlineHelp("Parsing profile", """
                    The parsing profile used to process timestamps in the JSON file.
                """);
        parsingLabel.setAlignment(Pos.CENTER_RIGHT);
        var parsingHBox = new HBox();
        parsingHBox.setSpacing(5);
        updateProfileList(prefs.jsonTimestampParsingProfiles.get());
        parsingChoiceBox.setMaxWidth(Double.MAX_VALUE);
        var editParsingButton = new Button("Edit");
        editParsingButton.setOnAction(event -> {
            try {
                new JsonParsingProfileDialog(this.getOwner(), parsingChoiceBox.getValue()).showAndWait().ifPresent(selection -> {
                    prefs.mostRecentlyUsedParsingProfile.set(selection.getProfileId());
                    updateProfileList(prefs.jsonTimestampParsingProfiles.get());
                });
            } catch (Exception e) {
                Dialogs.notifyException("Failed to show parsing profile windows", e, owner);
            }
        });
        parsingHBox.getChildren().addAll(parsingChoiceBox, editParsingButton);
        HBox.setHgrow(parsingChoiceBox, Priority.ALWAYS);
        GridPane.setConstraints(parsingLabel, 0, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        GridPane.setConstraints(parsingHBox, 1, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        getParamsGridPane().getChildren().addAll(parsingLabel, parsingHBox);
        pos++;
    }

    private void addParamField(TextField field, String label, String inlineHelp) {
        GridPane.setConstraints(field, 1, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        var tabsLabel = new LabelWithInlineHelp(label, inlineHelp);
        tabsLabel.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setConstraints(tabsLabel, 0, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        getParamsGridPane().getChildren().addAll(field, tabsLabel);
        pos++;
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open JSON file");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values files", "*.json"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*", "*"));
            Dialogs.getInitialDir(getMostRecentList()).ifPresent(fileChooser::setInitialDirectory);
            return fileChooser.showOpenDialog(NodeUtils.getStage(owner));
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    @Override
    protected Collection<DataAdapter> getDataAdapters() throws DataAdapterException {
        Path jsonPath = Paths.get(getSourceUri());
        if (!Files.exists(jsonPath)) {
            throw new CannotInitializeDataAdapterException("Cannot find " + getSourceUri());
        }
        if (!jsonPath.isAbsolute()) {
            throw new CannotInitializeDataAdapterException("The provided path is not valid.");
        }
        getMostRecentList().push(jsonPath);
        prefs.mostRecentlyUsedParsingProfile.set(parsingChoiceBox.getValue().getProfileId());
        String charsetName = encodingField.getText();
        if (!Charset.isSupported(charsetName)) {
            throw new CannotInitializeDataAdapterException("Invalid or unsupported encoding: " + charsetName);
        }
        prefs.mruEncoding.set(charsetName);
        return List.of(new JsonFileAdapter(
                getSourceUri(),
                ZoneId.of(getSourceTimezone()),
                charsetName,
                parsingChoiceBox.getValue()));
    }

    private void updateProfileList(JsonParsingProfile[] newValue) {
        parsingChoiceBox.getItems().clear();
        parsingChoiceBox.getItems().setAll(BuiltInJsonParsingProfile.values());
        parsingChoiceBox.getItems().addAll(newValue);
        parsingChoiceBox.getSelectionModel().select(parsingChoiceBox.getItems().stream()
                .filter(p -> Objects.equals(p.getProfileId(), prefs.mostRecentlyUsedParsingProfile.get()))
                .findAny().orElse(BuiltInJsonParsingProfile.ISO));
    }

}
