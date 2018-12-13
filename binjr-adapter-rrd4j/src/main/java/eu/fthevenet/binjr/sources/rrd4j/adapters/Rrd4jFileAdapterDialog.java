/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.binjr.sources.rrd4j.adapters;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.exceptions.CannotInitializeDataAdapterException;
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.binjr.dialogs.DataAdapterDialog;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class Rrd4jFileAdapterDialog extends DataAdapterDialog {
    List<File> rrdFiles = new ArrayList<>();

    /**
     * Initializes a new instance of the {@link Rrd4jFileAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public Rrd4jFileAdapterDialog(Node owner) {
        super(owner, Mode.PATH);
        this.parent.setHeaderText("Add Rrd4j files");
    }

    @Override
    protected File displayFileChooser(Node owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Rrd4j Files");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RRD file", "*.rrd"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        fileChooser.setInitialDirectory(GlobalPreferences.getInstance().getMostRecentSaveFolder().toFile());
        rrdFiles = fileChooser.showOpenMultipleDialog(Dialogs.getStage(owner));
        if (rrdFiles != null) {
            uriField.setText(rrdFiles.stream().map(File::getPath).collect(Collectors.joining(";")));
        }
        return null;
    }

    @Override
    protected DataAdapter<?> getDataAdapter() throws DataAdapterException {
        List<Path> rrdFile = Arrays.stream(uriField.getText().split(";")).map(s -> Paths.get(s)).collect(Collectors.toList());
//        if (!Files.exists(rrdFile)) {
//            throw new CannotInitializeDataAdapterException("Cannot find " + uriField.getText());
//        }
        rrdFile.stream().findFirst().ifPresent(path -> {
            GlobalPreferences.getInstance().setMostRecentSaveFolder(path.getParent());
        });

        return new Rrd4jFileAdapter(rrdFile, ZoneId.of(this.timezoneField.getText()));

    }
}
