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

package eu.binjr.core.dialogs;

import eu.binjr.common.javafx.controls.LabelWithInlineHelp;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SerializedDataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A dialog box that returns a {@link SerializedDataAdapter} built according to user inputs.
 *
 * @author Frederic Thevenet
 */
public abstract class DataAdapterDialog<T> extends Dialog<Collection<DataAdapter>> {
    private static final Logger logger = Logger.create(DataAdapterDialog.class);
    private final Mode mode;
    private Collection<DataAdapter> result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final ComboBox<String> uriField;
    private final TextField timezoneField;
    private final LabelWithInlineHelp timezoneLabel;
    private final DialogPane parent;
    private final GridPane paramsGridPane;
    private final MostRecentlyUsedList<T> mostRecentList;
    private final StringProperty uriLabelInlineHelp = new SimpleStringProperty();
    private final StringProperty timezoneLabelInlineHelp = new SimpleStringProperty();

    protected enum Mode {
        PATH(Path.class),
        URI(URI.class);
        private final Class<?> type;

        Mode(Class<?> type) {
            this.type = type;
        }
    }

    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *
     * @param owner              the owner window for the dialog
     * @param mode               the mode (Path or URL) to use for the dialog.
     * @param showTimezone       set to true to display the Timezone text field, false otherwise.
     * @param mostRecentListName the name of the {@link MostRecentlyUsedList} preference associated with this dialog.
     */
    public DataAdapterDialog(Node owner, Mode mode, String mostRecentListName, boolean showTimezone) {
        if (owner != null) {
            this.initOwner(NodeUtils.getStage(owner));
        }
        this.setTitle("Source");
        this.mode = mode;

        switch (mode) {
            case PATH:
                this.mostRecentList = (MostRecentlyUsedList<T>) UserHistory.getInstance().pathMostRecentlyUsedList(mostRecentListName, 20, false);
                break;
            case URI:
                this.mostRecentList = (MostRecentlyUsedList<T>) UserHistory.getInstance().uriMostRecentlyUsedList(mostRecentListName, 20);
                break;
            default:
                throw new UnsupportedOperationException("Unknown mode type");
        }

        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/eu/binjr/views/DataAdapterView.fxml"));
        try {
            parent = fXMLLoader.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load /views/DataAdapterView.fxml", e);
        }
        this.setDialogPane(parent);
        Button browseButton = (Button) parent.lookup("#browseButton");
        LabelWithInlineHelp uriLabel = (LabelWithInlineHelp) parent.lookup("#uriLabel");
        uriField = (ComboBox<String>) parent.lookup("#uriField");


        uriField.setItems(FXCollections.observableArrayList(mostRecentList.getAll().stream().map(Object::toString).collect(Collectors.toList())));
        uriField.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                autoCompletionBinding.dispose();
            } else {
                updateUriAutoCompletionBinding();
            }
        });
        uriField.getSelectionModel().selectedItemProperty().addListener((obs, oldText, uri) -> {
            if (!uri.isEmpty()) {
                validateUri(uri);
            }
        });
        timezoneField = (TextField) parent.lookup("#timezoneField");
        timezoneLabel = (LabelWithInlineHelp) parent.lookup("#timeZoneLabel");
        timezoneLabel.inlineHelpProperty().bind(timezoneLabelInlineHelp);
        setTimezoneLabelInlineHelp("The timezone of the source.");
        timezoneField.setManaged(showTimezone);
        timezoneField.setVisible(showTimezone);
        timezoneLabel.setManaged(showTimezone);
        timezoneLabel.setVisible(showTimezone);
        paramsGridPane = (GridPane) parent.lookup("#paramsGridPane");
        HBox uriHBox = (HBox) parent.lookup("#uriHBox");
        if (mode == Mode.URI) {
            browseButton.setPrefWidth(0);
            uriHBox.setSpacing(0);
            uriLabel.setText("Address");
            setUriLabelInlineHelp("The address to access the source.");
        } else {
            browseButton.setPrefWidth(-1);
            uriLabel.setText("Path");
            setUriLabelInlineHelp("The path to access the source.");

        }
        uriLabel.inlineHelpProperty().bind(uriLabelInlineHelp);
        browseButton.setOnAction(event -> {
            File selectedFile = displayFileChooser((Node) event.getSource());
            if (selectedFile != null) {
                uriField.setValue(selectedFile.getPath());
            }
        });
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Platform.runLater(uriField::requestFocus);
        updateUriAutoCompletionBinding();
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                ZoneId zoneId = ZoneId.of(timezoneField.getText());
                result = getDataAdapters();
                switch (mode) {
                    case PATH:
                        mostRecentList.push((T) Path.of(uriField.getEditor().getText()));
                        break;
                    case URI:
                        mostRecentList.push((T) URI.create(uriField.getEditor().getText()));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown mode type");
                }
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

    protected void validateUri(String uri) {
    }

    /**
     * Returns an instance of {@link SerializedDataAdapter}
     *
     * @return an instance of {@link SerializedDataAdapter}
     * @throws DataAdapterException if the provided {@link ZoneId} is invalid
     */
    protected abstract Collection<DataAdapter> getDataAdapters() throws DataAdapterException;

    protected File displayFileChooser(Node owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        return fileChooser.showOpenDialog(NodeUtils.getStage(owner));
    }

    private void updateUriAutoCompletionBinding() {
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(uriField.getEditor(),
                mostRecentList.getAll().stream().map(Object::toString).collect(Collectors.toList()));
        autoCompletionBinding.setPrefWidth(uriField.getPrefWidth());
    }

    public String getSourceUri() {
        return this.uriField.getEditor().getText();
    }

    public void setSourceUri(String value) {
        this.uriField.setValue(value);
    }

    public String getSourceTimezone() {
        return this.timezoneField.getText();
    }

    public void setSourceTimezone(String value) {
        this.timezoneField.setText(value);
    }

    public String getDialogHeaderText() {
        return this.parent.getHeaderText();
    }

    public void setDialogHeaderText(String value) {
        this.parent.setHeaderText(value);
    }

    public GridPane getParamsGridPane() {
        return paramsGridPane;
    }

    protected MostRecentlyUsedList<T> getMostRecentList() {
        return mostRecentList;
    }

    public String getUriLabelInlineHelp() {
        return uriLabelInlineHelp.get();
    }

    public StringProperty uriLabelInlineHelpProperty() {
        return uriLabelInlineHelp;
    }

    public void setUriLabelInlineHelp(String uriLabelInlineHelp) {
        this.uriLabelInlineHelp.set(uriLabelInlineHelp);
    }

    public String getTimezoneLabelInlineHelp() {
        return timezoneLabelInlineHelp.get();
    }

    public StringProperty timezoneLabelInlineHelpProperty() {
        return timezoneLabelInlineHelp;
    }

    public void setTimezoneLabelInlineHelp(String timezoneLabelInlineHelp) {
        this.timezoneLabelInlineHelp.set(timezoneLabelInlineHelp);
    }


}
