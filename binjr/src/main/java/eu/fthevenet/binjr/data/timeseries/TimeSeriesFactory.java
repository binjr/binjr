package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

/**
 * A functional interface to be used as a factory for {@link TimeSeries} of type {@code T}
 *
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface TimeSeriesFactory<T extends Number> {
    /**
     *  Initializes a new instance of the {@link TimeSeries} class from the provided {@link TimeSeriesBinding}
     * @param binding the binding to create the {@link TimeSeries}  from
     * @return a new instance of the {@link TimeSeries} class
     */
    TimeSeries<T> create(TimeSeriesBinding<T> binding);
}
