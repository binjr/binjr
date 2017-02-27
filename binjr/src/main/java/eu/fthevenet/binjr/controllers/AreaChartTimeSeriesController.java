package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.charts.ZonedDateTimeAxis;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;

/**
 * An implementation of {@link TimeSeriesController} that provides a {@link AreaChart} chart.
 *
 * @author Frederic Thevenet
 */
public class AreaChartTimeSeriesController extends TimeSeriesController {

    /**
     * Initializes a new instance of the {@link AreaChartTimeSeriesController} class
     *
     * @param mainViewController A reference to the {@link MainViewController} instance
     * @param worksheet          the {@link Worksheet} instance associated to the controller
     */
    public AreaChartTimeSeriesController(MainViewController mainViewController, Worksheet worksheet) {
        super(mainViewController, worksheet);
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis) {
        AreaChart<ZonedDateTime, Double> newChart = new AreaChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        return newChart;
    }

}
