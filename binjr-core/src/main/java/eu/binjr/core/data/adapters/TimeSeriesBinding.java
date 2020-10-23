/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.core.data.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a binding between a time series and the {@link SerializedDataAdapter} used to produce it.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class TimeSeriesBinding extends SourceBinding<Double> {
    private static final Logger logger = Logger.create(TimeSeriesBinding.class);

    @XmlAttribute
    private final UnitPrefixes prefix;
    @XmlAttribute
    private final ChartType graphType;
    @XmlAttribute
    private final String unitName;

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class.
     */
    public TimeSeriesBinding() {
        super();
        this.prefix = UnitPrefixes.BINARY;
        this.graphType = ChartType.STACKED;
        this.unitName = "";

    }

    @Override
    public Class<? extends Worksheet<Double>> getWorksheetClass() {
        return XYChartsWorksheet.class;
    }

    /**
     * Creates a clone of the provided binding, save for its {@link SerializedDataAdapter} instance which should be reassigned based its adapter ID
     *
     * @param binding the {@link TimeSeriesBinding} instance to clone.
     */
    public TimeSeriesBinding(TimeSeriesBinding binding) {
        this(binding.label,
                binding.path,
                binding.color,
                binding.legend,
                binding.prefix,
                binding.graphType,
                binding.unitName,
                binding.treeHierarchy,
                null,
                binding.adapterId);
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class with the provided properties
     *
     * @param label         the label for the binding
     * @param path          the path to retrieve value from the source
     * @param color         the color of the graph
     * @param legend        the legend of the binding
     * @param prefix        the unit prefix
     * @param graphType     the preferred type of graph representation
     * @param unitName      the unit for values
     * @param treeHierarchy the hierarchy in the tree representation
     * @param adapter       the {@link SerializedDataAdapter} to the source
     */
    public TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter<Double> adapter) {
        this(label,
                path,
                color,
                legend,
                prefix,
                graphType,
                unitName,
                treeHierarchy,
                adapter,
                null);
    }

    private TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter<Double> adapter, UUID adapterId) {
        super(label, legend, color, path, treeHierarchy, adapter, adapterId);
        this.prefix = prefix;
        this.graphType = graphType;
        this.unitName = unitName;
    }

    /**
     * Returns the type of graph of the bound series as defined in the source.
     *
     * @return the type of graph of the bound series as defined in the source.
     */
    public ChartType getGraphType() {
        return graphType;
    }

    /**
     * Returns the unit name of the bound series as defined in the source.
     *
     * @return the  unit name of the bound series as defined in the source.
     */
    public String getUnitName() {
        return unitName;
    }

    /**
     * Returns the unit numerical base of the bound series as defined in the source.
     *
     * @return the  unit numerical base of the bound series as defined in the source.
     */
    public UnitPrefixes getUnitPrefix() {
        return prefix;
    }

    @Override
    public int hashCode() {
        return super.hashCode() +
                Objects.hashCode(prefix) +
                Objects.hashCode(graphType)+
                Objects.hashCode(unitName);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) &&
                Objects.equals(prefix, ((TimeSeriesBinding) obj).prefix) &&
                Objects.equals(graphType, ((TimeSeriesBinding) obj).graphType) &&
                Objects.equals(unitName, ((TimeSeriesBinding) obj).unitName);
    }

    public static class Builder extends SourceBinding.Builder<Double, TimeSeriesBinding, TimeSeriesBinding.Builder> {
        private UnitPrefixes prefix = UnitPrefixes.METRIC;
        private ChartType graphType = ChartType.STACKED;
        private String unitName = "-";

        public Builder withPrefix(UnitPrefixes prefix) {
            this.prefix = prefix;
            return self();
        }

        public Builder withGraphType(ChartType graphType) {
            this.graphType = graphType;
            return self();
        }

        public Builder withUnitName(String unitName) {
            this.unitName = unitName;
            return self();
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected TimeSeriesBinding construct(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<Double> adapter) {
            return new TimeSeriesBinding(label, path, color, legend, prefix, graphType, unitName, treeHierarchy, adapter);
        }
    }
}
