package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by FTT2 on 30/01/2017.
 */
@FunctionalInterface
public interface TimeSeriesFactory<T extends Number>   {
    TimeSeries<T> create(String name);
  //  TimeSeries<T> create(String name, List<XYChart.Data<ZonedDateTime, T>> data, TimeSeriesBinding<T> binding);
}
