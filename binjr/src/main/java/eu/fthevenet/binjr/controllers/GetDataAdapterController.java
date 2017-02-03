package eu.fthevenet.binjr.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for GetDataAdapterView
 *
 * @author Frederic Thevenet
 */
public class GetDataAdapterController implements Initializable {
    @FXML
    private TextField urlField;
    @FXML
    private TextField timezoneField;

    /**
     * Gets the Url field
     *
     * @return the Url field
     */
    public TextField getUrlField() {
        return urlField;
    }

    /**
     * Gets the timezone field
     *
     * @return the timezone field
     */
    public TextField getTimezoneField() {
        return timezoneField;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert urlField != null : "fx:id\"urlField\" was not injected!";
        assert timezoneField != null : "fx:id\"timezoneField\" was not injected!";
    }
}
