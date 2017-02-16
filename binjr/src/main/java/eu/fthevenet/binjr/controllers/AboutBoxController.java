package eu.fthevenet.binjr.controllers;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller for the about dialog
 *
 * @author Frederic Thevenet
 */
public class AboutBoxController implements Initializable {
    private static final Logger logger = LogManager.getLogger(AboutBoxController.class);
    public static final String HTTP_WWW_BINJR_EU = "http://www.binjr.eu";
    @FXML
    private DialogPane aboutRoot;

    @FXML
    private Label versionLabel;

    public Hyperlink getBinjrUrl() {
        return binjrUrl;
    }

    @FXML
    private Hyperlink binjrUrl;

    @FXML
    private void handleCloseButtonAction(ActionEvent event){
        ((Stage)aboutRoot.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert aboutRoot != null : "fx:id\"aboutRoot\" was not injected!";
        assert versionLabel != null : "fx:id\"versionLabel\" was not injected!";
        assert binjrUrl != null : "fx:id\"binjrUrl\" was not injected!";

        versionLabel.setText("version " + GlobalPreferences.getInstance().getManifestVersion());
    }

    public void goTobinjrDotEu(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(HTTP_WWW_BINJR_EU));
                } catch (IOException | URISyntaxException e) {
                    logger.error(e);
                }
            }
        }
        binjrUrl.setVisited(false);
    }
}
