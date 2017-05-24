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

import eu.fthevenet.util.logging.Profiler;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

//FIXME not used at the moment
public class TimeSeriesTransformer<T extends Number> {

    private static final Logger logger = LogManager.getLogger(TimeSeriesTransformer.class);
    private Map<String, List<XYChart.Data<ZonedDateTime, T>>> timeSeries;


    public TimeSeriesTransformer(Map<String, List<XYChart.Data<ZonedDateTime, T>>> timeSeries) {
        this.timeSeries = timeSeries;
    }

    public TimeSeriesTransformer<T> apply(Supplier<Boolean> condition, TimeSeriesTransform<T> seriesTransform, String... seriesNames) {
        return apply(condition.get(), seriesTransform, seriesNames);
    }

    public TimeSeriesTransformer<T> apply(boolean isTransformEnabled, TimeSeriesTransform<T> seriesTransform, String... seriesNames) {
        if (isTransformEnabled) {
            Set<String> nameSet = seriesNames.length == 0 ? timeSeries.keySet() : new HashSet<String>(Arrays.asList(seriesNames));
            Map<String, List<XYChart.Data<ZonedDateTime, T>>> series = timeSeries.entrySet()
                    .stream()
                    .filter(s -> nameSet.contains(s.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (series == null || series.size() != nameSet.size()) {
                throw new IllegalArgumentException("Failed to retrieve all timeSeries with name " + Arrays.toString(seriesNames));
            }
            try (Profiler ignored = Profiler.start("Applying transform" + seriesTransform.getName() + " to series " + Arrays.toString(nameSet.toArray()), logger::trace)) {
                // Map<String, List<XYChart.Data<ZonedDateTime, T>>> a = seriesTransform.apply(series);
                //  timeSeries.putAll(a);
            }
        }
        else {
            logger.debug(() -> "Transform " + seriesTransform.getName() + " on series " + Arrays.toString(seriesNames) + " is disabled.");
        }
        return this;
    }


    public Map<String, XYChart.Series<ZonedDateTime, T>> toSeries() {
        return timeSeries.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    XYChart.Series<ZonedDateTime, T> s = new XYChart.Series<>();
                    s.setName(e.getKey());
                    s.getData().addAll(e.getValue());
                    return s;
                }));
    }

}



