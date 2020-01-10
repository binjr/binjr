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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A time series transform that applies the <a href="https://github.com/sveinn-steinarsson/flot-downsample">Largest-Triangle-Three-Buckets algorithm</a>
 * to reduce the number of discreet data points in a series while keeping a good visual approximation of its appearance when plotted.
 * This implementation should only be used for multiple highly correlated series that are rendered in a stacked area charts.
 *
 * @author Frederic Thevenet
 */
public class StackedLargestTriangleThreeBucketsTransform extends LargestTriangleThreeBucketsTransform {
    private boolean[] keepList;

    /**
     * Initializes a new instance of the {@link StackedLargestTriangleThreeBucketsTransform} class.
     *
     * @param threshold the maximum number of points to keep following the reduction.
     */
    public StackedLargestTriangleThreeBucketsTransform(final int threshold) {
        super("StackedLargestTriangleThreeBucketsTransform", threshold);
    }

    @Override
    protected synchronized List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            if (keepList == null){
                keepList = new boolean[data.size()];
                return applyLTTBReduction(data, threshold);
            }else{
                var filtered = new ArrayList<XYChart.Data<ZonedDateTime, Double>>();
                for (int i = 0; i < data.size(); i++) {
                    if (keepList[i]){
                        filtered.add(data.get(i));
                    }
                }
                return filtered;
            }
        }
        return data;
    }
}