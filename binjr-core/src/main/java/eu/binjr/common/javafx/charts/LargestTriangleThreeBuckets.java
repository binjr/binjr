/*
 * Copyright 2026 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.javafx.charts;

import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;


/**
 * Helpers for the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 */
public class LargestTriangleThreeBuckets {

    /**
     * Returns a bitset that flags which of the samples provided as input should be kept according to the LTTB algorithm.
     *
     * @param threshold    The mnumber of samples to keep.
     * @param dateTimes    The timestamps for the timeseries to downsample.
     * @param seriesValues The data  for the timeseries to downsample.
     * @return Returns a bitset that flags which of the samples which samples to keep.
     */
    static public BitSet computeKeepList(int threshold,
                                         ZonedDateTime[] dateTimes,
                                         long[]... seriesValues) {
        return computeKeepList(threshold, Long::doubleValue, dateTimes, seriesValues);
    }

    /**
     * Returns a bitset that flags which of the samples provided as input should be kept according to the LTTB algorithm.
     *
     * @param threshold    The mnumber of samples to keep.
     * @param decoder      The function to be used to convert long values to double.
     * @param dateTimes    The timestamps for the timeseries to downsample.
     * @param seriesValues The data  for the timeseries to downsample.
     * @return Returns a bitset that flags which of the samples which samples to keep.
     */
    static public BitSet computeKeepList(int threshold,
                                         Function<Long, Double> decoder,
                                         ZonedDateTime[] dateTimes,
                                         long[]... seriesValues) {
        int dataLength = dateTimes.length;
        int nbDim = seriesValues.length;
        BitSet keepList = new BitSet(dataLength);

        // Return immediately if the requested threshold is larger than the number of data points.
        if (dataLength <= threshold) {
            keepList.set(0, dataLength, true);
            return keepList;
        }

        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        int maxAreaPointIdx = a;
        keepList.set(a); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double[] avgY = new double[nbDim];
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = Math.min(avgRangeEnd, dataLength);
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += dateTimes[avgRangeStart].toInstant().toEpochMilli();
                for (int j = 0; j < nbDim; j++) {
                    avgY[j] += decoder.apply(seriesValues[j][avgRangeStart]);
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
            double pointAx = dateTimes[a].toInstant().toEpochMilli();
            double maxArea = -1;//
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double[] area = new double[nbDim];
                for (int j = 0; j < nbDim; j++) {
                    double pointAy = decoder.apply(seriesValues[j][a]);
                    double pointOffs = decoder.apply(seriesValues[j][rangeOffs]);
                    area[j] = Math.abs((pointAx - avgX) * (pointOffs - pointAy) -
                            (pointAx - dateTimes[rangeOffs].toInstant().toEpochMilli()) * (avgY[j] - pointAy)) * 0.5;
                    if (area[j] > maxArea) {
                        maxArea = area[j];
                        maxAreaPointIdx = rangeOffs;
                        nextA = rangeOffs; // Next a is this b
                    }
                }
            }
            keepList.set(maxAreaPointIdx); // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }
        keepList.set(dataLength - 1); // Always add last
        return keepList;
    }

    /**
     * <p>Method implementing the Largest-Triangle-Three-Buckets algorithm.</p>
     * <p>Adapted from <a href="https://gist.github.com/DanielWJudge/63300889f27c7f50eeb7">DanielWJudge/LargestTriangleThreeBuckets.cs</a></p>
     *
     * @return a list of samples indexes to retain after a reduction
     */
    public static BitSet computeKeepList(int threshold, ZonedDateTime[] dateTimes, List<Double[]> seriesValues) {
        int dataLength = dateTimes.length;
        int nbDim = seriesValues.size();
        BitSet keepList = new BitSet(dataLength);
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        int maxAreaPointIdx = a;
        keepList.set(a); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double[] avgY = new double[nbDim];
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = Math.min(avgRangeEnd, dataLength);
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += dateTimes[avgRangeStart].toInstant().toEpochMilli();
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
            double pointAx = dateTimes[a].toInstant().toEpochMilli();
            double maxArea = -1;//
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double[] area = new double[nbDim];
                for (int j = 0; j < nbDim; j++) {
                    area[j] = Math.abs((pointAx - avgX) * (seriesValues.get(j)[rangeOffs] - seriesValues.get(j)[a]) -
                            (pointAx - dateTimes[rangeOffs].toInstant().toEpochMilli()) * (avgY[j] - seriesValues.get(j)[a])) * 0.5;
                    if (area[j] > maxArea) {
                        maxArea = area[j];
                        maxAreaPointIdx = rangeOffs;
                        nextA = rangeOffs; // Next a is this b
                    }
                }
            }
            keepList.set(maxAreaPointIdx); // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }
        keepList.set(dataLength - 1); // Always add last
        return keepList;
    }

    /**
     * <p>Method implementing the Largest-Triangle-Three-Buckets algorithm.</p>
     * <p>Adapted from <a href="https://gist.github.com/DanielWJudge/63300889f27c7f50eeb7">DanielWJudge/LargestTriangleThreeBuckets.cs</a></p>
     *
     * @param threshold d the maximum number of samples to keep.
     * @param data      the list of sample to apply the reduction to.
     * @return a reduced list of samples.
     */
    public static List<XYChart.Data<ZonedDateTime, Double>> reduce(int threshold, List<XYChart.Data<ZonedDateTime, Double>> data) {
        int dataLength = data.size();
        List<XYChart.Data<ZonedDateTime, Double>> sampled = new ArrayList<>(threshold);
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);
        int a = 0;
        int nextA = 0;
        XYChart.Data<ZonedDateTime, Double> maxAreaPoint = data.get(a);
        sampled.add(data.get(a)); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double avgY = 0;
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = Math.min(avgRangeEnd, dataLength);
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += getXValue(data.get(avgRangeStart));
                avgY += getYValue(data.get(avgRangeStart));
            }
            avgX /= avgRangeLength;
            avgY /= avgRangeLength;
            // Get the range for this bucket
            int rangeOffs = (int) (Math.floor((i) * every) + 1);
            int rangeTo = (int) (Math.floor((i + 1) * every) + 1);

            // Point a
            double pointAx = getXValue(data.get(a));
            double pointAy = getYValue(data.get(a));
            double maxArea = -1;
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double area = Math.abs((pointAx - avgX) * (getYValue(data.get(rangeOffs)) - pointAy) -
                        (pointAx - getXValue(data.get(rangeOffs))) * (avgY - pointAy)
                ) * 0.5;
                if (area > maxArea) {
                    maxArea = area;
                    maxAreaPoint = data.get(rangeOffs);
                    nextA = rangeOffs; // Next a is this b
                }
            }
            sampled.add(maxAreaPoint); // Pick this point from the bucket
            a = nextA; // This a is the next a (chosen b)
        }
        sampled.add(data.get(dataLength - 1)); // Always add last
        return sampled;
    }

    private static double getYValue(XYChart.Data<ZonedDateTime, Double> sample) {
        if (Double.isNaN(sample.getYValue())) {
            return 0.0;
        }
        return sample.getYValue();
    }

    private static double getXValue(XYChart.Data<ZonedDateTime, Double> sample) {
        return sample.getXValue().toInstant().toEpochMilli();
    }
}
