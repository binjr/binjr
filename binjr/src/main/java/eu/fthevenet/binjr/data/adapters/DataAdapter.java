package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.parsers.DataParser;
import javafx.scene.control.TreeItem;

import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Provides the means to access a data source to retrieve raw time series data, as well as properties needed to present them in the view.
 *
 * @author Frederic Thevenet
 */
public interface DataAdapter<T extends Number> extends Serializable {
    /**
     * Return a hierarchical view of all the individual bindings exposed by the underlying source.
     * @return a hierarchical view of all the individual bindings exposed by the underlying source.
     * @throws DataAdapterException if an error occurs while retrieving bindings.
     */
    TreeItem<TimeSeriesBinding<T>> getBindingTree() throws DataAdapterException;

    /**
     * Gets raw data from the source as an output stream, for the time interval specified.
     * @param path the path of the data in the source
     * @param begin the start of the time interval.
     * @param end the end of the time interval.
     * @param out the output stream in which to return data.
     * @return the size of the stream (if applicable).
     * @throws DataAdapterException  if an error occurs while retrieving data from the source.
     */
    long getData(String path, Instant begin, Instant end, OutputStream out) throws DataAdapterException;

    /**
     * Gets the encoding used to decode textual data sent by the source.
     * @return the encoding used to decode textual data sent by the source.
     */
    String getEncoding();

    /**
     * Gets the id of the time zone used to record dates in the source.
     * @return the id of the time zone used to record dates in the source.
     */
    ZoneId getTimeZoneId();

    /**
     * Gets the {@link DataParser} used to produce {@link eu.fthevenet.binjr.data.timeseries.TimeSeries} from the source.
     * @return the {@link DataParser} used to produce {@link eu.fthevenet.binjr.data.timeseries.TimeSeries} from the source.
     */
    DataParser<T> getParser();

    /**
     * Gets the name of the source.
     * @return the name of the source.
     */
    String getSourceName();
}
