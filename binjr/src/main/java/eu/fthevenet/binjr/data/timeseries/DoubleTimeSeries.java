package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Created by FTT2 on 30/01/2017.
 */
public class DoubleTimeSeries extends TimeSeries<Double> {

    public DoubleTimeSeries(String name) {
        super(name);

     //  this.currentValue = Double.NaN;
    }

    @Override
    public Double getMinValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).min();
        return res.isPresent() ? res.getAsDouble():Double.NaN;
    }

    @Override
    public Double getAverageValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).average();
        return res.isPresent() ? res.getAsDouble():Double.NaN;
    }

    @Override
    public Double getMaxValue() {
        OptionalDouble res = this.data.stream().mapToDouble(XYChart.Data::getYValue).max();
        return res.isPresent() ? res.getAsDouble():Double.NaN;
    }
}
