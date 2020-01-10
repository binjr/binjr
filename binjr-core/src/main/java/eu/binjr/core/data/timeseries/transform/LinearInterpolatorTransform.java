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
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class LinearInterpolatorTransform extends TimeSeriesTransform {
    private final int threshold;

    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     */
    public LinearInterpolatorTransform(int threshold) {
        super("LinearInterpolatorTransform");
        this.threshold = threshold;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (threshold > 0 && data.size() > threshold) {
            List<XYChart.Data<ZonedDateTime, Double>> reduced = new ArrayList<>(threshold);
            XYChart.Data<ZonedDateTime, Double> start = data.get(0);
            double startMillis = start.getXValue().toInstant().toEpochMilli();
            XYChart.Data<ZonedDateTime, Double> end = data.get(data.size() - 1);
            var li = new LinearInterpolator();
            PolynomialSplineFunction psf = li.interpolate(
                    data.stream().mapToDouble(sample -> ((double) sample.getXValue().toInstant().toEpochMilli())).toArray(),
                    data.stream().mapToDouble(XYChart.Data::getYValue).toArray());
            double stepMillis = (Duration.between(start.getXValue(), end.getXValue()).toMillis()) / (threshold - 2.0);
            reduced.add(start);
            for (int i = 1; i < threshold - 2; i++) {
                reduced.add(new XYChart.Data<>(start.getXValue().plusSeconds(i * (long) (Math.floor(stepMillis) / 1000.0)),
                        psf.value(startMillis + (i * stepMillis))));
            }
            reduced.add(end);
            return reduced;
        }

        return data;
    }

}
