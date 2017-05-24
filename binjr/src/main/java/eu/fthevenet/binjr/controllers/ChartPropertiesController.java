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

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.workspace.Worksheet;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller class for the chart properties view.
 *
 * @author Frederic Thevenet
 */
public class ChartPropertiesController<T extends Number> implements Initializable {
    public static double settingsPaneDistance = -210;
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty hidden = new SimpleBooleanProperty(true);
    private final Worksheet<T> worksheet;
    @FXML
    private AnchorPane root;
    @FXML
    private Button closeButton;
    @FXML
    private Slider graphOpacitySlider = new Slider();
    @FXML
    private Label opacityText = new Label();
    @FXML
    private ToggleSwitch showAreaOutline = new ToggleSwitch();
    @FXML
    private ToggleSwitch showChartSymbols = new ToggleSwitch();

    public ChartPropertiesController(Worksheet<T> worksheet) {
        this.worksheet = worksheet;
    }

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
        assert opacityText != null : "fx:id\"opacityText\" was not injected!";
        assert showAreaOutline != null : "fx:id\"showAreaOutline\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert graphOpacitySlider != null : "fx:id\"graphOpacitySlider\" was not injected!";

        graphOpacitySlider.valueProperty().bindBidirectional(worksheet.graphOpacityProperty());
        opacityText.textProperty().bind(Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));
        showAreaOutline.selectedProperty().bindBidirectional(worksheet.showAreaOutlineProperty());
        showChartSymbols.selectedProperty().bindBidirectional(worksheet.showChartSymbolsProperty());

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
