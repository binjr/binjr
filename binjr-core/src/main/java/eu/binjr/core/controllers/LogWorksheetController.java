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

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.LogWorksheet;
import eu.binjr.core.data.workspace.Worksheet;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeView;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

public class LogWorksheetController extends WorksheetController {
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/LogWorksheetView.fxml";
    private LogWorksheet worksheet;
    private Property<TimeRange> timeRangeProperty = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));

    public LogWorksheetController(MainViewController parent, LogWorksheet worksheet, Collection<?> collection){
        super(parent);
        this.worksheet = worksheet;
    }

    @Override
    public Worksheet getWorksheet() {
        return worksheet;
    }

    @Override
    public Property<TimeRange> selectedRangeProperty() {
        return timeRangeProperty;
    }

    @Override
    public Optional<ChartViewPort> getAttachedViewport(TitledPane pane) {
        return Optional.empty();
    }

    @Override
    public ContextMenu getChartListContextMenu(TreeView<TimeSeriesBinding> treeView) {
        return new ContextMenu(new MenuItem("Empty"));
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {

    }

    @Override
    public void refresh() {

    }

    @Override
    public void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {

    }

    @Override
    public void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh) {

    }

    @Override
    public void saveSnapshot() {

    }

    @Override
    public void toggleShowPropertiesPane() {

    }

    @Override
    public void setShowPropertiesPane(boolean value) {

    }

    @Override
    public List<ChartViewPort> getViewPorts() {
        return new ArrayList<>();
    }



    @Override
    public void close() {

    }

    @Override
    public String getView() {
        return   WORKSHEET_VIEW_FXML;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
