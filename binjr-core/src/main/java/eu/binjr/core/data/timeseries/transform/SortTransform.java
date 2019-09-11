/*
 *    Copyright 2019 Frederic Thevenet
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

import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortTransform extends TimeSeriesTransform {

    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     */
    public SortTransform() {
        super("SortTransform");
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(TimeSeriesInfo info, List<XYChart.Data<ZonedDateTime, Double>> data) {
        data.sort(Comparator.comparing(XYChart.Data::getXValue));
        return data;
    }

}
