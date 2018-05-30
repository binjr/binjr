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
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * An implementation of {@link WorksheetController} that provides a {@link StackedAreaChart} chart.
 *
 * @author Frederic Thevenet
 */
public class StackedAreaChartWorksheetController extends WorksheetController {

    /**
     * Initializes a new instance of the {@link StackedAreaChartWorksheetController} class
     *
     * @param worksheet the {@link Worksheet} instance associated to the controller
     */
    public StackedAreaChartWorksheetController(Worksheet worksheet) throws IOException {
        super(worksheet);
    }

    @Override
    public ChartType getChartType() {
        return ChartType.STACKED;
    }

    @Override
    protected XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis) {
        StackedAreaChart<ZonedDateTime, Double> newChart = new StackedAreaChart<>(xAxis, yAxis);
        newChart.setCreateSymbols(false);
        return newChart;
    }
}
