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

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.Axis;
import javafx.scene.shape.Rectangle;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

public class VerticalMarkerChart extends ImpulseChart<ZonedDateTime> {
    private final boolean hasDuration;
    private final Property<ChronoUnit> durationUnit = new SimpleObjectProperty<>(ChronoUnit.SECONDS);

    public VerticalMarkerChart(Axis<ZonedDateTime> xAxis, Axis<Double> yAxis, boolean hasDuration) {
        super(xAxis, yAxis);
        this.hasDuration = hasDuration;
        durationUnitProperty().addListener((_, oldVal, newVal) -> {
            if (oldVal != newVal){
                this.layoutPlotChildren();
            }
        } );
    }

    @Override
    protected void layoutPlotChildren() {
        var dataSize = (getData() != null) ? getData().size() : 0;
        var n = Duration.of(1, durationUnit.getValue()).toNanos();
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
                    if (hasDuration) {
                        var d = Duration.of(Math.round(item.getYValue() * n), ChronoUnit.NANOS);
                        symbol.setWidth(getXAxis().getDisplayPosition(item.getXValue().plus(d)) - x);
                    } else {
                        symbol.setWidth(0.1);
                    }
                    symbol.resizeRelocate(x, 0, symbol.getWidth(), symbol.getHeight());
                }
            }
        }
    }

    public ChronoUnit getDurationUnit() {
        return durationUnit.getValue();
    }

    public Property<ChronoUnit> durationUnitProperty() {
        return durationUnit;
    }

    public void setDurationUnit(ChronoUnit durationUnit) {
        this.durationUnit.setValue(durationUnit);
    }

    public boolean hasDuration() {
        return hasDuration;
    }
}

