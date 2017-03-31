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
public class JrdsSeriesBindingFactory {
    private static final Logger logger = LogManager.getLogger(JrdsSeriesBindingFactory.class);
    private static final UnitPrefixes DEFAULT_PREFIX = UnitPrefixes.BINARY;

    /**
     * Initializes a new instance of the {@link JrdsSeriesBindingFactory} class
     */
    public JrdsSeriesBindingFactory() {
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class.
     *
     * @param label   the name of the data store.
     * @param path    the id for the graph/probe
     * @param adapter the {@link JrdsDataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public TimeSeriesBinding<Double> of(String label, String path, DataAdapter<Double> adapter) {
        return new TimeSeriesBinding<>(
                label,
                path,
                null,
                label, DEFAULT_PREFIX,
                ChartType.STACKED,
                "-",
                adapter);
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class with the following parameters
     *
     * @param legend    the legend for the timeseries
     * @param graphdesc the graph description from JRDS
     * @param path      the id of the JRDS graph
     * @param adapter   the {@link JrdsDataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public TimeSeriesBinding<Double> of(String legend, Graphdesc graphdesc, String path, DataAdapter<Double> adapter) {
        final String label;
        final UnitPrefixes prefix;
        final ChartType graphType;
        final String unitName;
        label = isNullOrEmpty(graphdesc.name) ?
                (isNullOrEmpty(graphdesc.graphName) ?
                        "???" : graphdesc.graphName) : graphdesc.name;

        graphType = ChartType.STACKED;
        prefix = findPrefix(graphdesc);
        unitName = graphdesc.verticalLabel;
        return new TimeSeriesBinding<>(label, path, null, legend, prefix, graphType, unitName, adapter);
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class with the following parameters
     * @param graphdesc the graph description from JRDS
     * @param idx the index of the series in the graphdesc
     * @param path the id of the JRDS graph
     * @param adapter the {@link JrdsDataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public TimeSeriesBinding<Double> of(Graphdesc graphdesc, int idx, String path, DataAdapter<Double> adapter) {
        final String label;
        final Color color;
        final String legend;
        final UnitPrefixes prefix;
        final ChartType graphType;
        final String unitName;

        Graphdesc.SeriesDesc desc = graphdesc.seriesDescList.get(idx);
        label = isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name;
        Color c = null;
        try {
            if (!isNullOrEmpty(desc.color)) {
                c = Color.web(desc.color);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid color string for binding " + label);
        }
        color = c;
        legend = isNullOrEmpty(desc.legend) ?
                (isNullOrEmpty(desc.name) ?
                        (isNullOrEmpty(desc.dsName) ?
                                "???" : desc.dsName) : desc.name) : desc.legend;
        switch (desc.graphType.toLowerCase()) {
            case "area":
                graphType = ChartType.AREA;
                break;

            case "stacked":
                graphType = ChartType.STACKED;
                break;

            case "line":
                graphType = ChartType.LINE;
                break;

            case "none":
            default:
                graphType = ChartType.STACKED;
                break;
        }
        prefix = findPrefix(graphdesc);
        unitName = graphdesc.verticalLabel;
        return new TimeSeriesBinding<>(label, path, color, legend, prefix, graphType, unitName, adapter);
    }

    private UnitPrefixes findPrefix(Graphdesc graphdesc) {
        if (graphdesc.unit != null && graphdesc.unit.size() > 0) {
            if (graphdesc.unit.get(0) instanceof Graphdesc.JrdsMetricUnitType) {
                return UnitPrefixes.METRIC;
            }
            if (graphdesc.unit.get(0) instanceof Graphdesc.JrdsBinaryUnitType) {
                return UnitPrefixes.BINARY;
            }
        }
        return DEFAULT_PREFIX;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
