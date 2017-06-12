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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A simple {@link TimeSeriesTransform} that operates a linear decimation on the provided series.
 *
 * @author Frederic Thevenet
 */
public class DecimationTransform<T extends Number> extends TimeSeriesTransform<T> {
    private final int threshold;

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
    public Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> apply(Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> m) {
        return m.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> {
                    o.getValue().setData(decimate(o.getValue().getData(), threshold));
                    return o.getValue();
                }));
    }

    private List<XYChart.Data<ZonedDateTime, T>> decimate(List<XYChart.Data<ZonedDateTime, T>> data, int threshold) {
        int dataLength = data.size();
        if (threshold >= dataLength || threshold == 0) {
            return data;
        }
        List<XYChart.Data<ZonedDateTime, T>> sampled = new ArrayList<>(threshold);
        double every = (double) (dataLength - 2) / (threshold - 2);
        sampled.add(data.get(0)); // Always add the first point
        for (int i = 1; i < threshold - 1; i++) {
            sampled.add(data.get(Math.min(dataLength - 1, (int) Math.round(i * every))));
        }
        sampled.add(data.get(dataLength - 1));
        return sampled;
    }
}