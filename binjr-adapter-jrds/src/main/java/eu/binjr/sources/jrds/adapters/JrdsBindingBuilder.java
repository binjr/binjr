/*
 *    Copyright 2020 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.jrds.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

/**
 * Factory used to build instances of {@link eu.binjr.core.data.adapters.SourceBinding} from values
 * configured by the setters, for use with {@link JrdsDataAdapter}
 */
public class JrdsBindingBuilder extends TimeSeriesBinding.Builder {
    private static final Logger logger = Logger.create(JrdsBindingBuilder.class);

    @Override
    protected JrdsBindingBuilder self() {
        return this;
    }

    /**
     * Sets the builder with all the parameters contained in a {@link Graphdesc} instance.
     *
     * @param graphdesc the graphdesc
     * @return This builder.
     */
    public JrdsBindingBuilder withGraphDesc(Graphdesc graphdesc) {
        withLabel(isNullOrEmpty(graphdesc.name) ?
                (isNullOrEmpty(graphdesc.graphName) ?
                        "???" : graphdesc.graphName) : graphdesc.name);
        withGraphType(getChartType(graphdesc));
        withPrefix(findPrefix(graphdesc));
        withUnitName(graphdesc.verticalLabel);
        return self();
    }

    /**
     * Sets the builder with all the parameters contained in the {@link eu.binjr.sources.jrds.adapters.Graphdesc.SeriesDesc}
     * at the provided index
     *
     * @param graphdesc the graphdesc
     * @param idx       the index
     * @return This builder.
     */
    public JrdsBindingBuilder withGraphDesc(Graphdesc graphdesc, int idx) {
        Graphdesc.SeriesDesc desc = graphdesc.seriesDescList.get(idx);
        withLabel(isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name);
        Color c = null;
        try {
            if (!isNullOrEmpty(desc.color)) {
                c = Color.web(desc.color);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid color string for binding " + this);
        }
        withColor(c);
        withLegend(isNullOrEmpty(desc.legend) ?
                (isNullOrEmpty(desc.name) ?
                        (isNullOrEmpty(desc.dsName) ?
                                "???" : desc.dsName) : desc.name) : desc.legend);
        withGraphType(getChartType(desc));
        withPrefix(findPrefix(graphdesc));
        withUnitName(graphdesc.verticalLabel);
        return self();
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
        return UnitPrefixes.UNDEFINED;
    }

    private ChartType getChartType(Graphdesc graphdesc) {
        return graphdesc.seriesDescList.stream()
                .filter(desc -> !desc.graphType.equalsIgnoreCase("none") && !desc.graphType.equalsIgnoreCase("comment"))
                .reduce((last, n) -> n)
                .map(this::getChartType)
                .orElse(ChartType.UNDEFINED);
    }

    private ChartType getChartType(Graphdesc.SeriesDesc desc) {
        return switch (desc.graphType.toLowerCase()) {
            case "area" -> ChartType.AREA;
            case "line" -> ChartType.LINE;
            case "stacked" -> ChartType.STACKED;
            default -> ChartType.UNDEFINED;
        };
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
