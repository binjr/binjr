/*
 *    Copyright 2017-2019 Frederic Thevenet
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
 */

package eu.binjr.core.controllers;

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.Worksheet;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;

import java.io.Closeable;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The controller class for the chart properties view.
 *
 * @author Frederic Thevenet
 */
public class ChartPropertiesController implements Initializable, Closeable {
    private static final Logger logger = LogManager.getLogger(ChartPropertiesController.class);
    public static final double SETTINGS_PANE_DISTANCE = -210;
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty hidden = new SimpleBooleanProperty(true);
    private final Chart chart;
    private final Worksheet worksheet;
    private final AtomicBoolean closing = new AtomicBoolean(false);
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
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private ToggleSwitch autoScaleYAxis;
    @FXML
    private HBox yAxisScaleSettings;

    private final BindingManager bindingManager = new BindingManager();


    public ChartPropertiesController(Worksheet worksheet, Chart chart) {
        this.worksheet = worksheet;
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
        root.toFront();
        TranslateTransition openNav = new TranslateTransition(new Duration(200), root);
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

        bindingManager.bindBidirectional(graphOpacitySlider.valueProperty(), chart.graphOpacityProperty());
        bindingManager.bind(opacityText.textProperty(), Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));
        bindingManager.bindBidirectional(strokeWidthSlider.valueProperty(), chart.strokeWidthProperty());
        bindingManager.bind(strokeWidthText.textProperty(), Bindings.format("%.1f", strokeWidthSlider.valueProperty()));
        adaptToChartType(chart.getChartType() == ChartType.LINE || chart.getChartType() == ChartType.SCATTER);
        bindingManager.attachListener(chart.chartTypeProperty(), (ChangeListener<ChartType>) (observable, oldValue, newValue) -> {
            adaptToChartType(newValue == ChartType.LINE || chart.getChartType() == ChartType.SCATTER);
        });
        bindingManager.bindBidirectional(showAreaOutline.selectedProperty(), chart.showAreaOutlineProperty());
        bindingManager.bindBidirectional(autoScaleYAxis.selectedProperty(), chart.autoScaleYAxisProperty());
        NumberStringConverter numberStringConverter = new NumberStringConverter(new DecimalFormat("###,###.####"));
        TextFormatter<Number> yMinFormatter = new TextFormatter<>(numberStringConverter);
        bindingManager.attachListener(yMinFormatter.valueProperty(), (ChangeListener<Number>) (observable, oldValue, newValue) -> {
            if ((chart.getyAxisMaxValue() - newValue.doubleValue() > 0)) {
                chart.yAxisMinValueProperty().setValue(newValue);
            } else {
                yMinFormatter.valueProperty().setValue(oldValue);
            }
        });
        bindingManager.attachListener(chart.yAxisMinValueProperty(), (ChangeListener<Double>) (observable, oldValue, newValue) -> {
            yMinFormatter.valueProperty().setValue(newValue);
        });
        yMinFormatter.valueProperty().setValue(chart.getyAxisMinValue());
        yMinRange.setTextFormatter(yMinFormatter);
        TextFormatter<Number> yMaxFormatter = new TextFormatter<>(numberStringConverter);
        bindingManager.attachListener(yMaxFormatter.valueProperty(), (ChangeListener<Number>) (observable, oldValue, newValue) -> {
            if ((newValue.doubleValue() - chart.getyAxisMinValue() > 0)) {
                chart.yAxisMaxValueProperty().setValue(newValue);
            } else {
                yMaxFormatter.valueProperty().setValue(oldValue);
            }
        });
        bindingManager.attachListener(chart.yAxisMaxValueProperty(), (ChangeListener<Double>) (observable, oldValue, newValue) -> {
            yMaxFormatter.valueProperty().setValue(newValue);
        });
        yMaxFormatter.valueProperty().setValue(chart.getyAxisMaxValue());
        yMaxRange.setTextFormatter(yMaxFormatter);

        chartTypeChoice.getItems().setAll(ChartType.values());
        chartTypeChoice.getSelectionModel().select(chart.getChartType());
        bindingManager.bind(chart.chartTypeProperty(), chartTypeChoice.getSelectionModel().selectedItemProperty());


        strokeWidthControlDisabled(!showAreaOutline.isSelected());
        bindingManager.attachListener(showAreaOutline.selectedProperty(),
                (ChangeListener<Boolean>) (observable, oldValue, newValue) -> strokeWidthControlDisabled(!newValue));
        bindingManager.attachListener(visibleProperty(), (observable, oldValue, newValue) -> setPanelVisibility());
        bindingManager.bindBidirectional(visibleProperty(), chart.showPropertiesProperty());

        closeButton.setOnAction(e -> visibleProperty().setValue(false));
        bindingManager.bind(yAxisScaleSettings.disableProperty(), autoScaleYAxis.selectedProperty());
    }

    void setPanelVisibility() {
        if (isVisible()) {
            show();
        } else {
            hide();
        }
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
        return this.visible;
    }


    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            logger.debug(() -> "Closing ChartPropertiesController " + this.toString());
            bindingManager.close();
            closeButton.setOnAction(null);
        }
    }
}
