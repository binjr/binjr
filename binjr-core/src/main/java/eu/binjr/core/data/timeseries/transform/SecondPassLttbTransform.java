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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class SecondPassLttbTransform extends BaseTimeSeriesTransform {
    protected final int threshold;
    private final boolean[] whiteList;
    private static final Logger logger = LogManager.getLogger(SecondPassLttbTransform.class);

    /**
     * Initializes a new instnace of the {@link SecondPassLttbTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public SecondPassLttbTransform(final FirstPassLttbTransform firstPass, int threshold) {
        super("SecondPassLttbTransform");
        this.setEnabled(firstPass.isEnabled());
        this.threshold = threshold;
        whiteList = computeWhileList(firstPass.getTimeStamps(), firstPass.getSeriesValues(), threshold);
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            var filtered = new ArrayList<XYChart.Data<ZonedDateTime, Double>>();
            for (int i = 0; i < data.size(); i++) {
                if (whiteList[i]) {
                    filtered.add(data.get(i));
                }
            }
            logger.debug(() -> "Series reduced from " + data.size() + " to " + filtered.size() + " samples.");
            return filtered;
        }
        return data;
    }


    /**
     * <p>Method implementing the Largest-Triangle-Three-Buckets algorithm.</p>
     * <p>Adapted from <a href="https://gist.github.com/DanielWJudge/63300889f27c7f50eeb7">DanielWJudge/LargestTriangleThreeBuckets.cs</a></p>
     *
     * @return a white list of samples indexes to retain after a reduction
     */
    private boolean[] computeWhileList(ZonedDateTime[] data, List<Double[]> seriesValues, int threshold) {
        int dataLength = data.length;
        int nbDim = seriesValues.size();
        boolean[] whiteList = new boolean[dataLength];
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        int maxAreaPointIdx = a;
        whiteList[a] = true;// Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double[] avgY = new double[nbDim];
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = Math.min(avgRangeEnd, dataLength);
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += data[avgRangeStart].toInstant().toEpochMilli();
                for (int j = 0; j < nbDim; j++) {
                    avgY[j] += seriesValues.get(j)[avgRangeStart];
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
            double pointAx = data[a].toInstant().toEpochMilli();
            double maxArea = -1;//
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double[] area = new double[nbDim];
                for (int j = 0; j < nbDim; j++) {
                    area[j] = Math.abs((pointAx - avgX) * (seriesValues.get(j)[rangeOffs] - seriesValues.get(j)[a]) -
                            (pointAx - data[rangeOffs].toInstant().toEpochMilli()) * (avgY[j] - seriesValues.get(j)[a])
                    ) * 0.5;
                    if (area[j] > maxArea) {
                        maxArea = area[j];
                        maxAreaPointIdx = rangeOffs;
                        nextA = rangeOffs; // Next a is this b
                    }
                }
            }
            whiteList[maxAreaPointIdx] = true; // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }
        whiteList[dataLength - 1] = true; // Always add last

        logger.debug(() -> "Lttb white list computed");
        return whiteList;
    }


}