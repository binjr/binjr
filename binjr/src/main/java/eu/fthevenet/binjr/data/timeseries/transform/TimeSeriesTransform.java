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
public abstract class TimeSeriesTransform {

    private final String name;
    private final Function<Map<String, List<XYChart.Data<ZonedDateTime, Number>>>, Map<String, List<XYChart.Data<ZonedDateTime, Number>>>> transformFunction;


    protected TimeSeriesTransform(String name, Function<Map<String, List<XYChart.Data<ZonedDateTime, Number>>>, Map<String, List<XYChart.Data<ZonedDateTime, Number>>>> transformFunction) {
        this.name = name;
        this.transformFunction = transformFunction;
    }

    public Map<String, List<XYChart.Data<ZonedDateTime, Number>>> transform(Map<String, List<XYChart.Data<ZonedDateTime, Number>>> series){
       return  transform(series, true);
    }

    public Map<String, List<XYChart.Data<ZonedDateTime, Number>>> transform(Map<String, List<XYChart.Data<ZonedDateTime, Number>>> series, boolean enabled){
        return enabled ? transformFunction.apply(series) : series;
    }

    public String getName() {
        return name;
    }
}
