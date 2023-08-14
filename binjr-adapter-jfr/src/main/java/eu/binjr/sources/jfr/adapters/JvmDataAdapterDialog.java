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
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.jfr.adapters.gc.GcDataAdapter;
import eu.binjr.sources.jfr.adapters.jfr.events.JfrEventsAdapterPreferences;
import eu.binjr.sources.jfr.adapters.jfr.charts.JfrChartsDataAdapter;
import eu.binjr.sources.jfr.adapters.jfr.events.JfrEventsDataAdapter;
import eu.binjr.sources.logs.adapters.LogsDataAdapter;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.jcl.LogAdapter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link JvmDataAdapterDialog}
 *
 * @author Frederic Thevenet
 */
public class JvmDataAdapterDialog extends DataAdapterDialog<Path> {
    private static final Logger logger = Logger.create(JvmDataAdapterDialog.class);
    // private final TextField extensionFiltersTextField;
    private final JfrEventsAdapterPreferences prefs;
    private static final Gson gson = new Gson();

    /**
     * Initializes a new instance of the {@link JvmDataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     * @throws NoAdapterFoundException if no adapter could be found to get preferences for.
     */
    public JvmDataAdapterDialog(Node owner) throws NoAdapterFoundException {
        super(owner, Mode.PATH, "mostRecentJfrFiles", false);
        this.prefs = (JfrEventsAdapterPreferences) DataAdapterFactory.getInstance().getAdapterPreferences(JfrEventsDataAdapter.class.getName());
        setDialogHeaderText("Add a JFR File");
      /*  extensionFiltersTextField = new TextField(gson.toJson(prefs.fileExtensionFilters.get()));
        var label = new Label("Extensions:");
        GridPane.setConstraints(label, 0, 1, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        GridPane.setConstraints(extensionFiltersTextField, 1, 1, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));

        getParamsGridPane().getChildren().addAll(label, extensionFiltersTextField);*/
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            ContextMenu sourceMenu = new ContextMenu();
            MenuItem jfrMenuItem = getMenuItem("JFR file", "Open JFR File", new FileChooser.ExtensionFilter("JFR files", "*.jfr"), owner);
            sourceMenu.getItems().add(jfrMenuItem);

            MenuItem gcMenuItem = getMenuItem("GC log file", "Open Zip Archive", new FileChooser.ExtensionFilter("GC log files", "*.log", "*.txt", "*."), owner);
            sourceMenu.getItems().add(gcMenuItem);

            sourceMenu.show(owner, Side.RIGHT, 0, 0);

        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    private MenuItem getMenuItem(String JFR_file, String Open_JFR_File, FileChooser.ExtensionFilter JFR_files, Node owner) {
        MenuItem jfrMenuItem = new MenuItem(JFR_file);
        jfrMenuItem.setOnAction(eventHandler -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Open_JFR_File);
            fileChooser.getExtensionFilters().add(JFR_files);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*", "*"));
            Dialogs.getInitialDir(getMostRecentList()).ifPresent(fileChooser::setInitialDirectory);
            File selectedFile = fileChooser.showOpenDialog(NodeUtils.getStage(owner));
            if (selectedFile != null) {
                setSourceUri(selectedFile.getPath());
            }
        });
        return jfrMenuItem;
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
        if (path.getFileName().endsWith("jfr")) {
            return List.of(new JfrEventsDataAdapter(path, ZoneId.of(getSourceTimezone())),
                    new JfrChartsDataAdapter(path, ZoneId.of(getSourceTimezone())));
        }else{
            return List.of(new GcDataAdapter(path, ZoneId.of(getSourceTimezone())),
                    new LogsDataAdapter(path,
                            ZoneId.of(getSourceTimezone()),
                            StandardCharsets.UTF_8,
                            new String[]{"*"},
                            new String[]{"*"},
                            BuiltInParsingProfile.JVM));
        }
    }
}
