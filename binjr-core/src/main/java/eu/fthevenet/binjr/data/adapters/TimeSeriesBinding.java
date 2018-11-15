/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Represents a binding between a time series and the {@link DataAdapter} used to produce it.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Binding")
public class TimeSeriesBinding<T> {
    private static final Logger logger = LogManager.getLogger(TimeSeriesBinding.class);
    private static final ThreadLocal<MessageDigest> messageDigest = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Failed to instantiate MD5 message digest");
            throw new IllegalStateException("Failed to create a new instance of Md5HashTargetResolver", e);
        }
    });
    private final static Color[] defaultChartColors = new Color[]{
            Color.LIGHTBLUE,
            Color.LIGHTCORAL,
            Color.LIGHTCYAN,
            Color.LIGHTGRAY,
            Color.LIGHTGREEN,
            Color.LEMONCHIFFON,
            Color.LAVENDER,
            Color.LIGHTPINK,
            Color.LIGHTSALMON,
            Color.LIGHTSEAGREEN,
            Color.LIGHTSKYBLUE,
            Color.LIGHTSLATEGRAY,
            Color.LIGHTSTEELBLUE,
            Color.LIGHTYELLOW,
            Color.MEDIUMBLUE,
            Color.MEDIUMORCHID,
            Color.MEDIUMPURPLE,
            Color.MEDIUMSEAGREEN,
            Color.MEDIUMSPRINGGREEN,
            Color.MEDIUMTURQUOISE,
            Color.MEDIUMVIOLETRED,
            Color.MINTCREAM,
    };
    @XmlAttribute(name = "sourceId")
    private final UUID adapterId;
    @XmlAttribute
    private final String label;
    @XmlAttribute
    private final String path;
    @XmlAttribute
    private final Color color;
    @XmlAttribute
    private final String legend;
    @XmlAttribute
    private final UnitPrefixes prefix;
    @XmlAttribute
    private final ChartType graphType;
    @XmlAttribute
    private final String unitName;
    @XmlAttribute
    private final String treeHierarchy;
    @XmlTransient
    private DataAdapter<T, ?> adapter;

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class.
     */
    public TimeSeriesBinding() {
        this.label = "";
        this.path = "";
        this.color = null;
        this.legend = "";
        this.prefix = UnitPrefixes.BINARY;
        this.graphType = ChartType.STACKED;
        this.unitName = "";
        this.adapter = null;
        this.treeHierarchy = "";
        adapterId = null;
    }

    /**
     * Creates a clone of the provided binding, save for its {@link DataAdapter} instance which should be reassigned based its adapter ID
     *
     * @param binding the {@link TimeSeriesBinding} instance to clone.
     */
    public TimeSeriesBinding(TimeSeriesBinding<T> binding) {
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
     * @param adapter       the {@link DataAdapter} to the source
     */
    public TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter<T, ?> adapter) {
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

    private TimeSeriesBinding(String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter<T, ?> adapter, UUID adapterId) {
        this.label = label;
        this.path = path;
        this.legend = legend;
        this.prefix = prefix;
        this.graphType = graphType;
        this.unitName = unitName;
        this.treeHierarchy = treeHierarchy;
        this.adapter = adapter;
        UUID id = adapterId;
        if (id == null && adapter != null) {
            id = adapter.getId();
        }
        this.adapterId = id;
        if (color == null) {
            // pickup a default color at random, based on the hash of the binding path, so it stays stable
            this.color = computeDefaultColor();
        }
        else {
            this.color = color;
        }
    }

    /**
     * Returns the label of the binding
     *
     * @return the label of the binding
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns the path of the binding
     *
     * @return the path of the binding
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Returns the {@link DataAdapter} of the binding
     *
     * @return the {@link DataAdapter} of the binding
     */
    @XmlTransient
    public DataAdapter<T, ?> getAdapter() {
        return this.adapter;
    }

    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns the color of the bound series as defined in the source.
     *
     * @return the color of the bound series as defined in the source.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Returns the legend of the bound series as defined in the source.
     *
     * @return the legend of the bound series as defined in the source.
     */
    public String getLegend() {
        return this.legend;
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

    /**
     * Gets the {@link DataAdapter}'s id
     *
     * @return the {@link DataAdapter}'s id
     */
    public UUID getAdapterId() {
        return adapterId;
    }

    /**
     * Returns the full hierarchy in the tree for the binding
     *
     * @return the full hierarchy in the tree for the binding
     */
    public String getTreeHierarchy() {
        return treeHierarchy;
    }

    @Override
    public String toString() {
        return getLegend();
    }

    private Color computeDefaultColor() {
        long targetNum = getHashValue(this.getTreeHierarchy()) % defaultChartColors.length;
        if (targetNum < 0) {
            targetNum = targetNum * -1;
        }
        return defaultChartColors[((int) targetNum)];
    }

    private long getHashValue(final String value) {
        long hashVal;
        messageDigest.get().update(value.getBytes(Charset.forName("UTF-8")));
        hashVal = new BigInteger(1, messageDigest.get().digest()).longValue();
        return hashVal;
    }

}
