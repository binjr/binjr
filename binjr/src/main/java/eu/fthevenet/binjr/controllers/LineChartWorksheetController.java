package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.util.ui.charts.ZonedDateTimeAxis;
import javafx.beans.property.BooleanProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;

import java.io.IOException;
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
     * @param worksheet          the {@link Worksheet} instance associated to the controller
     */
    public LineChartWorksheetController(Worksheet worksheet) throws IOException {
        super(worksheet);
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis, BooleanProperty showSymbolsProperty) {
        LineChart<ZonedDateTime, Double> newChart = new LineChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(showSymbolsProperty);
        return newChart;
    }
}
