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
public abstract class TimeSeries<T extends Number> implements Serializable {
    protected  List<XYChart.Data<ZonedDateTime, T>> data;
    protected final String name;

    public String getName() {
        return name;
    }
    public List<XYChart.Data<ZonedDateTime, T>> getData() {
        return data;
    }
    public abstract T getMinValue();
    public abstract T getAverageValue();
    public abstract T getMaxValue();

    public void setData(List<XYChart.Data<ZonedDateTime, T>> data) {
        this.data = data;
    }

    public TimeSeries(String name) {
        this.data = new ArrayList<>();
        this.name = name;
    }

    public XYChart.Series<ZonedDateTime, T> asSeries(){
        XYChart.Series<ZonedDateTime, T> s =  new XYChart.Series<>();
        s.getData().addAll(data);
        return s;
    }
}
