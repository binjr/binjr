package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransformer;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.chart.XYChart;

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
public class TimeSeriesFactory {

    private static class TimeSeriesFactoryHolder {
        private final static TimeSeriesFactory instance = new TimeSeriesFactory();
    }

    public static TimeSeriesFactory getInstance() {
        return TimeSeriesFactoryHolder.instance;
    }

    private TimeSeriesFactory() {

    }

    public <T extends Number> List<TimeSeries<T>> getSeries(List<TimeSeriesBinding<T>> bindings, ZonedDateTime startTime, ZonedDateTime endTime) throws DataAdapterException {
        List<TimeSeries<T>> series = new ArrayList<>();
        // Group all bindings by common adapters
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
                    InputStream in = new ByteArrayInputStream(out.toByteArray());
                    Map<String, List<XYChart.Data<ZonedDateTime, T>>> m = adapter.getParser().parse(in, labels.toArray(new String[0]));
                    for (Map.Entry<String, List<XYChart.Data<ZonedDateTime, T>>> e3 : m.entrySet()){

                    }
                    TimeSeriesTransformer<T> transformer = new TimeSeriesTransformer<T>(m);
                    Map<String, XYChart.Series<ZonedDateTime, T>> m2 = transformer.transform(
                            GlobalPreferences.getInstance().getDownSamplingEnabled(),
                            new LargestTriangleThreeBucketsTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold())
                    ).toSeries();

//                    m2.entrySet().forEach(d -> {
//                        series.add(new TimeSeries<T>(
//                                d.getKey(),
//                                d.getValue(),
//                                0, 0, 0
//                        ))
//                    });


                } catch (IOException | ParseException e) {
                    throw new DataAdapterException("Error recovering data from source", e);
                }
            }
            //   e.getKey().getData(e.getValue(), startTime.toInstant(), endTime.toInstant());
        }
//
//        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            dp.getData(target, probe, begin, end, out);
//            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(currentZoneId);
//            TimeSeriesTransformer<Double> timeSeriesBuilder = new TimeSeriesTransformer<>(
//                    s -> {
//                        Double val = Double.parseDouble(s);
//                        return val.isNaN() ? 0 : val;
//                    },
//                    s -> ZonedDateTime.parse(s, formatter));
//            InputStream in = new ByteArrayInputStream(out.toByteArray());
//            return timeSeriesBuilder.fromCSV(in)
//                    .transform(globalPrefs.getDownSamplingEnabled(),
//                            new LargestTriangleThreeBucketsTransform<>(globalPrefs.getDownSamplingThreshold()))
//                    .toSeries();
//        } catch (DataAdapterException e) {
//            throw new IOException(String.format("Failed to retrieve data from JRDS for %s %s %s %s", target, probe, begin.toString(), end.toString()), e);
//        }
    }
}
