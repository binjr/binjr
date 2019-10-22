/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.common.javafx.charts.XYChartSelection;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
 *
 * @author Frederic Thevenet
 */
public class ChartViewportsState implements AutoCloseable {
    private final WorksheetController parent;
    private static final Logger logger = LogManager.getLogger(ChartViewportsState.class);
    private HashMap<Chart, AxisState> axisStates = new HashMap<>();
    private final SimpleObjectProperty<ZonedDateTime> startX;
    private final SimpleObjectProperty<ZonedDateTime> endX;
    private final ChangeListener<ZonedDateTime> onRefreshAllRequired;
    private final ReadOnlyObjectWrapper<TimeRange> timeRange = new ReadOnlyObjectWrapper<>();

    @Override
    public void close() {
        this.startX.removeListener(onRefreshAllRequired);
        this.endX.removeListener(onRefreshAllRequired);
        IOUtils.closeCollectionElements(axisStates.values());
    }

    /**
     * Returns the {@link TimeRange} for the state.
     *
     * @return the {@link TimeRange} for the state.
     */
    public TimeRange getTimeRange() {
        return timeRange.get();
    }

    /**
     * The timeRange property.
     *
     * @return the timeRange property.
     */
    public ReadOnlyObjectProperty<TimeRange> timeRangeProperty() {
        return timeRange.getReadOnlyProperty();
    }

    /**
     * Represents the state of an {@link javafx.scene.chart.Axis}
     */
    public class AxisState implements AutoCloseable {
        private final SimpleDoubleProperty startY;
        private final SimpleDoubleProperty endY;
        private final ChartViewPort chartViewPort;
        private final ChangeListener<Number> onRefreshViewportRequired;


        /**
         * Initializes a new instance of the {@link AxisState} class.
         *
         * @param chartViewPort the {@link ChartViewPort} instance attached to the axis.
         * @param startY        the lower bound of the Y axis
         * @param endY          the upper bound of the Y axis
         */
        public AxisState(ChartViewPort chartViewPort, double startY, double endY) {
            this.chartViewPort = chartViewPort;
            this.onRefreshViewportRequired = (observable, oldValue, newValue) -> parent.invalidate(chartViewPort, true, false);
            this.startY = new SimpleDoubleProperty(startY);
            this.endY = new SimpleDoubleProperty(endY);
            this.addListeners();
        }

        /**
         * Remove all listeners
         */
        public void removeListeners() {
            this.startY.removeListener(onRefreshViewportRequired);
            this.endY.removeListener(onRefreshViewportRequired);
        }

        /**
         * All listeners
         */
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

        /**
         * Retrieves a {@link XYChartSelection} for the selected time range.
         *
         * @param beginning the beginning of the time range.
         * @param end       the end of the time range.
         * @return a {@link XYChartSelection} for the selected time range.
         */
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
        public void setSelection(XYChartSelection<ZonedDateTime, Double> selection, boolean toHistory) {
            if (toHistory) {
                double r = (((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getUpperBound() - ((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getLowerBound()) - Math.abs(selection.getEndY() - selection.getStartY());
                logger.debug(() -> "Y selection - Y axis range = " + r);
                if (r > 0.0001) {
                    chartViewPort.getDataStore().setAutoScaleYAxis(false);
                }
            } else {
                // Disable auto range on Y axis if zoomed in
                chartViewPort.getDataStore().setAutoScaleYAxis(selection.isAutoRangeY());
            }
            this.startY.set(selection.getStartY());
            this.endY.set(selection.getEndY());
        }



        @Override
        public void close() {
            this.removeListeners();
        }
    }

    /**
     * Suspends listeners
     */
    public void suspendAxisListeners() {
        this.startX.removeListener(onRefreshAllRequired);
        this.endX.removeListener(onRefreshAllRequired);
        axisStates.values().forEach(AxisState::removeListeners);
    }

    /**
     * Resumes listeners
     */
    public void resumeAxisListeners() {
        this.startX.addListener(onRefreshAllRequired);
        this.endX.addListener(onRefreshAllRequired);
        axisStates.values().forEach(AxisState::addListeners);
    }

    /**
     * Initializes a new instance of the {@link ChartViewportsState} class.
     *
     * @param parent the parent worksheet controller
     * @param startX the beginning of the time range.
     * @param endX   the end of the time range.
     */
    public ChartViewportsState(WorksheetController parent, ZonedDateTime startX, ZonedDateTime endX) {
        this.parent = parent;
        onRefreshAllRequired = (observable, oldValue, newValue) -> parent.invalidateAll(true, false, false);
        this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
        this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
        this.startX.addListener(onRefreshAllRequired);
        this.endX.addListener(onRefreshAllRequired);
        for (ChartViewPort viewPort : parent.viewPorts) {
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

    /**
     * Returns the beginning the range for the X axis
     *
     * @return the beginning the range for the X axis
     */
    public ZonedDateTime getStartX() {
        return startX.get();
    }

    /**
     * The startX property
     *
     * @return the startX property.
     */
    public SimpleObjectProperty<ZonedDateTime> startXProperty() {
        return startX;
    }

    /**
     * Returns the end the range for the X axis.
     *
     * @return the end the range for the X axis.
     */
    public ZonedDateTime getEndX() {
        return endX.get();
    }

    /**
     * The endX property.
     *
     * @return the endX property.
     */
    public SimpleObjectProperty<ZonedDateTime> endXProperty() {
        return endX;
    }

    /**
     * Returns the current state for all charts as a map of {@link XYChartSelection} instances.
     *
     * @return the current state for all charts as a map of {@link XYChartSelection} instances.
     */
    public Map<Chart, XYChartSelection<ZonedDateTime, Double>> asSelection() {
        Map<Chart, XYChartSelection<ZonedDateTime, Double>> selection = new HashMap<>();
        for (Map.Entry<Chart, AxisState> e : axisStates.entrySet()) {
            selection.put(e.getKey(), e.getValue().asSelection());
        }
        return selection;
    }

    /**
     * Returns the current state for all charts as a map of {@link XYChartSelection} instances for the selected time range.
     *
     * @param start the beginning of the time range.
     * @param end   the end of the time range.
     * @return the current state for all charts as a map of {@link XYChartSelection} instances for the selected time range.
     */
    public Map<Chart, XYChartSelection<ZonedDateTime, Double>> selectTimeRange(ZonedDateTime start, ZonedDateTime end) {
        Map<Chart, XYChartSelection<ZonedDateTime, Double>> selection = new HashMap<>();

        for (Map.Entry<Chart, AxisState> e : axisStates.entrySet()) {
            selection.put(e.getKey(), e.getValue().selectTimeRange(start, end));
        }
        return selection;
    }

    /**
     * Mutates the current state according to the provided map of selections.
     *
     * @param selectionMap a map of selections to apply.
     * @param toHistory    true if state change should be added to hitory, false otherwise.
     */
    public void setSelection(Map<Chart, XYChartSelection<ZonedDateTime, Double>> selectionMap, boolean toHistory) {
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
            timeRange.set(TimeRange.of(startX.getValue(), endX.getValue()));
        } finally {
            this.resumeAxisListeners();
        }
    }

    /**
     * Associate the provided state to the specified chart.
     *
     * @param chart            the chart to associated a state to
     * @param xyChartViewState the state to associate.
     * @return the state associated.
     */
    public AxisState put(Chart chart, AxisState xyChartViewState) {
        return axisStates.put(chart, xyChartViewState);
    }

    /**
     * Returns an {@link Optional} of {@link AxisState} that contains the state associated to the sepcified chart.
     *
     * @param chart the chart to get the state for.
     * @return an {@link Optional} of {@link AxisState} that contains the state associated to the sepcified chart.
     */
    public Optional<AxisState> get(Chart chart) {
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
