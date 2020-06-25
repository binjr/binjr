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

package eu.binjr.core.controllers;

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.Property;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface WorksheetController extends Initializable, Closeable {
    static Optional<List<Chart>> treeItemsAsChartList(Collection<TreeItem<TimeSeriesBinding>> treeItems, Node dlgRoot) {
        var charts = new ArrayList<Chart>();
        var totalBindings = 0;
        for (var treeItem : treeItems) {
            for (var t : TreeViewUtils.splitAboveLeaves(treeItem, true)) {
                TimeSeriesBinding binding = t.getValue();
                Chart chart = new Chart(
                        binding.getLegend(),
                        binding.getGraphType(),
                        binding.getUnitName(),
                        binding.getUnitPrefix()
                );
                for (TimeSeriesBinding b : TreeViewUtils.flattenLeaves(t)) {
                    chart.addSeries(TimeSeriesInfo.fromBinding(b));
                    totalBindings++;
                }
                charts.add(chart);
            }
        }
        if (totalBindings >= UserPreferences.getInstance().maxSeriesPerChartBeforeWarning.get().intValue()) {
            if (Dialogs.confirmDialog(dlgRoot,
                    "This action will add " + totalBindings + " series on a single worksheet.",
                    "Are you sure you want to proceed?",
                    ButtonType.YES, ButtonType.NO) != ButtonType.YES) {
                return Optional.empty();
            }
        }
        return Optional.of(charts);
    }

    /**
     * Returns the {@link Worksheet} instance associated with this controller
     *
     * @return the {@link Worksheet} instance associated with this controller
     */
    Worksheet getWorksheet();

    Property<TimeRange> selectedRangeProperty();

    Optional<ChartViewPort> getAttachedViewport(TitledPane pane);

    ContextMenu getChartListContextMenu(TreeView<TimeSeriesBinding> treeView);

    void setReloadRequiredHandler(Consumer<WorksheetController> action);

    void refresh();

    void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh);

    void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh);

    void saveSnapshot();

    void toggleShowPropertiesPane();

    void setShowPropertiesPane(boolean value);

    List<ChartViewPort> getViewPorts();

    BindingManager getBindingManager();

    @Override
    void close();
}
