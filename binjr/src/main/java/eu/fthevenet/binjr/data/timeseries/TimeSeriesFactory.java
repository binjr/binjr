package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

/**
 * A functional interface to be used as a factory for {@link TimeSeries} of type T
 *
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface TimeSeriesFactory<T extends Number> {
    TimeSeries<T> create(TimeSeriesBinding<T> binding);
}
