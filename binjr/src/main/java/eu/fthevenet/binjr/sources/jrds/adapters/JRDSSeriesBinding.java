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
    private static final int DEFAULT_BASE = 10;
    private final DataAdapter<Double> adapter;
    private final String label;
    private final String path;
    private final Color color;
    private final String legend;
    private final int unitBase;
    private final String graphType;
    private final String unitName;


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
        unitBase = 10;
        unitName = "%";
    }

    public JRDSSeriesBinding(Graphdesc graphdesc, int idx, String path, DataAdapter<Double> adapter) {
        this.adapter = adapter;
        this.path = path;
        Graphdesc.SeriesDesc desc = graphdesc.seriesDescList.get(idx);

        this.label = isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name;

        Color c = null;
        try {
            if (desc.color != null) {
                c = Color.web(desc.color);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid color string for binding " + this.label);
        }
        this.color = c;


        this.legend = isNullOrEmpty(desc.legend) ?
                (isNullOrEmpty(desc.name) ?
                        (isNullOrEmpty(desc.dsName) ?
                                "???" : desc.dsName) : desc.name) : desc.legend;

        this.graphType = isNullOrEmpty(desc.graphType) ? "none" : desc.graphType;

        this.unitBase = "SI".equals(graphdesc.unit) ? 10 : ("binary".equals(graphdesc.unit) ? 2 : DEFAULT_BASE);
        this.unitName = graphdesc.verticalLabel;
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
    public String getUnitName() {
        return unitName;
    }

    @Override
    public int getUnitBase() {
        return unitBase;
    }

    @Override
    public String toString() {
        return getLegend();
    }
    //endregion
}
