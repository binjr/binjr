package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import eu.fthevenet.binjr.preferences.GlobalPreferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Created by FTT2 on 24/01/2017.
 */
public class TimeSeriesBuilder {

    private static class TimeSeriesFactoryHolder {
        private final static TimeSeriesBuilder instance = new TimeSeriesBuilder();
    }

    public static TimeSeriesBuilder getInstance() {
        return TimeSeriesFactoryHolder.instance;
    }

    private TimeSeriesBuilder() {

    }

    public <T extends Number> List<TimeSeries<T>> getSeries(List<TimeSeriesBinding<T>> bindings, ZonedDateTime startTime, ZonedDateTime endTime) throws DataAdapterException {
        List<TimeSeries<T>> series = new ArrayList<>();
        // Group all bindings by common adapters
        TimeSeriesTransform<T> reducer = new LargestTriangleThreeBucketsTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        Map<DataAdapter<T>, List<TimeSeriesBinding<T>>> bindingsByAdapters = bindings.stream().collect(groupingBy(TimeSeriesBinding::getAdapter));
        for (Map.Entry<DataAdapter<T>, List<TimeSeriesBinding<T>>> e1 : bindingsByAdapters.entrySet()) {
            DataAdapter<T> adapter = e1.getKey();
            // Group all bindings-by-adapters by path
            Map<String, List<TimeSeriesBinding<T>>> bindingsByPath = e1.getValue().stream().collect(groupingBy(TimeSeriesBinding::getPath));
            for (Map.Entry<String, List<TimeSeriesBinding<T>>> e2 : bindingsByPath.entrySet()) {
                String path = e2.getKey();
                List<String> labels = e2.getValue().stream().map(TimeSeriesBinding::getLabel).collect(toList());
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    adapter.getData(path, startTime.toInstant(), endTime.toInstant(), out);
                    try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
                        Map<String, TimeSeries<T>> m = adapter.getParser().parse(in, labels.toArray(new String[0]));
                        // Applying point reduction
                        m = reducer.transform(m, GlobalPreferences.getInstance().getDownSamplingEnabled());
                        // Adding the new series to the list
                        series.addAll(m.values());
                    }
                } catch (IOException | ParseException e) {
                    throw new DataAdapterException("Error recovering data from source", e);
                }
            }
        }
        return series;
    }
}
