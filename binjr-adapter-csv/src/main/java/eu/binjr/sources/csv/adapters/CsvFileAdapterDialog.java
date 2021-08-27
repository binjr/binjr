/*
 *    Copyright 2017-2021 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;

import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserHistory;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;


/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link CsvFileAdapter}
 *
 * @author Frederic Thevenet
 */
public class CsvFileAdapterDialog extends DataAdapterDialog<Path> {
    private final TextField dateFormatPattern = new TextField("yyyy-MM-dd HH:mm:ss");
    private final TextField encodingField = new TextField("utf-8");
    private final TextField separatorField = new TextField(",");
    private int pos = 2;

    /**
     * Initializes a new instance of the {@link CsvFileAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public CsvFileAdapterDialog(Node owner) {
        super(owner, Mode.PATH,"mostRecentCsvFiles", true);
        this.setDialogHeaderText("Add a csv file");
        addParamField(this.dateFormatPattern, "Date Format:");
        addParamField(this.encodingField, "Encoding:");
        addParamField(this.separatorField, "Separator:");
    }

    private void addParamField(TextField field, String label) {
        GridPane.setConstraints(field, 1, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        Label tabsLabel = new Label(label);
        GridPane.setConstraints(tabsLabel, 0, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        getParamsGridPane().getChildren().addAll(field, tabsLabel);
        pos++;
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open CSV file");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values files", "*.csv"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
            Dialogs.getInitialDir(getMostRecentList()).ifPresent(fileChooser::setInitialDirectory);
            return fileChooser.showOpenDialog(Dialogs.getStage(owner));
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    @Override
    protected Collection<DataAdapter> getDataAdapters() throws DataAdapterException {
        Path csvPath = Paths.get(getSourceUri());
        if (!Files.exists(csvPath)) {
            throw new CannotInitializeDataAdapterException("Cannot find " + getSourceUri());
        }
        if (!csvPath.isAbsolute()) {
            throw new CannotInitializeDataAdapterException("The provided path is not valid.");
        }
        getMostRecentList().push(csvPath);
        return List.of(new CsvFileAdapter(
                getSourceUri(),
                ZoneId.of(getSourceTimezone()),
                encodingField.getText(),
                dateFormatPattern.getText(),
                separatorField.getText().charAt(0)));
    }
}
