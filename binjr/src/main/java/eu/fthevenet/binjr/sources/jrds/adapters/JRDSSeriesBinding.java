package eu.fthevenet.binjr.sources.jrds.adapters;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;

/**
 * This class provides an implementation of {@link TimeSeriesBinding} for bindings targeting JRDS.
 *
 * @author Frederic Thevenet
 */
public class JRDSSeriesBinding implements TimeSeriesBinding<Double> {
    private final DataAdapter<Double> adapter;
    private final String label;
    private final String path;

    /**
     * Initializes a new instance of the {@link JRDSSeriesBinding} class.
     * @param label the name of the data store.
     * @param path the id for the graph/probe
     * @param adapter the {@link JRDSDataAdapter} for the binding.
     */
    public JRDSSeriesBinding(String label, String path, DataAdapter<Double> adapter){
        this.adapter = adapter;
        this.label = label;
        this.path = path;
    }

    //region [TimeSeriesBinding Members]
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

    @Override
    public String toString() {
        return this.label;
    }
    //endregion
}
