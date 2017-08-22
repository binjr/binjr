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

package eu.fthevenet.binjr.data.timeseries.transform;

import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.util.logging.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * The base class for time series transformation functions.
 *
 * @author Frederic Thevenet
 */
public abstract class TimeSeriesTransform<T extends Number> {
    private static final Logger logger = LogManager.getLogger(TimeSeriesTransform.class);
    private final String name;

    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     *
     * @param name the name of the transform function
     */
    public TimeSeriesTransform(String name) {
        this.name = name;
    }

    /**
     * The actual transform implementation
     *
     * @param series the time series to apply the transform to.
     * @return A map of the transformed series.
     */
    protected abstract Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> apply(Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> series);

    /**
     * Applies the transform function to the provided series
     *
     * @param series  the time series to apply the transform to.
     * @param enabled true if the transform should be applied, false otherwise.
     * @return A map of the transformed series.
     */
    public Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> transform(Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> series, boolean enabled) {
        String names = series.keySet().stream().map(tTimeSeriesInfo -> tTimeSeriesInfo.getBinding().getLabel()).reduce((s, s2) -> s + " " + s2).orElse("null");
        if (enabled) {
            try (Profiler ignored = Profiler.start("Applying transform" + getName() + " to series " + names, logger::trace)) {
                return apply(series);
            }
        }
        else {
            logger.debug(() -> "Transform " + getName() + " on series " + names + " is disabled.");
        }
        return series;
    }

    /**
     * Gets the name of the transform function
     *
     * @return the name of the transform function
     */
    public String getName() {
        return name;
    }
}
