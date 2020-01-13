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
public class FirstPassLttbTransform extends BaseTimeSeriesTransform implements TimeSeriesAccumulator {
    protected final int threshold;
    private final List<Double[]> accumulationBuffer;
    private static final Logger logger = LogManager.getLogger(FirstPassLttbTransform.class);

    /**
     * Initializes a new instnace of the {@link FirstPassLttbTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public FirstPassLttbTransform(final int threshold) {
        super("FirstPassLttbTransform");
        this.threshold = threshold;
        accumulationBuffer = new ArrayList<>();
    }

    @Override
    public List<Double[]> getAccumulationBuffer() {
        return accumulationBuffer;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        // collect values for second pass
        if (threshold > 0 && data.size() > threshold) {
            accumulationBuffer.add(data.stream().map(XYChart.Data::getYValue).toArray(Double[]::new));
        }
        return data;
    }

    @Override
    public TimeSeriesTransform getNextPassTransform() {
        return new SecondPassLttbTransform(this, threshold);
    }
}