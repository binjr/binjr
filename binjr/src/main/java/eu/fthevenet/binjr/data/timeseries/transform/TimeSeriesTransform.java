package eu.fthevenet.binjr.data.timeseries.transform;

import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by FTT2 on 02/12/2016.
 */
public abstract class TimeSeriesTransform<T extends Number> {

    private final String name;

    public TimeSeriesTransform(String name) {
        this.name = name;

    }

    public abstract Map<String, List<XYChart.Data<ZonedDateTime, T>>> transform(Map<String, List<XYChart.Data<ZonedDateTime, T>>> series);

    public  Map<String, List<XYChart.Data<ZonedDateTime, T>>> transform(Map<String, List<XYChart.Data<ZonedDateTime, T>>> series, boolean enabled){
        return enabled ? transform(series) : series;
    }

    public String getName() {
        return name;
    }
}
