package eu.fthevenet.binjr.viewer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


import javax.imageio.ImageIO;
import java.io.IOException;
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
