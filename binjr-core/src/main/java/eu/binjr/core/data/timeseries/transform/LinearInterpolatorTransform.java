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
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.time.ZonedDateTime;
import java.util.List;

public class LinearInterpolatorTransform   extends TimeSeriesTransform{


    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     *
     * @param name the name of the transform function
     */
    public LinearInterpolatorTransform(String name) {
        super(name);
    }

    public double[] linearInterp(double[] x, double[] y, double[] xi) {
        LinearInterpolator li = new LinearInterpolator(); // or other interpolator
        PolynomialSplineFunction psf = li.interpolate(x, y);

        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            yi[i] = psf.value(xi[i]);
        }
        return yi;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        return null;
    }
}
