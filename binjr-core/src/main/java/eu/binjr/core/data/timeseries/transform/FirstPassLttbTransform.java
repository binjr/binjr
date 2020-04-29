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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 *
 * @author Frederic Thevenet
 */
public class FirstPassLttbTransform extends BaseTimeSeriesTransform {
    protected final int threshold;
    private final List<Double[]> seriesValues;
    private ZonedDateTime[] timeStamps;
    private static final Logger logger = LogManager.getLogger(FirstPassLttbTransform.class);

    /**
     * Initializes a new instnace of the {@link FirstPassLttbTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public FirstPassLttbTransform(final int threshold) {
        super("FirstPassLttbTransform");
        this.threshold = threshold;
        seriesValues = new ArrayList<>();
    }

    public List<Double[]> getSeriesValues() {
        return seriesValues;
    }

    public ZonedDateTime[] getTimeStamps() {
        return timeStamps;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        // collect values for second pass
        if (threshold > 0 && data.size() > threshold) {
            var values = data.stream().map(XYChart.Data::getYValue).toArray(Double[]::new);
            synchronized (seriesValues) {
                seriesValues.add(values);
                if (timeStamps == null) {
                    timeStamps = data.stream().map(XYChart.Data::getXValue).toArray(ZonedDateTime[]::new);
                }
            }
        }
        return data;
    }

    @Override
    public TimeSeriesTransform getNextPassTransform() {
        if (timeStamps == null) {
            logger.debug(() -> "No data collected from first pass: return noOp transform");
            return new NoOpTransform();
        }
        boolean isBufferCoherent = true;
        int nbSamples = timeStamps.length;
        for (var b : seriesValues) {
            isBufferCoherent = isBufferCoherent & (b.length == nbSamples);
        }
        if (!isBufferCoherent) {
            logger.debug(() -> "Collected series data are not coherent: falling back to single pass lttb");
            return new LargestTriangleThreeBucketsTransform(threshold);
        }
        return new SecondPassLttbTransform(this, threshold);
    }
}