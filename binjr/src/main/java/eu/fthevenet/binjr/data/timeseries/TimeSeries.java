package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.scene.chart.XYChart;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by FTT2 on 21/12/2016.
 */
public class TimeSeries<T extends Number> implements Serializable {

    public String getName() {
        return name;
    }

    public List<XYChart.Data<ZonedDateTime, T>> getData() {
        return data;
    }

    public XYChart.Series<ZonedDateTime, T> asSeries(){
        XYChart.Series<ZonedDateTime, T> s =  new XYChart.Series<>();
        s.getData().addAll(data);
        return s;
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

    public TimeSeriesBinding getBinding() {
        return binding;
    }

    private final String name;

    public void setData(List<XYChart.Data<ZonedDateTime, T>> data) {
        this.data = data;
    }

    private   List<XYChart.Data<ZonedDateTime, T>> data;
    private  T minValue;
    private  T averageValue;
    private  T maxValue;
    private  TimeSeriesBinding binding;

    public void setMinValue(T minValue) {
        this.minValue = minValue;
    }

    public void setAverageValue(T averageValue) {
        this.averageValue = averageValue;
    }

    public void setMaxValue(T maxValue) {
        this.maxValue = maxValue;
    }

    public TimeSeries(String name) {
        this.data = new ArrayList<>();
        this.name = name;
    }

    public TimeSeries(String name, List<XYChart.Data<ZonedDateTime, T>> data,T minValue, T averageValue, T maxValue, TimeSeriesBinding binding) {
        this.name = name;
        this.data = data;
        this.minValue = minValue;
        this.averageValue = averageValue;
        this.maxValue = maxValue;
        this.binding = binding;
    }


}
