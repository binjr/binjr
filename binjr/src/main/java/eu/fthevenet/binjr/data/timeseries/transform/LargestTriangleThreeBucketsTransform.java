package eu.fthevenet.binjr.data.timeseries.transform;

import javafx.scene.chart.XYChart;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 10/11/2016.
 */
public class LargestTriangleThreeBucketsTransform<T extends Number> extends TimeSeriesTransform<T> {
    private final int threshold;

    public LargestTriangleThreeBucketsTransform(final int threshold) {
        super("LargestTriangleThreeBucketsTransform");
        this.threshold = threshold;
    }

    @Override
    public Map<String, List<XYChart.Data<ZonedDateTime, T>>> transform(Map<String, List<XYChart.Data<ZonedDateTime, T>>> m) {
        return  m.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> applyLTTBReduction(e.getValue(), threshold)));
    }

    private  List<XYChart.Data<ZonedDateTime, T>> applyLTTBReduction(List<XYChart.Data<ZonedDateTime, T>> data, int threshold) {
        int dataLength = data.size();
        if (threshold >= dataLength || threshold == 0)
            return data; // Nothing to do

        List<XYChart.Data<ZonedDateTime, T>> sampled = new ArrayList<>(threshold);
        ZoneId zoneId = data.get(0).getXValue().getZone();
        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);

        int a = 0;
        int nextA = 0;
        XYChart.Data<ZonedDateTime, T> maxAreaPoint = data.get(a);// = new XYChart.Data<>(ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), zoneId),(T) new Integer(0));
        sampled.add(data.get(a)); // Always add the first point
        for (int i = 0; i < threshold - 2; i++) {
            // Calculate point average for next bucket (containing c)
            double avgX = 0;
            double avgY = 0;
            int avgRangeStart = (int) (Math.floor((i + 1) * every) + 1);
            int avgRangeEnd = (int) (Math.floor((i + 2) * every) + 1);
            avgRangeEnd = avgRangeEnd < dataLength ? avgRangeEnd : dataLength;
            int avgRangeLength = avgRangeEnd - avgRangeStart;
            for (; avgRangeStart < avgRangeEnd; avgRangeStart++) {
                avgX += data.get(avgRangeStart).getXValue().toEpochSecond();
                avgY += data.get(avgRangeStart).getYValue().doubleValue();
            }
            avgX /= avgRangeLength;
            avgY /= avgRangeLength;
            // Get the range for this bucket
            int rangeOffs = (int) (Math.floor((i) * every) + 1);
            int rangeTo = (int) (Math.floor((i + 1) * every) + 1);

            // Point a
            double pointAx = data.get(a).getXValue().toEpochSecond();
            double pointAy = data.get(a).getYValue().doubleValue();
            double maxArea = -1;
            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double area = Math.abs((pointAx - avgX) * (data.get(rangeOffs).getYValue().doubleValue() - pointAy) -
                        (pointAx - data.get(rangeOffs).getXValue().toEpochSecond()) * (avgY - pointAy)
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
}