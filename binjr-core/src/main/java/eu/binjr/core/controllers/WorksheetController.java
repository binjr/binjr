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
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
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

public abstract class WorksheetController implements Initializable, Closeable {
    protected final BindingManager bindingManager = new BindingManager();
    protected final MainViewController parentController;

    protected WorksheetController(MainViewController parentController){
        this.parentController = parentController;
    }

    /**
     * Returns the {@link XYChartsWorksheet} instance associated with this controller
     *
     * @return the {@link XYChartsWorksheet} instance associated with this controller
     */
    public abstract Worksheet getWorksheet();

    public abstract Property<TimeRange> selectedRangeProperty();

    public abstract Optional<ChartViewPort> getAttachedViewport(TitledPane pane);

    public abstract ContextMenu getChartListContextMenu(Collection<TreeItem<SourceBinding>> treeView);

    public abstract void setReloadRequiredHandler(Consumer<WorksheetController> action);

    public abstract void refresh();

    public abstract void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh);

    public abstract  void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh);

    public abstract  void saveSnapshot();

    public abstract void toggleShowPropertiesPane();

    public abstract void setShowPropertiesPane(boolean value);

    public abstract List<ChartViewPort> getViewPorts();


    public BindingManager getBindingManager() {
        return bindingManager;
    }

    @Override
    public abstract void close();

    public abstract String getView();
}
