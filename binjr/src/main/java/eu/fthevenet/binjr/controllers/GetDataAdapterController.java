package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import org.controlsfx.control.textfield.TextFields;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by FTT2 on 02/02/2017.
 */
public class GetDataAdapterController implements Initializable {
    @FXML
    private TextField urlField;
    @FXML
    private ChoiceBox<String> timezoneField;

    public TextField getUrlField() {
        return urlField;
    }

    public ChoiceBox<String> getTimezoneField() {
        return timezoneField;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert urlField != null : "fx:id\"urlField\" was not injected!";
        assert timezoneField != null : "fx:id\"timezoneField\" was not injected!";

    }
}
