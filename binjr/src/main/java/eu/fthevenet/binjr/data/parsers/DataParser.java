package eu.fthevenet.binjr.data.parsers;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides the means to parse data retreived from a data source via a {@link eu.fthevenet.binjr.data.adapters.DataAdapter}
 *
 * @author Frederic Thevenet
 */
public interface DataParser<T extends Number> {

    /**
     * Parse a stream of data into a map of {@link TimeSeries} instances.
     *
     * @param in          the input stream to parse.
     * @param seriesNames the name of the series to extract from the stream
     * @return a map of {@link TimeSeries} instances.
     * @throws IOException    in the event of an IO error
     * @throws ParseException in the event of an parsing error
     */
  //  Map<TimeSeriesBinding<T>, TimeSeries<T>> parse(InputStream in, TimeSeriesBinding<T>... seriesNames) throws IOException, ParseException;

    Map<TimeSeriesInfo<T>, TimeSeries<T>> parse(InputStream in, List<TimeSeriesInfo<T>> seriesNames) throws IOException, ParseException;
    //Map<TimeSeriesBinding<T>, TimeSeries<T>> parse(InputStream in) throws IOException, ParseException;
}
