/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link BaseTimeSeriesTransform} that operates a linear decimation on the provided series.
 *
 * @author Frederic Thevenet
 */
public class DecimationTransform extends BaseTimeSeriesTransform {
    private final int threshold;
    private static final Logger logger = Logger.create(DecimationTransform.class);
    /**
     * Initializes a new instance of the {@link DecimationTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public DecimationTransform(final int threshold) {
        super("DecimationTransform");
        this.threshold = threshold;
    }

    @Override
    public List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            return decimate(data, threshold);
        }
        return data;
    }


    private List<XYChart.Data<ZonedDateTime, Double>> decimate(List<XYChart.Data<ZonedDateTime, Double>> data, int threshold) {
        int dataLength = data.size();
        List<XYChart.Data<ZonedDateTime, Double>> sampled = new ArrayList<>(threshold);
        double every = (double) (dataLength - 2) / (threshold - 2);
        sampled.add(data.get(0)); // Always add the first point
        for (int i = 1; i < threshold - 1; i++) {
            sampled.add(data.get(Math.min(dataLength - 1, (int) Math.round(i * every))));
        }
        sampled.add(data.get(dataLength - 1));
        logger.debug(() -> "Series reduced from " + data.size() + " to " + sampled.size() + " samples.");
        return sampled;
    }
}