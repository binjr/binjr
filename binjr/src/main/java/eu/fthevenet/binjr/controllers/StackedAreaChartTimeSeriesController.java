package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.charts.StableTicksAxis;
import eu.fthevenet.binjr.charts.ZonedDateTimeAxis;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.geometry.Side;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;

/**
 * An implementation of {@link TimeSeriesController} that provides a {@link StackedAreaChart} chart.
 *
 * @author Frederic Thevenet
 */
public class StackedAreaChartTimeSeriesController extends TimeSeriesController {

    /**
     * Initializes a new instance of the {@link StackedAreaChartTimeSeriesController} class
     * @param mainViewController A reference to the {@link MainViewController} instance.
     */
    public StackedAreaChartTimeSeriesController(MainViewController mainViewController) {
        super(mainViewController);
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis) {
        StackedAreaChart<ZonedDateTime, Double> newChart = new StackedAreaChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        return newChart;
    }


}
