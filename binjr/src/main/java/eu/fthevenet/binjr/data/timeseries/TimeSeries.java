package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.chart.XYChart;

import java.io.*;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * The base class for time series classes, which holds raw data points and provides access to summary properties.
 *
 * @author Frederic Thevenet
 */
public abstract class TimeSeries<T extends Number> implements Serializable {
    protected List<XYChart.Data<ZonedDateTime, T>> data;
    protected final String name;

    /**
     * Initializes a new instance of the {@link TimeSeries} class with the provided name.
     *
     * @param name the name of the {@link TimeSeries}
     */
    public TimeSeries(String name) {
        this.data = new ArrayList<>();
        this.name = name;
    }

    /**
     * Gets the name of the {@link TimeSeries}
     *
     * @return the name of the {@link TimeSeries}
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the minimum value for the Y coordinates of the {@link TimeSeries}
     *
     * @return the minimum value for the Y coordinates of the {@link TimeSeries}
     */
    public abstract T getMinValue();

    /**
     * Gets the average for all Y coordinates of the {@link TimeSeries}
     *
     * @return the average for all Y coordinates of the {@link TimeSeries}
     */
    public abstract T getAverageValue();

    /**
     * Gets the maximum value for the Y coordinates of the {@link TimeSeries}
     *
     * @return  the maximum value for the Y coordinates of the {@link TimeSeries}
     */
    public abstract T getMaxValue();

    /**
     * Sets the data for the {@link TimeSeries}
     *
     * @param data the list of {@link XYChart.Data} points to use as the {@link TimeSeries}' data.
     */
    public void setData(List<XYChart.Data<ZonedDateTime, T>> data) {
        this.data = data;
    }

    /**
     * Gets the data of the {@link TimeSeries}
     *
     * @return the data of the {@link TimeSeries}
     */
    public List<XYChart.Data<ZonedDateTime, T>> getData() {
        return data;
    }

    /**
     * Returns the current TimeSeries' data as an instance of {@link XYChart.Series} so that it can be displayed in a chart.
     *
     * @return the current TimeSeries' data as an instance of {@link XYChart.Series}
     */
    public XYChart.Series<ZonedDateTime, T> asSeries() {
        XYChart.Series<ZonedDateTime, T> s = new XYChart.Series<>();
        s.getData().addAll(data);
        return s;
    }

    /**
     * A utility method that generates a list of {@link TimeSeries} instances from a corresponding list of a {@link TimeSeriesBinding}, for a given time interval.
     *
     * @param bindings  The {@link TimeSeriesBinding} that describes how the {@link TimeSeries} should be generated
     * @param startTime The start of the time interval for which to produce the series
     * @param endTime   The end of the time interval for which to produce the series
     * @param <T>       The type for the Y values of the series.
     * @return a list of {@link TimeSeries} instances generated from the provided bindings
     * @throws DataAdapterException In case an error occures while retreiving the data from the {@link DataAdapter} or parsing it in a {@link eu.fthevenet.binjr.data.parsers.DataParser}
     */
    public static <T extends Number> List<TimeSeries<T>> fromBinding(List<TimeSeriesBinding<T>> bindings, ZonedDateTime startTime, ZonedDateTime endTime) throws DataAdapterException {
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
