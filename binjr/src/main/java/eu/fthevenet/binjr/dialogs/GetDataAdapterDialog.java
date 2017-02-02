package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterFactory;
import eu.fthevenet.binjr.controllers.GetDataAdapterController;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Created by FTT2 on 02/02/2017.
 */
public class GetDataAdapterDialog extends Dialog<DataAdapter> {
    private static final Logger logger = LogManager.getLogger(GetDataAdapterDialog.class);
    private static final String BINJR_JRDS = "binjr/sources/jrds";
    private DataAdapter result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final Set<String> suggestedUrls;

//    public GetDataAdapterDialog(Supplier<DataAdapter<T>> adapterFactory){
//        this(null, adapterFactory);
//    }

    public GetDataAdapterDialog(Window owner, String title, DataAdapterFactory adapterFactory) {
        if (owner!=null){
            this.initOwner(owner);
        }

        String KNOWN_JRDS_URL = "urls_mru";

        Preferences prefs = Preferences.userRoot().node(BINJR_JRDS);
        suggestedUrls = new HashSet<>(Arrays.asList(prefs.get(KNOWN_JRDS_URL, "").split(" ")));

        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            this.setDialogPane(fXMLLoader.load(getClass().getResource("/views/GetDataAdapterView.fxml").openStream()));
            GetDataAdapterController ctlr = fXMLLoader.getController();
            autoCompletionBinding = TextFields.bindAutoCompletion(ctlr.getUrlField(), suggestedUrls);

            final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, ae -> {
                try {
                    result = adapterFactory.fromUrl(ctlr.getUrlField().getText(), ZoneId.systemDefault());// JRDSDataAdapter.fromUrl(ctlr.getUrlField().getText(), ZoneId.systemDefault());
                    autoCompletionLearnWord(ctlr.getUrlField());
                } catch (MalformedURLException e) {
                    notifyException("Invalid URL", e, ctlr.getUrlField());
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

        } catch (IOException e) {
            logger.error("Failed to load /views/GetDataAdapterView.fxml", e);
        }

    }

    private void notifyException(String title, Exception e, Node owner) {
        Notifications.create()
                .title(title)
                .text(e.getLocalizedMessage())
                .hideAfter(Duration.seconds(4))
                .position(Pos.BOTTOM_CENTER)
                .owner(owner).showError();
    }

    private void autoCompletionLearnWord(TextField field){
        suggestedUrls.add(field.getText());

        // we dispose the old binding and recreate a new binding
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedUrls);
    }

}
