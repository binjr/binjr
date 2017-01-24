package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

import java.sql.Time;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by FTT2 on 24/01/2017.
 */
public class TimeSeriesFactory {

    private static class TimeSeriesFactoryHolder {
        private final static TimeSeriesFactory instance = new TimeSeriesFactory();
    }

    public static TimeSeriesFactory getInstance() {
        return TimeSeriesFactoryHolder.instance;
    }

    private TimeSeriesFactory(){

    }

    public <T extends Number> List<TimeSeries<T>>  getSeries(List<TimeSeriesBinding> bindings, ZonedDateTime startTime, ZonedDateTime endTime){
        return null;
    }
}
