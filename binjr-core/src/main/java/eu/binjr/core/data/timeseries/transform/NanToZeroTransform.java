/*
 *    Copyright 2019-2020 Frederic Thevenet
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class NanToZeroTransform extends BaseTimeSeriesTransform {

    /**
     * Base constructor for {@link BaseTimeSeriesTransform} instances.
     */
    public NanToZeroTransform() {
        super("NaNtoZeroTransform");
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        return data.stream()
                .map(sample -> sample.getYValue().isNaN() ? new XYChart.Data<>(sample.getXValue(), 0.0) : sample)
                .collect(Collectors.toList());
    }

}
