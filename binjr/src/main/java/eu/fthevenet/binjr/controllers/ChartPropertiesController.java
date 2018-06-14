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

import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.binjr.data.workspace.ChartType;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.TextFields;

import java.net.URL;
import java.time.ZoneId;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The controller class for the chart properties view.
 *
 * @author Frederic Thevenet
 */
public class ChartPropertiesController<T extends Number> implements Initializable {
    public static final double SETTINGS_PANE_DISTANCE = -210;
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty hidden = new SimpleBooleanProperty(true);
    private final Chart<T> chart;
    @FXML
    private AnchorPane root;
    @FXML
    private Button closeButton;
    @FXML
    private Slider graphOpacitySlider = new Slider();
    @FXML
    private Label graphOpacityLabel = new Label();
    @FXML
    private Slider strokeWidthSlider = new Slider();
    @FXML
    private Label strokeWidthText = new Label();
    @FXML
    private Label strokeWidthLabel = new Label();
    @FXML
    private Label opacityText = new Label();
    @FXML
    private ToggleSwitch showAreaOutline = new ToggleSwitch();
    @FXML
    private ChoiceBox<ChartType> chartTypeChoice;
    //    @FXML
//    private ChoiceBox<UnitPrefixes> unitPrefixChoiceBox;
    @FXML
    private TextField timezoneField;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private ToggleSwitch autoScaleYAxis;
    @FXML
    private HBox yAxisScaleSettings;
//    @FXML
//    private TextField chartNameTextField;
//    @FXML
//    private TextField chartUnitTextField;


    public ChartPropertiesController(Chart<T> chart) {
        this.chart = chart;

    }

    public void show() {
        if (hidden.getValue()) {
            slidePanel(-1, Duration.millis(0));
            hidden.setValue(false);
        }
    }

    public void hide() {
        if (!hidden.getValue()) {
            slidePanel(1, Duration.millis(0));
            hidden.setValue(true);
        }
    }

    private void slidePanel(int show, Duration delay) {
        Node n = root.getParent();
        if (n != null) {
            n.toFront();
        }
        TranslateTransition openNav = new TranslateTransition(new Duration(200), n);
        openNav.setDelay(delay);
        openNav.setToX(show * -SETTINGS_PANE_DISTANCE);
        openNav.play();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert opacityText != null : "fx:id\"opacityText\" was not injected!";
        assert showAreaOutline != null : "fx:id\"showAreaOutline\" was not injected!";
        assert graphOpacitySlider != null : "fx:id\"graphOpacitySlider\" was not injected!";
        assert strokeWidthText != null : "fx:id\"strokeWidthText\" was not injected!";
        assert strokeWidthSlider != null : "fx:id\"strokeWidthSlider\" was not injected!";
        assert autoScaleYAxis != null : "fx:id\"autoScaleYAxis\" was not injected!";
        assert yAxisScaleSettings != null : "fx:id\"yAxisScaleSettings\" was not injected!";

        NumberStringConverter numberFormatter = new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT));
        graphOpacitySlider.valueProperty().bindBidirectional(chart.graphOpacityProperty());
        opacityText.textProperty().bind(Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));

        strokeWidthSlider.valueProperty().bindBidirectional(chart.strokeWidthProperty());
        strokeWidthText.textProperty().bind(Bindings.format("%.1f", strokeWidthSlider.valueProperty()));

        adaptToChartType(chart.getChartType() == ChartType.LINE);
        chart.chartTypeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                adaptToChartType(newValue == ChartType.LINE);
            }
        });
        showAreaOutline.selectedProperty().bindBidirectional(chart.showAreaOutlineProperty());
        autoScaleYAxis.selectedProperty().bindBidirectional(chart.autoScaleYAxisProperty());
//
//        chartNameTextField.textProperty().bindBidirectional(chart.nameProperty());
//        chartUnitTextField.textProperty().bindBidirectional(chart.unitProperty());

        setAndBindTextFormatter(yMinRange, numberFormatter, chart.yAxisMinValueProperty());
        setAndBindTextFormatter(yMaxRange, numberFormatter, chart.yAxisMaxValueProperty());

//        unitPrefixChoiceBox.getItems().setAll(UnitPrefixes.values());
//        unitPrefixChoiceBox.getSelectionModel().select(chart.getUnitPrefixes());
//        chart.unitPrefixesProperty().bind(unitPrefixChoiceBox.getSelectionModel().selectedItemProperty());

        chartTypeChoice.getItems().setAll(ChartType.values());
        chartTypeChoice.getSelectionModel().select(chart.getChartType());
        chart.chartTypeProperty().bind(chartTypeChoice.getSelectionModel().selectedItemProperty());

        //        TextFormatter<ZoneId> formatter = new TextFormatter<ZoneId>(new StringConverter<ZoneId>() {
        //            @Override
        //            public String toString(ZoneId object) {
        //                return object.toString();
        //            }
        //
        //            @Override
        //            public ZoneId fromString(String string) {
        //                return ZoneId.of(string);
        //            }
        //        });

        //formatter.valueProperty().bindBidirectional(chart.timeZoneProperty());

        //  timezoneField.setTextFormatter(formatter);

        strokeWidthControlDisabled(!showAreaOutline.isSelected());
        showAreaOutline.selectedProperty().addListener((observable, oldValue, newValue) -> strokeWidthControlDisabled(!newValue));
        visibleProperty().addListener((observable, oldValue, newValue) -> setPanelVisibility(newValue));
        this.visibleProperty().bindBidirectional(chart.showPropertiesProperty());
        TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());
        closeButton.setOnAction(e -> visibleProperty().setValue(false));
        yAxisScaleSettings.disableProperty().bind(autoScaleYAxis.selectedProperty());
        Platform.runLater(() -> setPanelVisibility(chart.isShowProperties()));
    }

    private void setPanelVisibility(boolean isVisible) {
        if (isVisible) {
            show();
        }
        else {
            hide();
        }
    }

    private <T extends Number> void setAndBindTextFormatter(TextField textField, StringConverter<T> converter, Property<T> stateProperty) {
        final TextFormatter<T> formatter = new TextFormatter<>(converter);
        formatter.valueProperty().bindBidirectional(stateProperty);
        textField.setTextFormatter(formatter);
    }

    private void adaptToChartType(boolean disable) {
        showAreaOutline.setDisable(disable);
        showAreaOutline.setSelected(disable);
        graphOpacitySlider.setDisable(disable);
        graphOpacityLabel.setDisable(disable);
        opacityText.setDisable(disable);
    }

    private void strokeWidthControlDisabled(boolean disable) {
        strokeWidthSlider.setDisable(disable);
        strokeWidthText.setDisable(disable);
        strokeWidthLabel.setDisable(disable);
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
