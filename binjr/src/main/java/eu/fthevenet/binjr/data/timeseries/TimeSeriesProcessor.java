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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The base class for time series processor classes, which holds raw data points and provides access to summary properties.
 *
 * @author Frederic Thevenet
 */
public abstract class TimeSeriesProcessor<T> {
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
     * Try to get the nearest value for the specified time stamp.
     * <p>
     * <p><b>Remark:</b> If the processor is already being accessed by another thread, returns immediately with Optional.empty</p>
     *
     * @param xValue the time stamp to get the value for.
     * @return An {@link Optional} instance that contains tthe value for the time position nearest to the one requested if process could complete and value is non-null.
     */
    public Optional<T> tryGetNearestValue(ZonedDateTime xValue) {
        // If the lock is already acquired, just abandon the request
        return monitor.read().tryLock(this::unsafeGetNearestValue, xValue);
    }

    /**
     * Get the nearest value for the specified time stamp.
     * <p>
     * <p><b>Remark:</b> If the processor is already being accessed by another thread, waits until lock is released and returns requested value</p>
     *
     * @param xValue the time stamp to get the value for.
     * @return the value for the time position nearest to the one requested.
     */
    public T getNearestValue(ZonedDateTime xValue) {
        return monitor.read().lock(this::unsafeGetNearestValue, xValue);
    }

    /**
     * Gets the data of the {@link TimeSeriesProcessor}
     * <p>
     * <p><b>Remark:</b> the returned collection is a shallow copy of the the processor's own backing collection,
     * so it can be iterated through without risking a concurrent access error even if content is being added or
     * removed to the processor on a seperate thread. However, the the actual data for individual samples  are
     * not guarded against concurrent access in any capacity.</p>
     *
     * @return the data of the {@link TimeSeriesProcessor}
     */
    public Collection<XYChart.Data<ZonedDateTime, T>> getData() {
        return monitor.read().lock(() -> new ArrayList<>(data));
    }

    /**
     * Sets the content for the {@link TimeSeriesProcessor}'s data store
     *
     * @param newData the list of {@link XYChart.Data} points to use as the {@link TimeSeriesProcessor}' data.
     */
    public void setData(Collection<XYChart.Data<ZonedDateTime, T>> newData) {
        monitor.write().lock(() -> this.data = new ArrayList<>(newData));
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

    private T unsafeGetNearestValue(ZonedDateTime xValue) {
        T value = null;
        if (xValue != null && data != null) {
            for (XYChart.Data<ZonedDateTime, T> sample : data) {
                value = sample.getYValue();
                if (xValue.isBefore(sample.getXValue())) {
                    //TODO check if previous value is nearer and return corresponding value instead
                    return value;
                }
            }
        }
        return value;
    }

}
