/*
 *    Copyright 2019-2020 Frederic Thevenet
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

package eu.binjr.common.javafx.charts;

import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;

import java.util.*;

/**
 * A {@link StackedAreaChart} that support NaN values in series.
 *
 * @param <X> Type for the X axis
 * @param <Y> Type for the Y axis
 */
public class NaNStackedAreaChart<X, Y> extends StackedAreaChart<X, Y> {


    public NaNStackedAreaChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    public NaNStackedAreaChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateAxisRange() {
        // This override is necessary to update axis range based on cumulative Y value for the
        // Y axis instead of the normal way where max value in the data range is used.
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        if (xa.isAutoRanging()) {
            List<X> xData = new ArrayList<>();
            for (XYChart.Series<X, Y> series : getData()) {
                for (XYChart.Data<X, Y> data : series.getData()) {
                    xData.add(data.getXValue());
                }
            }
            xa.invalidateRange(xData);
        }
        if (ya.isAutoRanging()) {
            double totalMinY = Double.MAX_VALUE;
            Iterator<XYChart.Series<X, Y>> seriesIterator = getDisplayedSeriesIterator();
            boolean first = true;
            NavigableMap<Double, Double> accum = new TreeMap<>();
            NavigableMap<Double, Double> prevAccum = new TreeMap<>();
            NavigableMap<Double, Double> currentValues = new TreeMap<>();
            while (seriesIterator.hasNext()) {
                currentValues.clear();
                XYChart.Series<X, Y> series = seriesIterator.next();
                for (XYChart.Data<X, Y> item : series.getData()) {
                    if (item != null) {
                        final double xv = xa.toNumericValue(item.getXValue());
                        final double yv = Double.isNaN(ya.toNumericValue(item.getYValue())) ? 0.0 : ya.toNumericValue(item.getYValue());
                        currentValues.put(xv, yv);
                        if (first) {
                            // On the first pass, just fill the map
                            accum.put(xv, yv);
                            // minimum is applicable only in the first series
                            totalMinY = Math.min(totalMinY, yv);
                        } else {
                            if (prevAccum.containsKey(xv)) {
                                accum.put(xv, prevAccum.get(xv) + yv);
                            } else {
                                // If the point wasn't yet in the previous (accumulated) series
                                Map.Entry<Double, Double> he = prevAccum.higherEntry(xv);
                                Map.Entry<Double, Double> le = prevAccum.lowerEntry(xv);
                                if (he != null && le != null) {
                                    // If there's both point above and below this point, interpolate
                                    accum.put(xv, ((xv - le.getKey()) / (he.getKey() - le.getKey())) *
                                            (le.getValue() + he.getValue()) + yv);
                                } else if (he != null) {
                                    // The point is before the first point in the previously accumulated series
                                    accum.put(xv, he.getValue() + yv);
                                } else if (le != null) {
                                    // The point is after the last point in the previously accumulated series
                                    accum.put(xv, le.getValue() + yv);
                                } else {
                                    // The previously accumulated series is empty
                                    accum.put(xv, yv);
                                }
                            }
                        }
                    }
                }
                // Now update all the keys that were in the previous series, but not in the new one
                for (Map.Entry<Double, Double> e : prevAccum.entrySet()) {
                    if (accum.keySet().contains(e.getKey())) {
                        continue;
                    }
                    Double k = e.getKey();
                    final Double v = e.getValue();
                    // Look at the values of the current series
                    Map.Entry<Double, Double> he = currentValues.higherEntry(k);
                    Map.Entry<Double, Double> le = currentValues.lowerEntry(k);
                    if (he != null && le != null) {
                        // Interpolate the for the point from current series and add the accumulated value
                        accum.put(k, ((k - le.getKey()) / (he.getKey() - le.getKey())) *
                                (le.getValue() + he.getValue()) + v);
                    } else if (he != null) {
                        // There accumulated value is before the first value in the current series
                        accum.put(k, he.getValue() + v);
                    } else if (le != null) {
                        // There accumulated value is after the last value in the current series
                        accum.put(k, le.getValue() + v);
                    } else {
                        // The current series are empty
                        accum.put(k, v);
                    }

                }

                prevAccum.clear();
                prevAccum.putAll(accum);
                accum.clear();
                first = (totalMinY == Double.MAX_VALUE); // If there was already some value in the series, we can consider as
                // being past the first series

            }
            if (totalMinY != Double.MAX_VALUE) ya.invalidateRange(Arrays.asList(ya.toRealValue(totalMinY),
                    ya.toRealValue(Collections.max(prevAccum.values()))));

        }
    }
}
