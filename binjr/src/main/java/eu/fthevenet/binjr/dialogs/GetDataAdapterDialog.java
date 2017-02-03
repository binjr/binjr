package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.controllers.GetDataAdapterController;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;
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
public class GetDataAdapterDialog extends Dialog<DataAdapter> {
    private static final Logger logger = LogManager.getLogger(GetDataAdapterDialog.class);
    private static final String BINJR_SOURCES = "binjr/sources";
    private DataAdapter result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final Set<String> suggestedUrls;

    /**
     * Initializes a new instance of the {@link GetDataAdapterDialog} class.
     *
     * @param owner          the owner window for the dialog
     * @param title          the title for the dialog box
     * @param adapterFactory a factory for {@link DataAdapter}
     */
    public GetDataAdapterDialog(Window owner, String title, DataAdapterFactory adapterFactory) {
        if (owner != null) {
            this.initOwner(owner);
        }
        this.setTitle(title);
        String KNOWN_JRDS_URL = "urls_mru";
        Preferences prefs = Preferences.userRoot().node(BINJR_SOURCES);
        suggestedUrls = new HashSet<>(Arrays.asList(prefs.get(KNOWN_JRDS_URL, "").split(" ")));

        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            this.setDialogPane(fXMLLoader.load(getClass().getResource("/views/GetDataAdapterView.fxml").openStream()));
            GetDataAdapterController ctlr = fXMLLoader.getController();
            autoCompletionBinding = TextFields.bindAutoCompletion(ctlr.getUrlField(), suggestedUrls);
            final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, ae -> {
                try {
                    ZoneId zoneId = ZoneId.of(ctlr.getTimezoneField().getText());
                    result = adapterFactory.fromUrl(ctlr.getUrlField().getText(), zoneId);
                    autoCompletionLearnWord(ctlr.getUrlField());
                } catch (MalformedURLException e) {
                    notifyException("Invalid URL", e, ctlr.getUrlField());
                    ae.consume();
                } catch (DateTimeException de) {
                    notifyException("Invalid Timezone", de, ctlr.getTimezoneField());
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
            TextFields.bindAutoCompletion(ctlr.getTimezoneField(), ZoneId.getAvailableZoneIds());
            ctlr.getTimezoneField().setText(ZoneId.systemDefault().toString());
        } catch (IOException e) {
            logger.error("Failed to load /views/GetDataAdapterView.fxml", e);
        }

    }

    private void notifyException(String title, Exception e, Node owner) {
        Notifications.create()
                .title(title)
                .text(e.getLocalizedMessage())
                .hideAfter(Duration.seconds(3))
                .position(Pos.CENTER)
                .owner(owner).showError();
    }

    private void autoCompletionLearnWord(TextField field) {
        suggestedUrls.add(field.getText());
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedUrls);
    }
}
