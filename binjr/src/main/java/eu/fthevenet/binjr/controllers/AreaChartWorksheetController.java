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
import eu.fthevenet.util.javafx.charts.ZonedDateTimeAxis;
import javafx.beans.property.BooleanProperty;
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
     * @param worksheet the {@link Worksheet} instance associated to the controller
     */
    public AreaChartWorksheetController(Worksheet worksheet) throws IOException {
        super(worksheet);
    }

    @Override
    public ChartType getChartType() {
        return ChartType.AREA;
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis, BooleanProperty showSymbolsProperty) {
        AreaChart<ZonedDateTime, Double> newChart = new AreaChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        newChart.createSymbolsProperty().bindBidirectional(showSymbolsProperty);
        return newChart;
    }

}
