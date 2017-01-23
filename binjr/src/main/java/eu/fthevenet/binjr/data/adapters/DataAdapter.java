package eu.fthevenet.binjr.data.adapters;

import javafx.scene.control.TreeItem;

import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;

/**
 * Created by FTT2 on 14/10/2016.
 */
public interface DataAdapter extends Serializable {
    TreeItem<TimeSeriesBinding> getTree() throws DataAdapterException;
    long getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out ) throws DataAdapterException;
    long getData(TimeSeriesBinding node, Instant begin, Instant end, OutputStream out ) throws DataAdapterException;
}
