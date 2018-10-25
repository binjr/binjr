/*
 *    Copyright 2018 Frederic Thevenet
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

import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.util.javafx.charts.XYChartSelection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.chart.ValueAxis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represent the state of the time series view
 */
public class ChartViewportsState implements AutoCloseable {
    private final WorksheetController parent;
    private static final Logger logger = LogManager.getLogger(ChartViewportsState.class);
    private HashMap<Chart<Double>, AxisState> axisStates = new HashMap<>();
    private final SimpleObjectProperty<ZonedDateTime> startX;
    private final SimpleObjectProperty<ZonedDateTime> endX;
    private final ChangeListener<ZonedDateTime> onRefreshAllRequired;
    private ChangeListener<ZonedDateTime> onStartXChanged;
    private ChangeListener<ZonedDateTime> onEndXChanged;


    @Override
    public void close() {
        this.startX.removeListener(onRefreshAllRequired);
        this.endX.removeListener(onRefreshAllRequired);
        axisStates.values().forEach(AxisState::close);
    }

    public class AxisState implements AutoCloseable {
        private final SimpleDoubleProperty startY;
        private final SimpleDoubleProperty endY;
        private final ChartViewPort<Double> chartViewPort;
        private final ChangeListener<Number> onRefreshViewportRequired;


        /**
         * Initializes a new instance of the {@link AxisState} class.
         *
         * @param chartViewPort
         * @param startY        the lower bound of the Y axis
         * @param endY          the upper bound of the Y axis
         */
        public AxisState(ChartViewPort<Double> chartViewPort, double startY, double endY) {
            this.chartViewPort = chartViewPort;
            this.onRefreshViewportRequired = (observable, oldValue, newValue) -> parent.invalidate(chartViewPort, true, false);
            this.startY = new SimpleDoubleProperty(roundYValue(startY));
            this.endY = new SimpleDoubleProperty(roundYValue(endY));
            this.addListeners();
        }


        public void removeListeners() {
            this.startY.removeListener(onRefreshViewportRequired);
            this.endY.removeListener(onRefreshViewportRequired);
        }

        public void addListeners() {
            this.startY.addListener(onRefreshViewportRequired);
            this.endY.addListener(onRefreshViewportRequired);
        }

        /**
         * Returns the current state as a {@link XYChartSelection}
         *
         * @return the current state as a {@link XYChartSelection}
         */
        public XYChartSelection<ZonedDateTime, Double> asSelection() {
            return new XYChartSelection<>(
                    getStartX(),
                    getEndX(),
                    startY.get(),
                    endY.get(),
                    chartViewPort.getDataStore().isAutoScaleYAxis()
            );
        }

        public XYChartSelection<ZonedDateTime, Double> selectTimeRange(ZonedDateTime beginning, ZonedDateTime end) {
            return new XYChartSelection<>(
                    beginning,
                    end,
                    startY.get(),
                    endY.get(),
                    chartViewPort.getDataStore().isAutoScaleYAxis()
            );
        }

        /**
         * Sets the current state from a {@link XYChartSelection}
         *
         * @param selection the {@link XYChartSelection} to set as the current state
         * @param toHistory true if the change in state should be recorded in the history
         */
        public  void setSelection(XYChartSelection<ZonedDateTime, Double> selection, boolean toHistory) {
            if (toHistory) {
                double r = (((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getUpperBound() - ((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getLowerBound()) - Math.abs(selection.getEndY() - selection.getStartY());
                logger.debug(() -> "Y selection - Y axis range = " + r);
                if (r > 0.0001) {
                    chartViewPort.getDataStore().setAutoScaleYAxis(false);
                }
            }
            else {
                // Disable auto range on Y axis if zoomed in
                chartViewPort.getDataStore().setAutoScaleYAxis(selection.isAutoRangeY());
            }
            this.startY.set(roundYValue(selection.getStartY()));
            this.endY.set(roundYValue(selection.getEndY()));
        }

        private double roundYValue(double y) {
            return y;
        }

        @Override
        public void close() {
            this.removeListeners();
        }
    }

    public void suspendAxisListeners() {
        this.startX.removeListener(onRefreshAllRequired);
        this.endX.removeListener(onRefreshAllRequired);
        axisStates.values().forEach(AxisState::removeListeners);
    }

    public void resumeAxisListeners() {
        this.startX.addListener(onRefreshAllRequired);
        this.endX.addListener(onRefreshAllRequired);
        axisStates.values().forEach(AxisState::addListeners);
    }

    public ChartViewportsState(WorksheetController parent, ZonedDateTime startX, ZonedDateTime endX) {
        this.parent = parent;
        onRefreshAllRequired =  (observable, oldValue, newValue) -> parent.invalidateAll(true, false, false);
        this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
        this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
        this.startX.addListener(onRefreshAllRequired);
        this.endX.addListener(onRefreshAllRequired);
        for (ChartViewPort<Double> viewPort : parent.viewPorts) {
            this.put(viewPort.getDataStore(), new AxisState(viewPort, viewPort.getDataStore().getyAxisMinValue(), viewPort.getDataStore().getyAxisMaxValue()));
            this.get(viewPort.getDataStore()).ifPresent(y -> {
                viewPort.getDataStore().yAxisMinValueProperty().bindBidirectional(y.startY);
                ((ValueAxis<Double>) viewPort.getChart().getYAxis()).lowerBoundProperty().bindBidirectional(y.startY);
                viewPort.getDataStore().yAxisMaxValueProperty().bindBidirectional(y.endY);
                ((ValueAxis<Double>) viewPort.getChart().getYAxis()).upperBoundProperty().bindBidirectional(y.endY);
            });
        }
        parent.getWorksheet().fromDateTimeProperty().bind(this.startX);
        parent.getWorksheet().toDateTimeProperty().bind(this.endX);

    }

    public ZonedDateTime getStartX() {
        return startX.get();
    }

    public SimpleObjectProperty<ZonedDateTime> startXProperty() {
        return startX;
    }

    public ZonedDateTime getEndX() {
        return endX.get();
    }

    public SimpleObjectProperty<ZonedDateTime> endXProperty() {
        return endX;
    }

    public Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> asSelection() {
        Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selection = new HashMap<>();
        for (Map.Entry<Chart<Double>, AxisState> e : axisStates.entrySet()) {
            selection.put(e.getKey(), e.getValue().asSelection());
        }
        return selection;
    }

    public Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selectTimeRange(ZonedDateTime start, ZonedDateTime end) {
        Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selection = new HashMap<>();

        for (Map.Entry<Chart<Double>, AxisState> e : axisStates.entrySet()) {
            selection.put(e.getKey(), e.getValue().selectTimeRange(start, end));
        }
        return selection;
    }

    public void setSelection(Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selectionMap, boolean toHistory) {
        this.suspendAxisListeners();
        try {
            selectionMap.forEach((chart, xyChartSelection) -> get(chart).ifPresent(y -> y.setSelection(xyChartSelection, toHistory)));
            selectionMap.entrySet().stream().findFirst().ifPresent(entry -> {
                ZonedDateTime newStartX = roundDateTime(entry.getValue().getStartX());
                ZonedDateTime newEndX = roundDateTime(entry.getValue().getEndX());
                boolean dontPlotChart = newStartX.isEqual(startX.get()) && newEndX.isEqual(endX.get());
                this.startX.set(newStartX);
                this.endX.set(newEndX);
                selectionMap.forEach((chart, xyChartSelection) -> get(chart).ifPresent(y -> y.setSelection(xyChartSelection, toHistory)));
                parent.invalidateAll(toHistory, dontPlotChart, false);
            });
        } finally {
            this.resumeAxisListeners();
        }
    }

    public AxisState put(Chart<Double> chart, AxisState xyChartViewState) {
        return axisStates.put(chart, xyChartViewState);
    }

    public Optional<AxisState> get(Chart<Double> chart) {
        AxisState yState = axisStates.get(chart);
        if (yState != null) {
            return Optional.of(yState);
        }
        logger.debug(() -> "Could not find a saved state for chart " + chart.getName());
        return Optional.empty();
    }

    private ZonedDateTime roundDateTime(ZonedDateTime zdt) {
        return ZonedDateTime.of(zdt.getYear(),
                zdt.getMonthValue(),
                zdt.getDayOfMonth(),
                zdt.getHour(),
                zdt.getMinute(),
                zdt.getSecond(),
                0,
                zdt.getZone()
        );
    }
}
