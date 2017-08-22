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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

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
    private DataAdapter result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final Set<String> suggestedUrls;

    protected final TextField urlField;
    protected final TextField timezoneField;
    protected final DialogPane parent;
    protected final Button okButton;
    protected final GridPane paramsGridPane;

    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public DataAdapterDialog(Node owner) {
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("Source");
        String KNOWN_JRDS_URL = "urls_mru";
        Preferences prefs = Preferences.userRoot().node(BINJR_SOURCES);
        suggestedUrls = new HashSet<>(Arrays.asList(prefs.get(KNOWN_JRDS_URL, "").split(" ")));
        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/DataAdapterView.fxml"));
        try {
            parent = fXMLLoader.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load /views/DataAdapterView.fxml", e);
        }
        this.setDialogPane(parent);
        urlField = (TextField) parent.lookup("#urlField");
        timezoneField = (TextField) parent.lookup("#timezoneField");
        paramsGridPane = (GridPane) parent.lookup("#paramsGridPane");
        okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Platform.runLater(urlField::requestFocus);
        autoCompletionBinding = TextFields.bindAutoCompletion(urlField, suggestedUrls);
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                ZoneId zoneId = ZoneId.of(timezoneField.getText());
                result = getDataAdapter();
                autoCompletionLearnWord(urlField);
            } catch (MalformedURLException e) {
                Dialogs.notifyError("Invalid URL", e.getLocalizedMessage(), Pos.CENTER, urlField);
                ae.consume();
            } catch (DateTimeException de) {
                Dialogs.notifyError("Invalid Timezone", de.getLocalizedMessage(), Pos.CENTER, timezoneField);
                ae.consume();
            }
        });
        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        suggestedUrls.stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> prefs.put(KNOWN_JRDS_URL, s));
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
    protected abstract DataAdapter getDataAdapter() throws MalformedURLException, DateTimeException;

    private void autoCompletionLearnWord(TextField field) {
        suggestedUrls.add(field.getText());
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedUrls);
    }
}
