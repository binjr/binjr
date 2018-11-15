/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.util.text.BinaryPrefixFormatter;
import eu.fthevenet.util.text.MetricPrefixFormatter;
import eu.fthevenet.util.text.PrefixFormatter;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;

import java.time.ZonedDateTime;

public class ChartViewPort<T extends Number> {
    private Chart<T> dataStore;
    private XYChart<ZonedDateTime, T> chart;
    private ChartPropertiesController<T> propertiesController;
    private PrefixFormatter prefixFormatter;
    private TableView<TimeSeriesInfo<Double>> seriesTable;

    public ChartViewPort(Chart<T> dataStore, XYChart<ZonedDateTime, T> chart, ChartPropertiesController<T> propertiesController) {
        this.dataStore = dataStore;
        this.chart = chart;
        this.seriesTable = new TableView<>();
        this.seriesTable.getStyleClass().add("skinnable-pane-border");
        this.seriesTable.setEditable(true);
        this.propertiesController = propertiesController;
        switch (dataStore.getUnitPrefixes()) {
            case BINARY:
                this.prefixFormatter = new BinaryPrefixFormatter();
                break;
            case METRIC:
                this.prefixFormatter = new MetricPrefixFormatter();
                break;

            default:
                throw new IllegalArgumentException("Unknown unit prefix");
        }
    }

    public ChartPropertiesController<T> getPropertiesController() {
        return propertiesController;
    }

    public XYChart<ZonedDateTime, T> getChart() {
        return chart;
    }

    public Chart<T> getDataStore() {
        return dataStore;
    }

    public PrefixFormatter getPrefixFormatter() {
        return prefixFormatter;
    }

    public TableView<TimeSeriesInfo<Double>> getSeriesTable() {
        return seriesTable;
    }

}
