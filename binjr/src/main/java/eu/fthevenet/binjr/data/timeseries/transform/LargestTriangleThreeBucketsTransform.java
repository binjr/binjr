package eu.fthevenet.binjr.data.timeseries.transform;

import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 10/11/2016.
 */
public class LargestTriangleThreeBucketsTransform extends TimeSeriesTransform {

    public LargestTriangleThreeBucketsTransform(final int threshold) {
        super("LargestTriangleThreeBucketsTransform",
                m -> m.entrySet()
                         .parallelStream()
                         .collect(Collectors.toMap(Map.Entry::getKey,e-> reduce(e.getValue(),threshold ))));
    }

    private static List<XYChart.Data<Date, Number>> reduce(List<XYChart.Data<Date, Number>> data, int threshold) {
        int dataLength = data.size();
        if (threshold >= dataLength || threshold == 0)
            return data; // Nothing to do

        List<XYChart.Data<Date, Number>> sampled = new ArrayList<>(threshold);

        // Bucket size. Leave room for start and end data points
        double every = (double) (dataLength - 2) / (threshold - 2);

        int a = 0;
        XYChart.Data<Date, Number> maxAreaPoint = new XYChart.Data<>(new Date(0), 0);
        int nextA = 0;

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
                avgX += data.get(avgRangeStart).getXValue().getTime(); // * 1 enforces Number (value may be Date)
                avgY += data.get(avgRangeStart).getYValue().doubleValue();
            }
            avgX /= avgRangeLength;

            avgY /= avgRangeLength;

            // Get the range for this bucket
            int rangeOffs = (int) (Math.floor((i) * every) + 1);
            int rangeTo = (int) (Math.floor((i + 1) * every) + 1);

            // Point a
            double pointAx = data.get(a).getXValue().getTime(); // enforce Number (value may be Date)
            double pointAy = data.get(a).getYValue().doubleValue();

            double maxArea = -1;

            for (; rangeOffs < rangeTo; rangeOffs++) {
                // Calculate triangle area over three buckets
                double area = Math.abs((pointAx - avgX) * (data.get(rangeOffs).getYValue().doubleValue() - pointAy) -
                        (pointAx - data.get(rangeOffs).getXValue().getTime()) * (avgY - pointAy)
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