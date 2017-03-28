package eu.fthevenet.util.ui.charts;

import eu.fthevenet.util.logging.Profiler;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.StackedAreaChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by FTT2 on 27/03/2017.
 */
public class ProfiledStackedAreaChart<X, Y> extends StackedAreaChart<X, Y> {
    private static final Logger logger = LogManager.getLogger(ProfiledStackedAreaChart.class);

    public ProfiledStackedAreaChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    public ProfiledStackedAreaChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }

    @Override
    protected void layoutPlotChildren() {
        try (Profiler p = Profiler.start("Plotting MyStackedAreaChart " + this.getTitle(), logger::trace)) {
            super.layoutPlotChildren();
        }
    }

}
