package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;

import java.awt.*;
import java.net.URL;
import java.util.Observable;
import java.util.ResourceBundle;

/**
 * Created by FTT2 on 16/01/2017.
 */
public class PreferenceDialogController implements Initializable {
    @FXML
    private PropertySheet preferencePropertySheet;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert preferencePropertySheet != null : "fx:id\"preferencePropertySheet\" was not injected!";

        ObservableList<PropertySheet.Item> props = BeanPropertyUtils.getProperties(GlobalPreferences.getInstance());
      //  ObservableList<PropertySheet.Item> props = BeanPropertyUtils.getProperties(new Button("hello"));
        preferencePropertySheet.getItems().setAll(props);
        preferencePropertySheet.setMode(PropertySheet.Mode.NAME);

    }
}
