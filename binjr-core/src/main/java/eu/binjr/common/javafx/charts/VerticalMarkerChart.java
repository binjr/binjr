/*
 * Copyright 2025 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.javafx.charts;

import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.shape.Rectangle;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

public class VerticalMarkerChart extends ValueAxisBarChart<ZonedDateTime> {
    private final boolean hasDuration;

    public VerticalMarkerChart(Axis<ZonedDateTime> xAxis, Axis<Double> yAxis, boolean hasDuration) {
        super(xAxis, yAxis);
        this.hasDuration = hasDuration;
    }

    public VerticalMarkerChart(Axis<ZonedDateTime> xAxis,
                               Axis<Double> yAxis,
                               boolean hasDuration,
                               ObservableList<Series<ZonedDateTime, Double>> data) {
        super(xAxis, yAxis, data);
        this.hasDuration = hasDuration;
    }

    @Override
    protected void layoutPlotChildren() {
        var dataSize = (getData() != null) ? getData().size() : 0;
        for (int seriesIndex = 0; seriesIndex < dataSize; seriesIndex++) {
            Series<ZonedDateTime, Double> series = getData().get(seriesIndex);
            for (Iterator<Data<ZonedDateTime, Double>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
                Data<ZonedDateTime, Double> item = it.next();
                // Re-enable symbol that were previously hidden because out of bound
                item.getNode().setVisible(true);
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y = getYAxis().getValueForDisplay(item.getYValue());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    // Hide because out of bound symbol to avoid visual artefacts
                    item.getNode().setVisible(false);
                    continue;
                }
                if (item.getNode() instanceof Rectangle symbol) {
                    symbol.setHeight(getYAxis().getHeight());
                    var markerWidth = hasDuration ? (getXAxis().getDisplayPosition(
                            item.getXValue().plus(Math.round(item.getYValue() * 1_000), ChronoUnit.MILLIS)) - x) : 0.1;
                    symbol.setWidth(markerWidth);
                    final double w = symbol.getWidth();
                    final double h = symbol.getHeight();
                    symbol.resizeRelocate(x, 0, w, h);
                }
            }
        }
    }
}

