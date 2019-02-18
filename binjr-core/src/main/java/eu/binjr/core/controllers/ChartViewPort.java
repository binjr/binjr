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

package eu.binjr.core.controllers;

import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.common.text.MetricPrefixFormatter;
import eu.binjr.common.text.PrefixFormatter;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;

import java.time.ZonedDateTime;

/**
 * Defines the viewport to hosts an XYChart in worksheet.
 *
 * @author Frederic Thevenet
 */
public class ChartViewPort {
    private Chart dataStore;
    private XYChart<ZonedDateTime, Double> chart;
    private ChartPropertiesController propertiesController;
    private PrefixFormatter prefixFormatter;
    private TableView<TimeSeriesInfo> seriesTable;

    /**
     * Initializes a new instance of the {@link ChartViewPort} class.
     *
     * @param dataStore            the data store for the viewport.
     * @param chart                the chart control.
     * @param propertiesController the chart properties controller.
     */
    public ChartViewPort(Chart dataStore, XYChart<ZonedDateTime, Double> chart, ChartPropertiesController propertiesController) {
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

    /**
     * Returns the chart properties controller.
     *
     * @return the chart properties controller.
     */
    public ChartPropertiesController getPropertiesController() {
        return propertiesController;
    }

    /**
     * Returns the {@link XYChart} control.
     *
     * @return the {@link XYChart} control.
     */
    public XYChart<ZonedDateTime, Double> getChart() {
        return chart;
    }

    /**
     * Returns the chart's data store.
     *
     * @return the chart's data store.
     */
    public Chart getDataStore() {
        return dataStore;
    }

    /**
     * Returns the unit prefix formatter.
     *
     * @return the unit prefix formatter.
     */
    public PrefixFormatter getPrefixFormatter() {
        return prefixFormatter;
    }

    /**
     * Returns the table view control holding the series info.
     *
     * @return the table view control holding the series info.
     */
    public TableView<TimeSeriesInfo> getSeriesTable() {
        return seriesTable;
    }

}
