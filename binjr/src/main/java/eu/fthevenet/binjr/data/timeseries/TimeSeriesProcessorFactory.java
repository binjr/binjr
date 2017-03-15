package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

/**
 * A functional interface to be used as a factory for {@link TimeSeriesProcessor} of type {@code T}
 *
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface TimeSeriesProcessorFactory<T extends Number> {
    /**
     *  Initializes a new instance of the {@link TimeSeriesProcessor} class from the provided {@link TimeSeriesBinding}
     * @return a new instance of the {@link TimeSeriesProcessor} class
     */
    TimeSeriesProcessor<T> create();
}
