package eu.fthevenet.binjr.data.timeseries;

import javafx.scene.chart.XYChart;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Created by FTT2 on 21/12/2016.
 */
public class TimeSeries<T extends Number> implements Serializable {

    public String getName() {
        return name;
    }

    public XYChart.Series<ZonedDateTime, T> getData() {
        return data;
    }

    public T getMinValue() {
        return minValue;
    }

    public T getAverageValue() {
        return averageValue;
    }

    public T getMaxValue() {
        return maxValue;
    }

    private final String name;
    private final  XYChart.Series<ZonedDateTime, T> data;
    private final T minValue;
    private final T averageValue;
    private final T maxValue;


    public TimeSeries(String name, XYChart.Series<ZonedDateTime, T> data, T minValue, T averageValue, T maxValue) {
        this.name = name;
        this.data = data;
        this.minValue = minValue;
        this.averageValue = averageValue;
        this.maxValue = maxValue;
    }
}
