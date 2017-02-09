package eu.fthevenet.binjr.sources.jrds.adapters;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class provides an implementation of {@link TimeSeriesBinding} for bindings targeting JRDS.
 *
 * @author Frederic Thevenet
 */
public class JRDSSeriesBinding implements TimeSeriesBinding<Double> {
    private static final Logger logger = LogManager.getLogger(JRDSSeriesBinding.class);
    private final DataAdapter<Double> adapter;
    private final String label;
    private final String path;
    private final Color color;
    private final String legend;
  //  private final int unitBase;
    private final String graphType;


    /**
     * Initializes a new instance of the {@link JRDSSeriesBinding} class.
     *
     * @param label   the name of the data store.
     * @param path    the id for the graph/probe
     * @param adapter the {@link JRDSDataAdapter} for the binding.
     */
    public JRDSSeriesBinding(String label, String path, DataAdapter<Double> adapter) {
        this.adapter = adapter;
        this.label = label;
        this.path = path;
        color = null;
        legend = label;
        graphType = "none";




    }

    public JRDSSeriesBinding(Graphdesc.SeriesDesc graphdesc, String path, DataAdapter<Double> adapter) {
        this.adapter = adapter;
        this.path = path;

        this.label = isNullOrEmpty(graphdesc.name) ?
                (isNullOrEmpty(graphdesc.dsName) ?
                        (isNullOrEmpty(graphdesc.legend) ?
                                "???" : graphdesc.legend) : graphdesc.dsName) : graphdesc.name;


        Color c = null;
        try {
            if (graphdesc.color!=null) {
                c = Color.web(graphdesc.color);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid color string for binding " + this.label);
        }
        this.color = c;

        this.legend = isNullOrEmpty(graphdesc.legend) ?
                (isNullOrEmpty(graphdesc.name) ?
                        (isNullOrEmpty(graphdesc.dsName) ?
                                "???" : graphdesc.dsName) : graphdesc.name) : graphdesc.legend;

        this.graphType = isNullOrEmpty(graphdesc.graphType) ? "none" : graphdesc.graphType;


    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
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
    public Color getColor() {
        return this.color;
    }

    @Override
    public String getLegend() {
        return this.legend;
    }

    @Override
    public String getGraphType() {
        return graphType;
    }

    @Override
    public String toString() {
        return getLegend();
    }
    //endregion
}
