/*
 *    Copyright 2017-2025 Frederic Thevenet
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
import eu.binjr.common.javafx.controls.LabelWithInlineHelp;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.control.ToggleSwitch;

import java.io.Closeable;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The controller class for the chart properties view.
 *
 * @author Frederic Thevenet
 */
public class ChartPropertiesController implements Initializable, Closeable {
    private static final Logger logger = Logger.create(ChartPropertiesController.class);
    private final Chart chart;
    private final XYChartsWorksheet worksheet;
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final BindingManager bindingManager = new BindingManager();
    @FXML
    private AnchorPane root;
    @FXML
    private Button closeButton;
    @FXML
    private Slider graphOpacitySlider;
    @FXML
    private LabelWithInlineHelp graphOpacityLabel;
    @FXML
    private Slider strokeWidthSlider;
    @FXML
    private Label strokeWidthText;
    @FXML
    private LabelWithInlineHelp strokeWidthLabel;
    @FXML
    private LabelWithInlineHelp showAreaOutlineLabel;
    @FXML
    private Label opacityText;
    @FXML
    private ToggleSwitch showAreaOutline;
    @FXML
    private ChoiceBox<ChartType> chartTypeChoice;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private ToggleSwitch autoScaleYAxis;
    @FXML
    private ToggleSwitch alwaysIncludeOriginInAutoScale;
    @FXML
    private LabelWithInlineHelp keepZeroLabel;
    @FXML
    private HBox yAxisScaleSettings;
    @FXML
    private Slider minChartHeightSlider;
    @FXML
    private Label minChartHeightText;
    @FXML
    private ChoiceBox<TimelineDisplayMode> timelineModeChoiceBox;
    @FXML
    private LabelWithInlineHelp durationUnitLabel;
    @FXML
    private ChoiceBox<ChronoUnit> durationUnitChoiceBox;

    public ChartPropertiesController(XYChartsWorksheet worksheet, Chart chart) {
        this.worksheet = worksheet;
        this.chart = chart;
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
        bindingManager.bind(minChartHeightSlider.minProperty(), UserPreferences.getInstance().lowerStackedChartHeight.property());
        bindingManager.bind(minChartHeightSlider.maxProperty(), UserPreferences.getInstance().upperChartHeight.property());
        bindingManager.bindBidirectional(minChartHeightSlider.valueProperty(), worksheet.minChartHeightProperty());
        bindingManager.bind(minChartHeightText.textProperty(), Bindings.format("%.0f", minChartHeightSlider.valueProperty()));
        bindingManager.bindBidirectional(graphOpacitySlider.valueProperty(), chart.graphOpacityProperty());
        bindingManager.bind(opacityText.textProperty(), Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));
        bindingManager.bindBidirectional(strokeWidthSlider.valueProperty(), chart.strokeWidthProperty());
        bindingManager.bind(strokeWidthText.textProperty(), Bindings.format("%.1f", strokeWidthSlider.valueProperty()));
        adaptToChartType(chart.getChartType());
        bindingManager.attachListener(chart.chartTypeProperty(), (ChangeListener<ChartType>) (observable, oldValue, newValue) -> {
            adaptToChartType(chart.getChartType());
        });
        bindingManager.bindBidirectional(showAreaOutline.selectedProperty(), chart.showAreaOutlineProperty());
        bindingManager.bindBidirectional(autoScaleYAxis.selectedProperty(), chart.autoScaleYAxisProperty());
        bindingManager.bindBidirectional(alwaysIncludeOriginInAutoScale.selectedProperty(), chart.alwaysIncludeOriginInAutoScaleProperty());
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
        bindingManager.attachListener(yMaxRange.focusedProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            if (newValue) {
                chart.armSaveHistory();
            }
        });
        bindingManager.attachListener(yMinRange.focusedProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            if (newValue) {
                chart.armSaveHistory();
            }
        });
        bindingManager.attachListener(autoScaleYAxis.selectedProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            chart.armSaveHistory();
        });

        chartTypeChoice.getItems().setAll(ChartType.definedValues());
        chartTypeChoice.getSelectionModel().select(chart.getChartType());
        bindingManager.bind(chart.chartTypeProperty(), chartTypeChoice.getSelectionModel().selectedItemProperty());
        var strokeWithEditable = Bindings.createBooleanBinding(() ->
                        (chart.chartTypeProperty().getValue() == ChartType.LINE ||
                                chart.chartTypeProperty().getValue() == ChartType.SCATTER ||
                                chart.getChartType() == ChartType.BAR ||
                                chart.getChartType() == ChartType.EVENT ||
                                chart.showAreaOutlineProperty().getValue()),
                chart.chartTypeProperty(),
                chart.showAreaOutlineProperty());
        strokeWidthSlider.disableProperty().bind(strokeWithEditable.not());
        strokeWidthText.disableProperty().bind(strokeWithEditable.not());
        strokeWidthLabel.disableProperty().bind(strokeWithEditable.not());
        bindingManager.bindBidirectional(root.visibleProperty(), chart.showPropertiesProperty());
        closeButton.setOnAction(e -> root.visibleProperty().setValue(false));
        bindingManager.bind(yAxisScaleSettings.disableProperty(), autoScaleYAxis.selectedProperty());
        bindingManager.bind(alwaysIncludeOriginInAutoScale.disableProperty(), autoScaleYAxis.selectedProperty().not());

        bindingManager.bindBidirectional(timelineModeChoiceBox.valueProperty(), chart.timelineDisplayModeProperty());
        this.timelineModeChoiceBox.getItems().setAll(TimelineDisplayMode.values());
        this.timelineModeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TimelineDisplayMode o) {
                return o != null ? o.toString() : "";
            }

            @Override
            public TimelineDisplayMode fromString(String s) {
                return timelineModeChoiceBox.getValue();
            }
        });
        bindingManager.bindBidirectional(durationUnitChoiceBox.valueProperty(), chart.durationUnitProperty());
        this.durationUnitChoiceBox.getItems().setAll(Arrays.stream(ChronoUnit.values()).filter(c -> c.compareTo(ChronoUnit.HOURS) <= 0).toList());
        this.durationUnitChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ChronoUnit o) {
                return o != null ? o.toString() : "";
            }

            @Override
            public ChronoUnit fromString(String s) {
                return durationUnitChoiceBox.getValue();
            }
        });
    }

    private void adaptToChartType(ChartType chartType) {
        var enable = !(chartType == ChartType.LINE ||
                chartType == ChartType.SCATTER ||
                chartType == ChartType.EVENT ||
                chartType == ChartType.BAR);
        showAreaOutline.setManaged(enable);
        showAreaOutlineLabel.setManaged(enable);
        graphOpacitySlider.setManaged(enable);
        graphOpacityLabel.setManaged(enable);
        opacityText.setManaged(enable);
        showAreaOutline.setVisible(enable);
        showAreaOutlineLabel.setVisible(enable);
        graphOpacitySlider.setVisible(enable);
        graphOpacityLabel.setVisible(enable);
        opacityText.setVisible(enable);

        var isDuration = chartType == ChartType.DURATION;
        durationUnitChoiceBox.setManaged(isDuration);
        durationUnitLabel.setManaged(isDuration);
        durationUnitChoiceBox.setVisible(isDuration);
        durationUnitLabel.setVisible(isDuration);
    }

    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            logger.debug(() -> "Closing ChartPropertiesController " + this);
            bindingManager.close();
            closeButton.setOnAction(null);
        }
    }
}
