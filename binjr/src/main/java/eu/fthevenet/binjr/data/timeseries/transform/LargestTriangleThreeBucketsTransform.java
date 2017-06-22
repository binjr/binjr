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
import javafx.scene.chart.XYChart;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of disreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class LargestTriangleThreeBucketsTransform<T extends Number> extends TimeSeriesTransform<T> {
    private final int threshold;

    /**
     * Initializes a new instnace of the {@link LargestTriangleThreeBucketsTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public LargestTriangleThreeBucketsTransform(final int threshold) {
        super("LargestTriangleThreeBucketsTransform");
        this.threshold = threshold;
    }

    @Override
    public Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> apply(Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> m) {
        return m.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> {
                    if (threshold > 0 && o.getValue().size() > threshold) {
                        o.getValue().setData(applyLTTBReduction(o.getValue(), threshold));
                    }
                    return o.getValue();
                }));
    }


    /**
     * <p>Method implementing the Largest-Triangle-Three-Buckets algorithm.</p>
     * <p>Adapted from <a href="https://gist.github.com/DanielWJudge/63300889f27c7f50eeb7">DanielWJudge/LargestTriangleThreeBuckets.cs</a></p>
     *
     * @param data      the list of sample to apply the reduction to.
     * @param threshold d the maximum number of samples to keep.
     * @return a reduced list of samples.
     */
    private Iterable<XYChart.Data<ZonedDateTime, T>> applyLTTBReduction(TimeSeriesProcessor<T> data, int threshold) {
        int dataLength = data.size();
        List<XYChart.Data<ZonedDateTime, T>> sampled = new ArrayList<>(threshold);
        ZoneId zoneId = data.getSample(0).getXValue().getZone();
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        XYChart.Data<ZonedDateTime, T> maxAreaPoint = data.getSample(a);
        sampled.add(data.getSample(a)); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double avgY = 0;
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = avgRangeEnd < dataLength ? avgRangeEnd : dataLength;
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += data.getSample(avgRangeStart).getXValue().toEpochSecond();
                avgY += data.getSample(avgRangeStart).getYValue().doubleValue();
            }
            avgX /= avgRangeLength;
            avgY /= avgRangeLength;
            // Get the range for this bucket
            int rangeOffs = (int) (Math.floor((i) * every) + 1);
            int rangeTo = (int) (Math.floor((i + 1) * every) + 1);

            // Point a
            double pointAx = data.getSample(a).getXValue().toEpochSecond();
            double pointAy = data.getSample(a).getYValue().doubleValue();
            double maxArea = -1;
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double area = Math.abs((pointAx - avgX) * (data.getSample(rangeOffs).getYValue().doubleValue() - pointAy) -
                        (pointAx - data.getSample(rangeOffs).getXValue().toEpochSecond()) * (avgY - pointAy)
                ) * 0.5;
                if (area > maxArea) {
                    maxArea = area;
                    maxAreaPoint = data.getSample(rangeOffs);
                    nextA = rangeOffs; // Next a is this b
                }
            }
            sampled.add(maxAreaPoint); // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }

        sampled.add(data.getSample(dataLength - 1)); // Always add last
        return sampled;
    }
}