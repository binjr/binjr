package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.controls.ColorUtils;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

/**
 * The base class for time series classes, which holds raw data points and provides access to summary properties.
 *
 * @author Frederic Thevenet
 */
public abstract class TimeSeries<T extends Number> implements Serializable, Comparable<TimeSeries<T>> {
    private static final Logger logger = LogManager.getLogger(TimeSeries.class);
    protected List<XYChart.Data<ZonedDateTime, T>> data;
    protected final TimeSeriesBinding<T> binding;
    protected final String name;
    protected Property<Color> displayColor;


    /**
     * Initializes a new instance of the {@link TimeSeries} class with the provided {@link TimeSeriesBinding}.
     *
     * @param binding the binding of the {@link TimeSeries}
     */
    public TimeSeries(TimeSeriesBinding<T> binding) {
        this.binding = binding;
        this.data = new ArrayList<>();
        this.name = binding.getLegend();
        this.displayColor = new SimpleObjectProperty<>(binding.getColor());
    }

    /**
     * Gets the {@link TimeSeriesBinding} to which the series is bound
     *
     * @return the {@link TimeSeriesBinding} to which the series is bound
     */
    public TimeSeriesBinding<T> getBinding() {
        return binding;
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
     * @return the maximum value for the Y coordinates of the {@link TimeSeries}
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


    public Color getDisplayColor() {
        return displayColor.getValue();
    }

    public Property<Color> displayColorProperty() {
        return displayColor;
    }

    public void setDisplayColor(Color displayColor) {
        this.displayColor.setValue(displayColor);
    }

    @Override
    public int compareTo(TimeSeries<T> o){
        return this.getBinding().compareTo(o.getBinding());
    }

    /**
     * Returns the current TimeSeries' data as an instance of {@link XYChart.Series} so that it can be displayed in a chart.
     *
     * @return the current TimeSeries' data as an instance of {@link XYChart.Series}
     */
    public XYChart.Series<ZonedDateTime, T> asSeries() {
        XYChart.Series<ZonedDateTime, T> s = new XYChart.Series<>();
        s.getData().addAll(data);

        s.nodeProperty().addListener((node, oldNode, newNode) -> {
            if (newNode != null) {
                logger.trace(() -> "Setting color of series " + getBinding().getLabel() + " to " + getBinding().getColor());
                //FIXME Seriously hackish code ahead!!!
                if (GlobalPreferences.getInstance().isUseSourceColors()) {
                    ((Group) newNode).getChildren().get(0).setStyle(" -fx-fill : " + ColorUtils.toHex(getDisplayColor(), 0.2) + ";");
                    ((Group) newNode).getChildren().get(1).setStyle(" -fx-stroke : " + ColorUtils.toHex(getDisplayColor()) + ";");
                }
                else{
                    ((Shape) ((Group) newNode).getChildren().get(1)).strokeProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            setDisplayColor((Color) newValue);
                        }
                    });
                }
            }
        });
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
     * @throws DataAdapterException In case an error occurs while retrieving the data from the {@link DataAdapter} or parsing it in a {@link eu.fthevenet.binjr.data.parsers.DataParser}
     */
    public static <T extends Number> SortedSet<TimeSeries<T>> fromBinding(Collection<TimeSeriesBinding<T>> bindings, ZonedDateTime startTime, ZonedDateTime endTime) throws DataAdapterException {
        SortedSet<TimeSeries<T>> series = new TreeSet<>();
        // Group all bindings by common adapters
        TimeSeriesTransform<T> reducer = new LargestTriangleThreeBucketsTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        Map<DataAdapter<T>, List<TimeSeriesBinding<T>>> bindingsByAdapters = bindings.stream().collect(groupingBy(TimeSeriesBinding::getAdapter));
        for (Map.Entry<DataAdapter<T>, List<TimeSeriesBinding<T>>> byAdapterEntry : bindingsByAdapters.entrySet()) {
            DataAdapter<T> adapter = byAdapterEntry.getKey();
            // Group all bindings-by-adapters by path
            Map<String, List<TimeSeriesBinding<T>>> bindingsByPath = byAdapterEntry.getValue().stream().collect(groupingBy(TimeSeriesBinding::getPath));
            for (Map.Entry<String, List<TimeSeriesBinding<T>>> byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    adapter.getData(path, startTime.toInstant(), endTime.toInstant(), out);
                    try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
                        Map<TimeSeriesBinding<T>, TimeSeries<T>> m = adapter.getParser().parse(in, byPathEntry.getValue());
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
