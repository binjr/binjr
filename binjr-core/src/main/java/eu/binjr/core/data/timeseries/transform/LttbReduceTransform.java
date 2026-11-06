/*
 *    Copyright 2016-2026 Frederic Thevenet
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

package eu.binjr.core.data.timeseries.transform;

import eu.binjr.common.javafx.charts.LargestTriangleThreeBuckets;
import eu.binjr.common.logging.Logger;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class LttbReduceTransform extends BaseTimeSeriesTransform<Double> {
    protected final int threshold;
    private static final Logger logger = Logger.create(LttbReduceTransform.class);

    /**
     * Initializes a new instnace of the {@link LttbReduceTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public LttbReduceTransform(final int threshold) {
        super("LttbReduceTransform");
        this.threshold = threshold;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            var reduced = LargestTriangleThreeBuckets.reduce(threshold, data);
            LttbReduceTransform.logger.debug(() -> "Series reduced from " + data.size() +
                    " to " + reduced.size() + " samples.");
            return reduced;
        }
        return data;
    }

}