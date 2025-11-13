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

import java.util.Iterator;

public class ValueAxisBarChart<X> extends ScatterChart<X, Double> {

    public ValueAxisBarChart(Axis<X> xAxis, Axis<Double> yAxis) {
        super(xAxis, yAxis);
    }

    public ValueAxisBarChart(Axis<X> xAxis, Axis<Double> yAxis, ObservableList<Series<X, Double>> data) {
        super(xAxis, yAxis, data);
    }

    @Override
    protected void dataItemAdded(Series<X, Double> series, int itemIndex, Data<X, Double> item) {
        Node symbol = item.getNode();
        if (symbol == null) {
            symbol = new Rectangle(1, 1);
            symbol.setAccessibleRole(AccessibleRole.TEXT);
            symbol.setAccessibleRoleDescription("Bar");
            item.setNode(symbol);
        }
        super.dataItemAdded(series, itemIndex, item);
    }


    @Override
    protected void layoutPlotChildren() {
        var dataSize = (getData() != null) ? getData().size() : 0;
        for (int seriesIndex = 0; seriesIndex < dataSize; seriesIndex++) {
            Series<X, Double> series = getData().get(seriesIndex);
            for (Iterator<Data<X, Double>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
                Data<X, Double> item = it.next();
                // Re-enable symbol that were previously hidden because out of bound
                item.getNode().setVisible(true);
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y;
                double bottom = getYAxis().getValueForDisplay(getYAxis().getHeight());
                double top = getYAxis().getValueForDisplay(0);
                if (bottom > item.getYValue()) {
                    y = getYAxis().getHeight();
                } else if (top < item.getYValue()) {
                    y = 0;
                } else {
                    y = getYAxis().getDisplayPosition(item.getYValue());
                }
                double height;
                if (bottom > 0) {
                    height = getYAxis().getHeight() - y;
                } else if (top < 0) {
                    height = 0 - y;
                } else {
                    height = getYAxis().getZeroPosition() - y;
                }
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    // Hide because out of bound symbol to avoid visual artefacts
                    item.getNode().setVisible(false);
                    continue;
                }
                if (height < 0) {
                    height = Math.abs(height);
                    y = y - height;
                }
                if (item.getNode() instanceof Rectangle symbol) {
                    symbol.setHeight(height);
                    final double width = symbol.getWidth();
                    symbol.resizeRelocate(x - (width / 2), y, width, height);
                }
            }
        }
    }
}

