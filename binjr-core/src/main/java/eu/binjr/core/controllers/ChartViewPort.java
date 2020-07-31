/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.javafx.charts.XYChartCrosshair;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.common.text.MetricPrefixFormatter;
import eu.binjr.common.text.PrefixFormatter;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Defines the viewport to hosts an XYChart in worksheet.
 *
 * @author Frederic Thevenet
 */
public class ChartViewPort implements Closeable {
    private final Chart dataStore;
    private final XYChart<ZonedDateTime, Double> chart;
    private ChartPropertiesController propertiesController;
    private final PrefixFormatter prefixFormatter;
    private final TableView<TimeSeriesInfo<Double>> seriesTable;
    private static final Logger logger = Logger.create(ChartViewPort.class);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private XYChartCrosshair<ZonedDateTime, Double> crosshair;

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
    public TableView<TimeSeriesInfo<Double>> getSeriesTable() {
        return seriesTable;
    }

    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            logger.debug(() -> "Closing ChartViewPort " + this.toString());
            propertiesController.close();
            propertiesController = null;
            seriesTable.getColumns().forEach(c -> {
                c.setCellValueFactory(null);
                c.setCellFactory(null);
            });
            seriesTable.setRowFactory(null);
            seriesTable.getColumns().clear();
            seriesTable.setItems(null);
            if (crosshair != null) {
                crosshair.dispose();
            }
        }
    }

    public XYChartCrosshair<ZonedDateTime, Double> getCrosshair() {
        return crosshair;
    }

    public void setCrosshair(XYChartCrosshair<ZonedDateTime, Double> crosshair) {
        this.crosshair = crosshair;
    }
}
