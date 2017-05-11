package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.ui.charts.ZonedDateTimeAxis;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * An implementation of {@link WorksheetController} that provides a {@link AreaChart} chart.
 *
 * @author Frederic Thevenet
 */
public class AreaChartWorksheetController extends WorksheetController {

    /**
     * Initializes a new instance of the {@link AreaChartWorksheetController} class
     *
     * @param worksheet          the {@link Worksheet} instance associated to the controller
     */
    public AreaChartWorksheetController(Worksheet worksheet) throws IOException {
        super(worksheet);
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis) {
        AreaChart<ZonedDateTime, Double> newChart = new AreaChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        return newChart;
    }

}
