package eu.fthevenet.binjr.data.timeseries;

import javafx.scene.chart.XYChart;

import java.util.OptionalDouble;

/**
 * This class provides a full implementation of a {@link TimeSeriesProcessor} of {@link Double} values.
 *
 * @author Frederic Thevenet
 */
public class DoubleTimeSeriesProcessor extends TimeSeriesProcessor<Double> {

    /**
     * Initializes a new instance of the {@link DoubleTimeSeriesProcessor} class with the provided binding.
     *
     */
    public DoubleTimeSeriesProcessor() {
        super();
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
