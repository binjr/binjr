package eu.fthevenet.binjr.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;


import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by ftt2 on 16/11/2016.
 */
public class AboutBoxController implements Initializable {

    @FXML
    private DialogPane aboutRoot;

    @FXML
    private void handleCloseButtonAction(ActionEvent event){
        ((Stage)aboutRoot.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {


    }
}
