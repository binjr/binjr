package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.parsers.DataParser;
import javafx.scene.control.TreeItem;

import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Created by FTT2 on 14/10/2016.
 */
public interface DataAdapter<T extends Number> extends Serializable {
    TreeItem<TimeSeriesBinding<T>> getTree() throws DataAdapterException;

    long getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out) throws DataAdapterException;

    long getData(String path, Instant begin, Instant end, OutputStream out) throws DataAdapterException;

    String getEncoding();

    ZoneId getTimeZoneId();

    DataParser<T> getParser();
}
