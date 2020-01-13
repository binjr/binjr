/*
 *    Copyright 2016-2018 Frederic Thevenet
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

import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class MultiDimLargestTriangleThreeBucketsTransform extends TimeSeriesTransform {
    protected final int threshold;

    /**
     * Initializes a new instnace of the {@link MultiDimLargestTriangleThreeBucketsTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public MultiDimLargestTriangleThreeBucketsTransform(final int threshold) {
        super("LargestTriangleThreeBucketsTransform");
        this.threshold = threshold;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {


            //    return applyLTTBReduction(data, threshold);
            return null;
        }
        return data;
    }


    protected List<List<XYChart.Data<ZonedDateTime, Double>>> applyMulti(List<List<XYChart.Data<ZonedDateTime, Double>>> data) {

        if (data != null && data.size() >0 &&threshold > 0 && data.get(0).size() > threshold) {


            //    return applyLTTBReduction(data, threshold);
            return null;
        }
        return data;
    }


    /**
     * <p>Method implementing the Largest-Triangle-Three-Buckets algorithm.</p>
     * <p>Adapted from <a href="https://gist.github.com/DanielWJudge/63300889f27c7f50eeb7">DanielWJudge/LargestTriangleThreeBuckets.cs</a></p>
     *
     * @param data      the list of sample to apply the reduction to.
     * @param threshold d the maximum number of samples to keep.
     * @return a reduced list of samples.
     */
    private List<XYChart.Data<ZonedDateTime, Double[]>> applyLTTBReduction(List<XYChart.Data<ZonedDateTime, Double[]>> data, int threshold) {
        int dataLength = data.size();

        List<XYChart.Data<ZonedDateTime, Double[]>> sampled = new ArrayList<>();
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        XYChart.Data<ZonedDateTime, Double[]> maxAreaPoint = data.get(a);
        sampled.add(data.get(a)); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            int nbDim = data.get(a).getYValue().length;
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double[] avgY = new double[nbDim];
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = Math.min(avgRangeEnd, dataLength);
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += data.get(avgRangeStart).getXValue().toInstant().toEpochMilli();
                for (int j = 0; j < nbDim; j++) {
                    avgY[j] += data.get(avgRangeStart).getYValue()[j];
                }
            }
            avgX /= avgRangeLength;
            for (int j = 0; j < nbDim; j++) {
                avgY[j] = avgY[j] / avgRangeLength;
            }
            // Get the range for this bucket
            int rangeOffs = (int) (Math.floor((i) * every) + 1);
            int rangeTo = (int) (Math.floor((i + 1) * every) + 1);

            // Point a
            double pointAx = data.get(a).getXValue().toInstant().toEpochMilli();
            Double[] pointAy = data.get(a).getYValue();
            double maxArea = -1;//
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double[] area = new double[nbDim];
                for (int j = 0; j < nbDim; j++) {
                    area[j] = Math.abs((pointAx - avgX) * (data.get(rangeOffs).getYValue()[j] - pointAy[j]) -
                            (pointAx - data.get(rangeOffs).getXValue().toInstant().toEpochMilli()) * (avgY[j] - pointAy[j])
                    ) * 0.5;
                    if (area[j] > maxArea) {
                        maxArea = area[j];
                        maxAreaPoint = data.get(rangeOffs);
                        nextA = rangeOffs; // Next a is this b
                    }
                }


            }
            sampled.add(maxAreaPoint); // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }
        sampled.add(data.get(dataLength - 1)); // Always add last
        return sampled;
    }


}