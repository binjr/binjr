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

import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import javafx.animation.TranslateTransition;
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
    private final Worksheet<T> worksheet;
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

    public ChartPropertiesController(Worksheet<T> worksheet) {
        this.worksheet = worksheet;
        //     this.parentController = parentController;
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

        NumberStringConverter numberFormatter = new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT));
        graphOpacitySlider.valueProperty().bindBidirectional(worksheet.graphOpacityProperty());
        opacityText.textProperty().bind(Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));

        strokeWidthSlider.valueProperty().bindBidirectional(worksheet.strokeWidthProperty());
        strokeWidthText.textProperty().bind(Bindings.format("%.1f", strokeWidthSlider.valueProperty()));

        adaptToChartType(worksheet.getChartType() == ChartType.LINE);
        worksheet.chartTypeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                adaptToChartType(newValue == ChartType.LINE);
            }
        });
        showAreaOutline.selectedProperty().bindBidirectional(worksheet.showAreaOutlineProperty());

        autoScaleYAxis.selectedProperty().bindBidirectional(worksheet.autoScaleYAxisProperty());


        setAndBindTextFormatter(yMinRange, numberFormatter, worksheet.yAxisMinValueProperty());
        setAndBindTextFormatter(yMaxRange, numberFormatter, worksheet.yAxisMaxValueProperty());
//        setAndBindTextFormatter(propertiesController.yMaxRange, numberFormatter, currentState.endY, ((ValueAxis<Double>) chart.getYAxis()).upperBoundProperty());


        chartTypeChoice.getItems().setAll(ChartType.values());
        chartTypeChoice.getSelectionModel().select(worksheet.getChartType());
        worksheet.chartTypeProperty().bind(chartTypeChoice.getSelectionModel().selectedItemProperty());
        strokeWidthControlDisabled(!showAreaOutline.isSelected());
        showAreaOutline.selectedProperty().addListener((observable, oldValue, newValue) -> strokeWidthControlDisabled(!newValue));

        TextFormatter<ZoneId> formatter = new TextFormatter<ZoneId>(new StringConverter<ZoneId>() {
            @Override
            public String toString(ZoneId object) {
                return object.toString();
            }

            @Override
            public ZoneId fromString(String string) {
                return ZoneId.of(string);
            }
        });
        formatter.valueProperty().bindBidirectional(worksheet.timeZoneProperty());
        timezoneField.setTextFormatter(formatter);

        TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());

        visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                show();
            }
            else {
                hide();
            }
        });
        closeButton.setOnAction(e -> visibleProperty().setValue(false));

        yAxisScaleSettings.disableProperty().bind(autoScaleYAxis.selectedProperty());

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
