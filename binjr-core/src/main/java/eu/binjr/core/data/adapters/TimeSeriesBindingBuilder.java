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

package eu.binjr.core.data.adapters;

import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

public class TimeSeriesBindingBuilder {
    private String label = "";
    private String path = "";
    private Color color = null;
    private String legend = null;
    private UnitPrefixes prefix = UnitPrefixes.METRIC;
    private ChartType graphType = ChartType.STACKED;
    private String unitName = "";
    private String treeHierarchy = "";
    private final DataAdapter adapter;
    private String parent = null;

    public TimeSeriesBindingBuilder(DataAdapter adapter) {
        this.adapter = adapter;
    }

    public TimeSeriesBindingBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public TimeSeriesBindingBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public TimeSeriesBindingBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    public TimeSeriesBindingBuilder setLegend(String legend) {
        this.legend = legend;
        return this;
    }

    public TimeSeriesBindingBuilder setPrefix(UnitPrefixes prefix) {
        this.prefix = prefix;
        return this;
    }

    public TimeSeriesBindingBuilder setGraphType(ChartType graphType) {
        this.graphType = graphType;
        return this;
    }

    public TimeSeriesBindingBuilder setUnitName(String unitName) {
        this.unitName = unitName;
        return this;
    }

    public TimeSeriesBindingBuilder setTreeHierarchy(String treeHierarchy) {
        this.treeHierarchy = treeHierarchy;
        return this;
    }

    public TimeSeriesBindingBuilder setParent(String parent) {
        this.parent = parent;
        return this;
    }

    public TimeSeriesBinding build() {
        if (legend == null) {
            legend = label;
        }
        if (parent != null) {
            treeHierarchy = parent + "/" + legend;
        }
        return new TimeSeriesBinding(label, path, color, legend, prefix, graphType, unitName, treeHierarchy, adapter);
    }

    // public TimeSeriesBinding(String label,
    // String path,
    // Color color,
    // String legend,
    // UnitPrefixes prefix,
    // ChartType graphType,
    // String unitName,
    // String treeHierarchy,
    // DataAdapter adapter) {

}
