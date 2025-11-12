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

public class ValueAxisBarChart<X, Y> extends ScatterChart<X, Y> {
    public ValueAxisBarChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    public ValueAxisBarChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }


    @Override
    protected void dataItemAdded(Series<X, Y> series, int itemIndex, Data<X, Y> item) {
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
        // update symbol positions
        for (int seriesIndex = 0; seriesIndex < dataSize; seriesIndex++) {
            Series<X, Y> series = getData().get(seriesIndex);
            for (Iterator<Data<X, Y>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
                Data<X, Y> item = it.next();
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y = getYAxis().getDisplayPosition(item.getYValue());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }
                if (item.getNode() instanceof Rectangle symbol) {
                    symbol.setHeight(getYAxis().getZeroPosition() - y);
                    final double w = symbol.getWidth();
                    final double h = symbol.getHeight();
                    symbol.resizeRelocate(x - (w / 2), y, w, h);
                }
            }
        }
    }
}

