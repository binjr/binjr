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
