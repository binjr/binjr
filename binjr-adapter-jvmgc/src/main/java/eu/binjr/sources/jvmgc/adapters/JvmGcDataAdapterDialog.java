/*
 * Copyright 2024 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.sources.jvmgc.adapters;

import eu.binjr.common.javafx.controls.LabelWithInlineHelp;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link JvmGcDataAdapterDialog}
 *
 * @author Frederic Thevenet
 */
public class JvmGcDataAdapterDialog extends DataAdapterDialog<Path> {
    private static final Logger logger = Logger.create(JvmGcDataAdapterDialog.class);
    private final CheckBox detectRollingLogsCheckbox;
    private int pos = 1;
    /**
     * Initializes a new instance of the {@link JvmGcDataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     * @throws NoAdapterFoundException if no adapter could be found to get preferences for.
     */
    public JvmGcDataAdapterDialog(Node owner) throws NoAdapterFoundException {
        super(owner, Mode.PATH, "mostRecentJvmGcFiles", false);
        setDialogHeaderText("Add a Hotspot GC log file");
        this.detectRollingLogsCheckbox = new CheckBox("Detect rolling logs");
        try {
            var adapterPrefs = (JvmGcAdapterPreferences) DataAdapterFactory.getInstance()
                    .getDataAdapterInfo(JvmGcDataAdapter.class.getName())
                    .getPreferences();
            detectRollingLogsCheckbox.selectedProperty().bindBidirectional(adapterPrefs.isDetectRollingLogs.property());
        } catch (NoAdapterFoundException e) {
            logger.error("Cannot bind detectRollingLogs properties to preferences: {}", e.getMessage());
            logger.debug("Stack trace", e);
        }
        addParamNode(detectRollingLogsCheckbox,"", """
                        Use the given path to find rotating log files. If the path is a file,
                        the file name is used to match other files in the directory.
                        If the path is a directory, all files in the directory are considered.""");
    }

    protected File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open GC log file");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GC log files", "*.log", "*.txt", "*."));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip archives", "*.zip", "*."));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*", "*"));
            Dialogs.getInitialDir(getMostRecentList()).ifPresent(fileChooser::setInitialDirectory);
            File selectedFile = fileChooser.showOpenDialog(NodeUtils.getStage(owner));
            if (selectedFile != null) {
                return selectedFile;
            }
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    private void addParamNode(Node field, String label, String inlineHelp) {
        GridPane.setConstraints(field, 1, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        var tabsLabel = new LabelWithInlineHelp(label, inlineHelp);
        tabsLabel.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setConstraints(tabsLabel, 0, pos, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        getParamsGridPane().getChildren().addAll(field, tabsLabel);
        pos++;
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
            return List.of(new JvmGcDataAdapter(path, ZoneId.of(getSourceTimezone()), detectRollingLogsCheckbox.isSelected()));

    }
}
