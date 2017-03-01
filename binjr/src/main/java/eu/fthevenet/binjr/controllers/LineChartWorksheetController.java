package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.charts.ZonedDateTimeAxis;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;

/**
 * An implementation of {@link WorksheetController} that provides a {@link LineChart} chart.
 *
 * @author Frederic Thevenet
 */
public class LineChartWorksheetController extends WorksheetController {

    /**
     * Initializes a new instance of the {@link LineChartWorksheetController} class
     *
     * @param mainViewController A reference to the {@link MainViewController} instance.
     * @param worksheet          the {@link Worksheet} instance associated to the controller
     */
    public LineChartWorksheetController(MainViewController mainViewController, Worksheet worksheet) {
        super(mainViewController, worksheet);
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis) {
        LineChart<ZonedDateTime, Double> newChart = new LineChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        return newChart;
    }
}
