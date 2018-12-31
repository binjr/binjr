/*
 *    Copyright 2017-2018 Frederic Thevenet
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

import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A simple {@link TimeSeriesTransform} that operates a linear decimation on the provided series.
 *
 * @author Frederic Thevenet
 */
public class DecimationTransform<T> extends TimeSeriesTransform<T> {
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
                    if (threshold > 0 && o.getValue().size() > threshold) {
                        o.getValue().setData(decimate(o.getValue(), threshold));
                    }
                    return o.getValue();
                }));
    }

    private Collection<XYChart.Data<ZonedDateTime, T>> decimate(TimeSeriesProcessor<T> data, int threshold) {
        int dataLength = data.size();
        List<XYChart.Data<ZonedDateTime, T>> sampled = new ArrayList<>(threshold);
        double every = (double) (dataLength - 2) / (threshold - 2);
        sampled.add(data.getSample(0)); // Always add the first point
        for (int i = 1; i < threshold - 1; i++) {
            sampled.add(data.getSample(Math.min(dataLength - 1, (int) Math.round(i * every))));
        }
        sampled.add(data.getSample(dataLength - 1));
        return sampled;
    }
}