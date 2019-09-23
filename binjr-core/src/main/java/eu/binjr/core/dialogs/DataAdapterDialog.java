/*
 *    Copyright 2017-2019 Frederic Thevenet
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

package eu.binjr.core.dialogs;

import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SerializedDataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import java.net.URI;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A dialog box that returns a {@link SerializedDataAdapter} built according to user inputs.
 *
 * @author Frederic Thevenet
 */
public abstract class DataAdapterDialog extends Dialog<DataAdapter> {
    private static final Logger logger = LogManager.getLogger(DataAdapterDialog.class);

    private final HBox uriHBox;
    private DataAdapter result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
   // private final Set<String> suggestedUrls;
    private final Button browseButton;
    private final Label uriLabel;
    private final ComboBox<String> uriField;
    private final TextField timezoneField;
    private final DialogPane parent;
    private final Button okButton;
    private final GridPane paramsGridPane;



    private final MostRecentlyUsedList<URI> mostRecentList;

    protected enum Mode {
        PATH,
        URL;
    }

    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *  @param owner the owner window for the dialog
     * @param mode  the mode (Path or URL) to use for the dialog.
     * @param mostRecentListName
     */
    public DataAdapterDialog(Node owner, Mode mode,String mostRecentListName) {
        this.mostRecentList = UserHistory.getInstance().uriMostRecentlyUsedList(mostRecentListName, 20);
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("Source");

        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/eu/binjr/views/DataAdapterView.fxml"));
        try {
            parent = fXMLLoader.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load /views/DataAdapterView.fxml", e);
        }
        this.setDialogPane(parent);
        browseButton = (Button) parent.lookup("#browseButton");
        uriLabel = (Label) parent.lookup("#uriLabel");
        uriField = (ComboBox<String>) parent.lookup("#uriField");
        uriField.setItems(FXCollections.observableArrayList(mostRecentList.getAll().stream().map(URI::toString).collect(Collectors.toList())));
        uriField.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue){
                autoCompletionBinding.dispose();
            }else{
                updateUriAutoCompletionBinding();
            }
        });
        timezoneField = (TextField) parent.lookup("#timezoneField");
        paramsGridPane = (GridPane) parent.lookup("#paramsGridPane");
        uriHBox = (HBox) parent.lookup("#uriHBox");
        if (mode == Mode.URL) {
            this.browseButton.setPrefWidth(0);
            this.uriHBox.setSpacing(0);
            this.uriLabel.setText("Address:");
        } else {
            this.browseButton.setPrefWidth(-1);
            this.uriLabel.setText("Path:");
        }
        this.browseButton.setOnAction(event -> {
            File selectedFile = displayFileChooser((Node) event.getSource());
            if (selectedFile != null) {
                uriField.setValue(selectedFile.getPath());
            }
        });
        okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Platform.runLater(uriField::requestFocus);
        updateUriAutoCompletionBinding();
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                ZoneId zoneId = ZoneId.of(timezoneField.getText());
                result = getDataAdapter();
               // suggestedUrls.add(uriField.getValue());
                mostRecentList.push(URI.create(uriField.getValue()));
                updateUriAutoCompletionBinding();
            } catch (DateTimeException e) {
                Dialogs.notifyError("Invalid Timezone", e, Pos.CENTER, timezoneField);
                ae.consume();
            } catch (CannotInitializeDataAdapterException e) {
                Dialogs.notifyError("Error initializing adapter to source", e, Pos.CENTER, timezoneField);
                ae.consume();
            } catch (DataAdapterException e) {
                Dialogs.notifyError("Error with the adapter to source", e, Pos.CENTER, timezoneField);
                ae.consume();
            } catch (Throwable e) {
                Dialogs.notifyError("Unexpected error while retrieving data adapter", e, Pos.CENTER, timezoneField);
                ae.consume();
            }
        });
        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                      //  suggestedUrls.stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> prefs.put(mruKey, s));
                        return result;
                    }
                    return null;
                }
        );
        TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());
        timezoneField.setText(ZoneId.systemDefault().toString());
        // Workaround JDK-8179073 (ref: https://bugs.openjdk.java.net/browse/JDK-8179073)
        this.setResizable(AppEnvironment.getInstance().isResizableDialogs());
    }

    /**
     * Returns an instance of {@link SerializedDataAdapter}
     *
     * @return an instance of {@link SerializedDataAdapter}
     * @throws DataAdapterException if the provided {@link ZoneId} is invalid
     */
    protected abstract DataAdapter getDataAdapter() throws DataAdapterException;

    protected File displayFileChooser(Node owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        //FIXME
      //  fileChooser.setInitialDirectory(GlobalPreferences.getInstance().getMostRecentSaveFolder().toFile());
        return fileChooser.showOpenDialog(Dialogs.getStage(owner));
    }

    private void updateUriAutoCompletionBinding() {
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(uriField.getEditor(),
                mostRecentList.getAll().stream().map(URI::toString).collect(Collectors.toList()));
        autoCompletionBinding.setPrefWidth(uriField.getPrefWidth());
    }

    public String getSourceUri(){
        return this.uriField.getValue();
    }

    public void setSourceUri(String value){
        this.uriField.setValue(value);
    }

    public String getSourceTimezone(){
        return this.timezoneField.getText();
    }

    public void setSourceTimezone(String value){
        this.timezoneField.setText(value);
    }

    public String getDialogHeaderText(){
        return this.parent.getHeaderText();
    }

    public void setDialogHeaderText(String value){
        this.parent.setHeaderText(value);
    }

    public GridPane getParamsGridPane() {
        return paramsGridPane;
    }

    protected MostRecentlyUsedList<URI> getMostRecentList() {
        return mostRecentList;
    }

}
