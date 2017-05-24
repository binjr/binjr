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

package eu.fthevenet.util.ui.charts;

import eu.fthevenet.util.logging.Profiler;
import javafx.scene.chart.Axis;
import javafx.scene.chart.StackedAreaChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link StackedAreaChart} that logs the execution time of the {@code layoutPlotChildren} method
 *
 * @author Frederic Thevenet
 */
public class ProfiledStackedAreaChart<X, Y> extends StackedAreaChart<X, Y> {
    private static final Logger logger = LogManager.getLogger(ProfiledStackedAreaChart.class);

    /**
     * Initializes a new instance of the {@link ProfiledStackedAreaChart} class
     *
     * @param xAxis the x axis of the chart
     * @param yAxis the y axis of the chart
     */
    public ProfiledStackedAreaChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    @Override
    protected void layoutPlotChildren() {
        try (Profiler p = Profiler.start("Plotting MyStackedAreaChart " + this.getTitle(), logger::trace)) {
            super.layoutPlotChildren();
        }
    }

}
