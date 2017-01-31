package eu.fthevenet.binjr.data.timeseries;

import javafx.scene.chart.XYChart;

import java.util.OptionalDouble;

/**
 * This class provides a full implementation of a {@link TimeSeries} of {@link Double} values.
 *
 * @author Frederic Thevenet
 */
public class DoubleTimeSeries extends TimeSeries<Double> {

    /**
     * Initializes a new instance of the {@link DoubleTimeSeries} class with the provided name.
     * @param name the name of the {@link DoubleTimeSeries}
     */
    public DoubleTimeSeries(String name) {
        super(name);
    }

    @Override
    public Double getMinValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).min();
        return res.isPresent() ? res.getAsDouble() : Double.NaN;
    }

    @Override
    public Double getAverageValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).average();
        return res.isPresent() ? res.getAsDouble() : Double.NaN;
    }

    @Override
    public Double getMaxValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).max();
        return res.isPresent() ? res.getAsDouble() : Double.NaN;
    }
}
