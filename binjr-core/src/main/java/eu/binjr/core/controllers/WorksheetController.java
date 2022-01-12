/*
 *    Copyright 2020-2021 Frederic Thevenet
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
import eu.binjr.common.javafx.controls.SnapshotUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.ToolButtonBuilder;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.core.dialogs.Dialogs;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

import java.io.Closeable;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public abstract class WorksheetController implements Initializable, Closeable {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private final BindingManager bindingManager = new BindingManager();
    private final MainViewController parentController;
    @FXML
    public AnchorPane root;

    @FXML
    public Button toggleChartDisplayModeButton;

    protected WorksheetController(MainViewController parentController){
        this.parentController = parentController;
    }

    /**
     * Returns the {@link XYChartsWorksheet} instance associated with this controller
     *
     * @return the {@link XYChartsWorksheet} instance associated with this controller
     */
    public abstract Worksheet<?> getWorksheet();

    public abstract Property<TimeRange> selectedRangeProperty();

    public abstract Optional<ChartViewPort> getAttachedViewport(TitledPane pane);

    public abstract ContextMenu getChartListContextMenu(Collection<TreeItem<SourceBinding>> treeView);

    public abstract void setReloadRequiredHandler(Consumer<WorksheetController> action);

    public abstract void refresh();

    public void refresh(boolean force){
        refresh();
    }

    public abstract  void saveSnapshot();

    public abstract void toggleShowPropertiesPane();

    public abstract void setShowPropertiesPane(boolean value);

    public abstract List<ChartViewPort> getViewPorts();

    public void navigateBackward(){
        //Noop
    }

    public void navigateForward(){
        //Noop
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleChartDisplayModeButton.setOnAction(getBindingManager().registerHandler(event ->  getParentController().handleTogglePresentationMode()));
        getBindingManager().attachListener(getWorksheet().editModeEnabledProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            setEditChartMode(newValue);
        });
        setEditChartMode(getWorksheet().isEditModeEnabled());
    }

    public BindingManager getBindingManager() {
        return bindingManager;
    }

    public MainViewController getParentController() {
        return parentController;
    }

    @Override
    public  void close(){
        bindingManager.close();
    }

    public abstract String getView();

    protected void setEditChartMode(Boolean newValue) {
        if (!newValue) {
            toggleChartDisplayModeButton.getTooltip().setText("Reduce series view (Ctrl+M)");
            toggleChartDisplayModeButton.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "reduce-view-icon"));
        } else {
            toggleChartDisplayModeButton.getTooltip().setText("Expand series view (Ctrl+M)");
            toggleChartDisplayModeButton.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "expand-view-icon"));
        }
    }

    protected <T> TableRow<TimeSeriesInfo<T>> seriesTableRowFactory(TableView<TimeSeriesInfo<T>> tv) {
        TableRow<TimeSeriesInfo<T>> row = new TableRow<>();
        var selectionIsEmpty = Bindings.createBooleanBinding(
                () -> tv.getSelectionModel().getSelectedItems().isEmpty(),
                tv.getSelectionModel().getSelectedItems());
        var selectionIsMono = Bindings.createBooleanBinding(
                () -> tv.getSelectionModel().getSelectedItems().size() < 2,
                tv.getSelectionModel().getSelectedItems());
        var menu = new ContextMenu();
        var removeMenuItem = new MenuItem("Remove Series");
        getBindingManager().bind(removeMenuItem.disableProperty(), selectionIsEmpty);
        removeMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            List<TimeSeriesInfo<T>> selected = new ArrayList<>(tv.getSelectionModel().getSelectedItems());
            tv.getItems().removeAll(selected);
            tv.getSelectionModel().clearSelection();
            refresh();
        }));

        var inferNameItem = new MenuItem("Infer Series Names");
        getBindingManager().bind(inferNameItem.disableProperty(), selectionIsMono);
        inferNameItem.setOnAction(getBindingManager().registerHandler(e -> {
            var tokens = tv.getSelectionModel().getSelectedItems().stream()
                    .map(t -> t.getBinding().getTreeHierarchy().split("/")).toList();
            tokens.stream().mapToInt(a -> a.length).max().ifPresent(maxToken -> {
                var keepList = new ArrayList<Integer>();
                for (int i = 0; i < maxToken; i++) {
                    for (String[] token : tokens) {
                        if (token.length <= i || !tokens.get(0)[i].equals(token[i])) {
                            keepList.add(i);
                            break;
                        }
                    }
                }
                for (int i = 0; i < tokens.size(); i++) {
                    var token = tokens.get(i);
                    var reduced = new ArrayList<String>();
                    for (int j = 0; j < Math.min(maxToken, token.length); j++) {
                        if (keepList.contains(j)) {
                            reduced.add(token[j]);
                        }
                    }
                    tv.getSelectionModel().getSelectedItems().get(i).setDisplayName(String.join(" ", reduced));
                }
            });
        }));

        var inferColorItem = new MenuItem("Infer Series Colors");
        getBindingManager().bind(inferColorItem.disableProperty(), selectionIsEmpty);
        inferColorItem.setOnAction(getBindingManager().registerHandler(e -> {
            tv.getSelectionModel().getSelectedItems().forEach(s -> {
                s.setDisplayColor(s.getBinding().getAutoColor(s.getDisplayName()));
            });
        }));

        var automation = new Menu("Automation");
        automation.getItems().setAll(inferNameItem, inferColorItem);
        menu.getItems().setAll(removeMenuItem, automation);
        row.setContextMenu(menu);


        row.setOnDragDetected(getBindingManager().registerHandler(event -> {
            if (!row.isEmpty()) {
                Integer index = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(SnapshotUtils.scaledSnapshot(row, Dialogs.getOutputScaleX(root), Dialogs.getOutputScaleY(root)));
                ClipboardContent cc = new ClipboardContent();
                cc.put(SERIALIZED_MIME_TYPE, index);
                db.setContent(cc);
                event.consume();
            }
        }));

        row.setOnDragOver(getBindingManager().registerHandler(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE) && row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        }));

        row.setOnDragDropped(getBindingManager().registerHandler(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                TimeSeriesInfo<T> draggedseries = tv.getItems().remove(draggedIndex);
                int dropIndex;
                if (row.isEmpty()) {
                    dropIndex = tv.getItems().size();
                } else {
                    dropIndex = row.getIndex();
                }
                tv.getItems().add(dropIndex, draggedseries);
                event.setDropCompleted(true);
                tv.getSelectionModel().clearAndSelect(dropIndex);
                refresh();
                event.consume();
            }
        }));
        return row;
    }

}
