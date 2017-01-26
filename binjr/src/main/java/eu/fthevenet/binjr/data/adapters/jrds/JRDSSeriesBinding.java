package eu.fthevenet.binjr.data.adapters.jrds;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

/**
 * Created by FTT2 on 23/01/2017.
 */
public class JRDSSeriesBinding implements TimeSeriesBinding<Double> {
    private final DataAdapter<Double> adapter;
    private final String label;
    private final String path;

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public DataAdapter<Double> getAdapter() {
        return this.adapter;
    }

    public JRDSSeriesBinding(String label, String path, DataAdapter<Double> adapter){
        this.adapter = adapter;
        this.label = label;
        this.path = path;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
