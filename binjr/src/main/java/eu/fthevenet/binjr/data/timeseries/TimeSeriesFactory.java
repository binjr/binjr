package eu.fthevenet.binjr.data.timeseries;

/**
 * A functional interface to be used as a factory for {@link TimeSeries} of type T
 *
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface TimeSeriesFactory<T extends Number> {
    TimeSeries<T> create(String name);
}
