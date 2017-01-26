package eu.fthevenet.binjr.data.parsers;

import eu.fthevenet.binjr.data.timeseries.TimeSeries;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;

/**
 * Created by FTT2 on 26/01/2017.
 */
public interface DataParser<T extends Number> {
    Map<String, TimeSeries<T>> parse(InputStream in, String... seriesNames) throws IOException, ParseException;
}
