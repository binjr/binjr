/*
 *    Copyright 2020 Frederic Thevenet
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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

//TODO This is buggy: this introduce a eft to right offset.
public final class AverageResamplingTransform extends TimeSeriesTransform {
    private static final Logger logger = LogManager.getLogger(AverageResamplingTransform.class);
    private final int threshold;

    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     */
    public AverageResamplingTransform(int threshold) {
        super("AverageResamplingTransform");
        this.threshold = threshold;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            List<XYChart.Data<ZonedDateTime, Double>> reduced = new ArrayList<>(threshold);
            var start = data.get(0).getXValue();
            var end = data.get(data.size() - 1).getXValue();
            double stepNanos = (Duration.between(start, end).toNanos()) / (double) (threshold);
            reduced.add(data.get(0));
            var nextSampleTime = start.plusNanos((long) Math.floor(stepNanos));
            double bucketAgg = 0;
            long bucketSize = 0;
            for (int i = 1; i < data.size() - 1; i++) {
                if (data.get(i).getXValue().isBefore(nextSampleTime)) {
                    bucketAgg += data.get(i).getYValue();
                    bucketSize++;
                } else {
                    reduced.add(new XYChart.Data<>(nextSampleTime, bucketAgg / bucketSize));
                    //initialize next bucket
                    nextSampleTime = nextSampleTime.plusNanos((long) Math.floor(stepNanos));
                    bucketAgg = data.get(i).getYValue();
                    bucketSize = 1;
                }
            }
            reduced.add(data.get(data.size() - 1));
            logger.info(() -> "Series reduced from " + data.size() + " to " + reduced.size() + " samples.");
            return reduced;
        }

        return data;
    }

}
