/*
 *    Copyright 2017 Frederic Thevenet
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

package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.util.concurrent.ReadWriteLockHelper;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The base class for time series processor classes, which holds raw data points and provides access to summary properties.
 *
 * @author Frederic Thevenet
 */
public abstract class TimeSeriesProcessor<T extends Number> {
    private static final Logger logger = LogManager.getLogger(TimeSeriesProcessor.class);
    private final ReadWriteLockHelper monitor = new ReadWriteLockHelper();
    protected List<XYChart.Data<ZonedDateTime, T>> data;

    /**
     * Initializes a new instance of the {@link TimeSeriesProcessor} class with the provided {@link TimeSeriesBinding}.
     */
    public TimeSeriesProcessor() {
        this.data = new ArrayList<>();
    }

    /**
     * Gets the minimum value for the Y coordinates of the {@link TimeSeriesProcessor}
     *
     * @return the minimum value for the Y coordinates of the {@link TimeSeriesProcessor}
     */
    public final T getMinValue() {
        return monitor.read().lock(this::computeMinValue);
    }

    /**
     * Gets the average for all Y coordinates of the {@link TimeSeriesProcessor}
     *
     * @return the average for all Y coordinates of the {@link TimeSeriesProcessor}
     */
    public final T getAverageValue() {
        return monitor.read().lock(this::computeAverageValue);
    }

    /**
     * Gets the maximum value for the Y coordinates of the {@link TimeSeriesProcessor}
     *
     * @return the maximum value for the Y coordinates of the {@link TimeSeriesProcessor}
     */
    public final T getMaxValue() {
        return monitor.read().lock(this::computeMaxValue);
    }

    /**
     * Gets the nearest value for the specified time stamp.
     *
     * @param xValue the time stamp to get the value for.
     * @return An {@link Optional} instance that contains the value for the specified time stamp if process could complete and value is non-null.
     */
    public Optional<T> getNearestValue(ZonedDateTime xValue) {
        // If the lock is already acquired, just abandon the request
        return monitor.read().tryLock(() -> {
            T value = null;
            if (xValue != null && data != null) {
                for (XYChart.Data<ZonedDateTime, T> sample : data) {
                    value = sample.getYValue();
                    if (xValue.isBefore(sample.getXValue())) {
                        return value;
                    }
                }
            }
            return value;
        });
    }

    /**
     * Gets the data of the {@link TimeSeriesProcessor}
     *
     * @return the data of the {@link TimeSeriesProcessor}
     */
    public Iterable<XYChart.Data<ZonedDateTime, T>> getData() {
        return monitor.read().lock(() -> this.data);
    }

    /**
     * Sets the content for the {@link TimeSeriesProcessor}'s data store
     *
     * @param data the list of {@link XYChart.Data} points to use as the {@link TimeSeriesProcessor}' data.
     */
    public void setData(Iterable<XYChart.Data<ZonedDateTime, T>> data) {
        monitor.write().lock(() -> {
            this.data.clear();
            for (XYChart.Data<ZonedDateTime, T> sample : data) {
                this.data.add(sample);
            }
        });
    }

    /**
     * Returns the data sample at the given index.
     *
     * @param index the index of the sample to retrieve.
     * @return the data sample at the given index.
     */
    public XYChart.Data<ZonedDateTime, T> getSample(int index) {
        return monitor.read().lock(() -> this.data.get(index));
    }

    /**
     * Returns the number of elements in the processor's data store
     *
     * @return the number of elements in the processor's data store
     */
    public int size() {
        return monitor.read().lock(() -> this.data.size());
    }

    /**
     * Adds a new sample to the processor's data store
     *
     * @param sample a new sample to add to the processor's data store
     */
    public void addSample(XYChart.Data<ZonedDateTime, T> sample) {
        monitor.write().lock(() -> this.data.add(sample));
    }

    protected abstract T computeMinValue();

    protected abstract T computeAverageValue();

    protected abstract T computeMaxValue();

}
