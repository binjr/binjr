package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesBuilder;
import javafx.scene.control.TreeItem;

import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Created by FTT2 on 14/10/2016.
 */
public interface DataAdapter extends Serializable {
    TreeItem<TimeSeriesBinding> getTree() throws DataAdapterException;

    long getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out) throws DataAdapterException;

    long getData(TimeSeriesBinding node, Instant begin, Instant end, OutputStream out) throws DataAdapterException;

    String getEncoding();

    ZoneId getTimeZoneId();

    <T extends Number> TimeSeriesBuilder<T> getTimesSeriesBuilder(List<TimeSeriesBinding> bindings);
}
