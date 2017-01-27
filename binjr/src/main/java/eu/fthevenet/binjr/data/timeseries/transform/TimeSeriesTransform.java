package eu.fthevenet.binjr.data.timeseries.transform;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by FTT2 on 02/12/2016.
 */
public abstract class TimeSeriesTransform<T extends Number> {
    private static final Logger logger = LogManager.getLogger(TimeSeriesTransform.class);
    private final String name;

    public TimeSeriesTransform(String name) {
        this.name = name;

    }

    protected abstract Map<String, TimeSeries<T>> apply(Map<String, TimeSeries<T>> series);

    //  public abstract TimeSeries<T> apply(TimeSeries<T> series);

    public Map<String, TimeSeries<T>> transform(Map<String, TimeSeries<T>> series, boolean enabled) {
        String names = Arrays.toString(series.keySet().toArray());
        if (enabled) {
            try (Profiler ignored = Profiler.start("Applying transform" + getName() + " to series " + names, logger::trace)) {
                series = apply(series);
            }
        }
        else {
            logger.debug(() -> "Transform " + getName() + " on series " + names + " is disabled.");
        }
        return series;
    }

    public String getName() {
        return name;
    }
}
