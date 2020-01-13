/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.binjr.core.preferences;

import eu.binjr.core.data.timeseries.transform.*;
import eu.binjr.core.data.workspace.ChartType;

import java.util.function.BiFunction;

/**
 * An enumeration of supported sample reducing transformation.
 *
 * @author Frederic Thevenet
 */
public enum DownSamplingAlgorithm {
    AUTO("Automatic", (type, threshold) ->
            type == ChartType.STACKED ? new FirstPassLttbTransform(threshold) : new LargestTriangleThreeBucketsTransform(threshold)),
    DECIMATION("Decimation", (type, threshold) -> new DecimationTransform(threshold)),
    LTTB("Largest Triangle Three Buckets", (type, threshold) -> new LargestTriangleThreeBucketsTransform(threshold)),
    TWO_PASS_LTTB("Two-pass Largest Triangle Three Buckets", (type, threshold)-> new FirstPassLttbTransform(threshold)),
    AVERAGE("Average resampling", ((type, threshold) -> new AverageResamplingTransform(threshold)));

    private final String name;
    private final BiFunction<ChartType, Integer, TimeSeriesTransform> factory;

    DownSamplingAlgorithm(String name, BiFunction<ChartType, Integer, TimeSeriesTransform> factory) {
        this.name = name;
        this.factory = factory;
    }

    public String getName() {
        return name;
    }

    public TimeSeriesTransform instantiateTransform(ChartType chartType, Integer threshold) {
        return factory.apply(chartType, threshold);
    }
}

