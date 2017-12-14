/*
 *    Copyright 2017 Frederic Thevenet
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

package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.exceptions.CannotInitializeDataAdapterException;
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * A dialog box that returns a {@link DataAdapter} built according to user inputs.
 *
 * @author Frederic Thevenet
 */
public abstract class DataAdapterDialog extends Dialog<DataAdapter> {
    private static final Logger logger = LogManager.getLogger(DataAdapterDialog.class);
    private static final String BINJR_SOURCES = "binjr/sources";
    private final HBox uriHBox;
    private DataAdapter result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final Set<String> suggestedUrls;
    protected final Button browseButton;
    protected final Label uriLabel;
    protected final TextField uriField;
    protected final TextField timezoneField;
    protected final DialogPane parent;
    protected final Button okButton;
    protected final GridPane paramsGridPane;

    protected enum Mode {
        PATH,
        URL;
    }

    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public DataAdapterDialog(Node owner, Mode mode) {
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("Source");
        String mruKey = "mru_" + mode.toString();
        Preferences prefs = Preferences.userRoot().node(BINJR_SOURCES);
        suggestedUrls = new HashSet<>(Arrays.asList(prefs.get(mruKey, "").split(" ")));
        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/DataAdapterView.fxml"));
        try {
            parent = fXMLLoader.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load /views/DataAdapterView.fxml", e);
        }
        this.setDialogPane(parent);
        browseButton = (Button) parent.lookup("#browseButton");

        uriLabel = (Label) parent.lookup("#uriLabel");
        uriField = (TextField) parent.lookup("#uriField");
        timezoneField = (TextField) parent.lookup("#timezoneField");
        paramsGridPane = (GridPane) parent.lookup("#paramsGridPane");
        uriHBox = (HBox) parent.lookup("#uriHBox");
        if (mode == Mode.URL) {
            this.browseButton.setPrefWidth(0);
            this.uriHBox.setSpacing(0);
            this.uriLabel.setText("Address:");
        }
        else {
            this.browseButton.setPrefWidth(-1);
            this.uriLabel.setText("Path:");
        }

        this.browseButton.setOnAction(event -> {
            File selectedFile = displayFileChooser(owner);
            if (selectedFile != null) {
                uriField.setText(selectedFile.getPath());
            }
        });

        okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Platform.runLater(uriField::requestFocus);
        autoCompletionBinding = TextFields.bindAutoCompletion(uriField, suggestedUrls);
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                ZoneId zoneId = ZoneId.of(timezoneField.getText());
                result = getDataAdapter();
                autoCompletionLearnWord(uriField);
            } catch (DateTimeException e) {
                Dialogs.notifyError("Invalid Timezone", e.getMessage(), Pos.CENTER, timezoneField);
                ae.consume();
            } catch (CannotInitializeDataAdapterException e) {
                logger.debug(() -> "Stack trace", e);
                Dialogs.notifyError("Error initializing adapter to source", e.getMessage(), Pos.CENTER, timezoneField);
                ae.consume();
            } catch (DataAdapterException e) {
                logger.debug(() -> "Stack trace", e);
                Dialogs.notifyError("Error with the adapter to source", e.getMessage(), Pos.CENTER, timezoneField);
                ae.consume();
            }
        });
        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        suggestedUrls.stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> prefs.put(mruKey, s));
                        return result;
                    }
                    return null;
                }
        );
        TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());
        timezoneField.setText(ZoneId.systemDefault().toString());
    }

    /**
     * Returns an instance of {@link DataAdapter}
     *
     * @return an instance of {@link DataAdapter}
     * @throws MalformedURLException if the provided url is invalid
     * @throws DateTimeException     if the provided {@link ZoneId] is invalid
     */
    protected abstract DataAdapter<?, ?> getDataAdapter() throws DataAdapterException;

    protected File displayFileChooser(Node owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        fileChooser.setInitialDirectory(new File(GlobalPreferences.getInstance().getMostRecentSaveFolder()));
        return fileChooser.showOpenDialog(Dialogs.getStage(owner));
    }

    private void autoCompletionLearnWord(TextField field) {
        suggestedUrls.add(field.getText());
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedUrls);
    }
}
