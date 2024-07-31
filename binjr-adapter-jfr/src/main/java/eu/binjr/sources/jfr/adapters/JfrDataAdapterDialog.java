/*
 *    Copyright 2023 Frederic Thevenet
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

package eu.binjr.sources.jfr.adapters;

import com.google.gson.Gson;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.jfr.adapters.charts.JfrChartsDataAdapter;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link JfrDataAdapterDialog}
 *
 * @author Frederic Thevenet
 */
public class JfrDataAdapterDialog extends DataAdapterDialog<Path> {
    private static final Logger logger = Logger.create(JfrDataAdapterDialog.class);
    // private final TextField extensionFiltersTextField;
    private final JfrAdapterPreferences prefs;
    private static final Gson gson = new Gson();

    /**
     * Initializes a new instance of the {@link JfrDataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     * @throws NoAdapterFoundException if no adapter could be found to get preferences for.
     */
    public JfrDataAdapterDialog(Node owner) throws NoAdapterFoundException {
        super(owner, Mode.PATH, "mostRecentJfrFiles", false);
        this.prefs = (JfrAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JfrDataAdapter.class.getName());
        setDialogHeaderText("Add a JFR File");
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open JFR File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JFR files", "*.jfr"));
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
        return List.of(new JfrDataAdapter(path, ZoneId.of(getSourceTimezone())),
                new JfrChartsDataAdapter(path, ZoneId.of(getSourceTimezone())));
    }
}
