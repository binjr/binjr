package eu.fthevenet.binjr.controllers;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Frederic Thevenet
 */
public class ChartPropertiesController implements Initializable {
    public static double settingsPaneDistance = -210;
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty hidden = new SimpleBooleanProperty(true);
    @FXML
    private AnchorPane root;
    @FXML
    private Button closeButton;


    private void show() {
        if (hidden.getValue()) {
            slidePanel(-1, Duration.millis(0));
            hidden.setValue(false);
        }
    }

    private void hide() {
        if (!hidden.getValue()) {
            slidePanel(1, Duration.millis(0));
            hidden.setValue(true);
        }
    }

    private void slidePanel(int show, Duration delay) {
        Node n = root.getParent();
        TranslateTransition openNav = new TranslateTransition(new Duration(200), n);
        openNav.setDelay(delay);
        openNav.setToX(show * -settingsPaneDistance);
        openNav.play();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                show();
            }
            else {
                hide();
            }
        });
        closeButton.setOnAction(e -> visibleProperty().setValue(false));


//        root.hoverProperty().addListener((observable, oldValue, newValue) -> {
//            if (!newValue) {
//                hide(Duration.millis(800));
//            }
//        });
    }

    public ReadOnlyBooleanProperty hiddenProperty() {
        return hidden;
    }

    public boolean isHidden() {
        return hidden.getValue();
    }


    public boolean isVisible() {
        return visible.get();
    }

    public BooleanProperty visibleProperty() {
        return visible;
    }
}
