package eu.fthevenet.binjr.sources.jrds.adapters;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class provides an implementation of {@link TimeSeriesBinding} for bindings targeting JRDS.
 *
 * @author Frederic Thevenet
 */
public class JrdsSeriesBinding implements TimeSeriesBinding<Double> {
    private static final Logger logger = LogManager.getLogger(JrdsSeriesBinding.class);
    private static final UnitPrefixes DEFAULT_BASE = UnitPrefixes.METRIC;
    private final DataAdapter<Double> adapter;
    private final String label;
    private final String path;
    private final Color color;
    private final String legend;
    private final UnitPrefixes prefix;
    private final ChartType graphType;
    private final String unitName;

    /**
     * Initializes a new instance of the {@link JrdsSeriesBinding} class.
     *
     * @param label   the name of the data store.
     * @param path    the id for the graph/probe
     * @param adapter the {@link JrdsDataAdapter} for the binding.
     */
    public JrdsSeriesBinding(String label, String path, DataAdapter<Double> adapter) {
        this.adapter = adapter;
        this.label = label;
        this.path = path;
        color = null;
        legend = label;
        graphType = ChartType.STACKED;
        prefix = DEFAULT_BASE;
        unitName = "-";
    }

    public JrdsSeriesBinding(String legend, Graphdesc graphdesc, String path, DataAdapter<Double> adapter) {
        this.path = path;
        this.adapter = adapter;
        this.color = null;
        this.label = isNullOrEmpty(graphdesc.name) ?
                (isNullOrEmpty(graphdesc.graphName) ?
                        "???" : graphdesc.graphName) : graphdesc.name;
        this.legend = legend;
        this.graphType = ChartType.STACKED;
        if (graphdesc.unit != null &&
                graphdesc.unit.size() > 0 &&
                graphdesc.unit.get(0) instanceof Graphdesc.JrdsMetricUnitType) {
            this.prefix = UnitPrefixes.METRIC;
        }
        else{
            this.prefix = UnitPrefixes.BINARY;
        }
        this.unitName = graphdesc.verticalLabel;
    }

    public JrdsSeriesBinding(Graphdesc graphdesc, int idx, String path, DataAdapter<Double> adapter) {
        this.adapter = adapter;
        this.path = path;

        Graphdesc.SeriesDesc desc = graphdesc.seriesDescList.get(idx);

        this.label = isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name;

        Color c = null;
        try {
            if (!isNullOrEmpty(desc.color)) {
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

        switch (desc.graphType.toLowerCase()) {
            case "area":
                this.graphType = ChartType.AREA;
                break;

            case "stacked":
                this.graphType = ChartType.STACKED;
                break;
            case "line":
                this.graphType = ChartType.LINE;
                break;

            case "none":
            default:
                this.graphType = ChartType.STACKED;
                break;
        }
        if (graphdesc.unit != null &&
                graphdesc.unit.size() > 0 &&
                graphdesc.unit.get(0) instanceof Graphdesc.JrdsBinaryUnitType) {
            this.prefix = UnitPrefixes.BINARY;
        }
        else{
            this.prefix = UnitPrefixes.METRIC;
        }
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
    public ChartType getGraphType() {
        return graphType;
    }

    @Override
    public String getUnitName() {
        return unitName;
    }

    @Override
    public UnitPrefixes getUnitPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return getLegend();
    }


    //endregion
}
