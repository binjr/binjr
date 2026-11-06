/*
 *    Copyright 2020-2026 Frederic Thevenet
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class SecondPassLttbTransform extends BaseTimeSeriesTransform<Double> {
    protected final int threshold;
    private final BitSet keepList;
    private static final Logger logger = Logger.create(SecondPassLttbTransform.class);

    /**
     * Initializes a new instnace of the {@link SecondPassLttbTransform} class.
     *
     * @param firstPass A reference to the TimeSeriesTransform that was applied as the first pass of the algorithm.
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public SecondPassLttbTransform(final FirstPassLttbTransform firstPass, int threshold) {
        super("SecondPassLttbTransform");
        this.setEnabled(firstPass.isEnabled());
        this.threshold = threshold;
        this.keepList = LargestTriangleThreeBuckets.computeKeepList(threshold, firstPass.getTimeStamps(), firstPass.getSeriesValues());
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            var filtered = new ArrayList<XYChart.Data<ZonedDateTime, Double>>();
            for (int i = keepList.nextSetBit(0); i >= 0; i = keepList.nextSetBit(i+1)) {
                filtered.add(data.get(i));
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
            logger.debug(() -> "Series reduced from " + data.size() + " to " + filtered.size() + " samples.");
            return filtered;
        }
        return data;
    }


}