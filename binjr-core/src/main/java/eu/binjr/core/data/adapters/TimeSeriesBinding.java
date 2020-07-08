/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Represents a binding between a time series and the {@link SerializedDataAdapter} used to produce it.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class TimeSeriesBinding extends SourceBinding {
    private static final Logger logger = LogManager.getLogger(TimeSeriesBinding.class);
    private static final ThreadLocal<MessageDigest> messageDigest = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Failed to instantiate MD5 message digest");
            throw new IllegalStateException("Failed to create a new instance of Md5HashTargetResolver", e);
        }
    });

    @XmlAttribute
    private final Color color;
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
        this.color = null;
        this.prefix = UnitPrefixes.BINARY;
        this.graphType = ChartType.STACKED;
        this.unitName = "";

    }

    @Override
    public Class<? extends Worksheet> getWorksheetClass() {
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
    public TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter adapter) {
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

    private TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter adapter, UUID adapterId) {
        super(label, legend, path, treeHierarchy, adapter, adapterId);
        this.prefix = prefix;
        this.graphType = graphType;
        this.unitName = unitName;
        this.color = color;
    }

    /**
     * Returns the color of the bound series as defined in the source.
     *
     * @return the color of the bound series as defined in the source.
     */
    public Color getColor() {
        return this.color == null ? computeDefaultColor() : this.color;
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

    private Color computeDefaultColor() {
        long targetNum = getHashValue(this.getLabel()) % StageAppearanceManager.getInstance().getDefaultChartColors().length;
        if (targetNum < 0) {
            targetNum = targetNum * -1;
        }
        return StageAppearanceManager.getInstance().getDefaultChartColors()[((int) targetNum)];
    }

    private long getHashValue(final String value) {
        long hashVal;
        messageDigest.get().update(value.getBytes(StandardCharsets.UTF_8));
        hashVal = new BigInteger(1, messageDigest.get().digest()).longValue();
        return hashVal;
    }

    public static class Builder extends SourceBinding.Builder<TimeSeriesBinding, TimeSeriesBinding.Builder> {
        private Color color = null;
        private UnitPrefixes prefix = UnitPrefixes.METRIC;
        private ChartType graphType = ChartType.STACKED;
        private String unitName = "-";

        public Builder withColor(Color color) {
            this.color = color;
            return self();
        }

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
        protected TimeSeriesBinding construct(String label, String legend, String path, String treeHierarchy, DataAdapter adapter) {
            return new TimeSeriesBinding(label, path, color, legend, prefix, graphType, unitName, treeHierarchy, adapter);
        }
    }
}
