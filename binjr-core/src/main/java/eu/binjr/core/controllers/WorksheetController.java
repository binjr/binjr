/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.controlsfx.control.Notifications;

import javax.imageio.ImageIO;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class WorksheetController implements Initializable, Closeable {
    private static final Logger logger = Logger.create(WorksheetController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private final BindingManager bindingManager = new BindingManager();
    private final MainViewController parentController;
    @FXML
    public AnchorPane root;

    @FXML
    public Button toggleChartDisplayModeButton;

    protected WorksheetController(MainViewController parentController) {
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

    public void refresh(boolean force) {
        refresh();
    }

    protected Image captureSnapshot() {
        return null;
    }

    protected void saveSnapshotToFile() {
        var snapImg = captureSnapshot();
        if (snapImg == null || snapImg.getWidth() == 0 || snapImg.getHeight() == 0) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SnapShot");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(String.format("binjr_snapshot_%s.png", getWorksheet().getName()));
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                if (selectedFile.getParent() != null) {
                    UserHistory.getInstance().mostRecentSaveFolders.push(selectedFile.getParentFile().toPath());
                }
                ImageIO.write(SwingFXUtils.fromFXImage(snapImg, null), "png", selectedFile);
            } catch (IOException e) {
                Dialogs.notifyException("Failed to save snapshot to disk", e, root);
            }
        }
    }

    public void saveSnapshot() {
        var snapImg = captureSnapshot();
        if (snapImg == null || snapImg.getWidth() == 0 || snapImg.getHeight() == 0) {
            return;
        }

        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.IMAGE, snapImg));

        var imgView = new ImageView(snapImg);
        var ratio = snapImg.getHeight() / snapImg.getWidth();
        var maxHeight = UserPreferences.getInstance().maxSnapshotSnippetHeight.get().intValue();
        var maxWidth = UserPreferences.getInstance().maxSnapshotSnippetWidth.get().intValue();
        if (ratio * maxWidth > maxHeight) {
            imgView.setFitHeight(maxHeight);
        } else {
            imgView.setFitWidth(maxWidth);
        }
        imgView.setPreserveRatio(true);
        var box = new VBox(imgView);
        box.setSpacing(10);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);
        var saveBtn = new Button("Save to file");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(getBindingManager().registerHandler(event -> {
            Dialogs.dismissParentNotificationPopup((Node) event.getSource());
            saveSnapshotToFile();
        }));
        box.getChildren().add(saveBtn);
        Dialogs.runOnFXThread(() -> Notifications.create()
                .title("Snapshot saved to clipboard")
                .text("""
                        You can paste it into another 
                        application or save it to a file
                        by pressing the button below.
                        """)
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(Pos.BOTTOM_RIGHT)
                .graphic(box)
                .owner(root).show());
    }

    public void toggleShowPropertiesPane() {
    }

    public void setShowPropertiesPane(boolean value) {
    }

    public abstract List<ChartViewPort> getViewPorts();

    public void navigateBackward() {
        //Noop
    }

    public void navigateForward() {
        //Noop
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleChartDisplayModeButton.setOnAction(getBindingManager().registerHandler(event -> getParentController().handleTogglePresentationMode()));
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
    public void close() {
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

    protected < T extends TimeSeriesInfo<?>> TableRow<T> seriesTableRowFactory(TableView<T> tv) {
        TableRow<T> row = new TableRow<>();
        var selectionIsEmpty = Bindings.createBooleanBinding(
                () -> tv.getSelectionModel().getSelectedItems().isEmpty(),
                tv.getSelectionModel().getSelectedItems());
        var selectionIsMono = Bindings.createBooleanBinding(
                () -> tv.getSelectionModel().getSelectedItems().size() < 2,
                tv.getSelectionModel().getSelectedItems());
        var menu = new ContextMenu();

        var selectAllMenuItem = new MenuItem("Select All");
        selectAllMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            tv.getSelectionModel().selectAll();
        }));

        var removeMenuItem = new MenuItem("Delete");
        getBindingManager().bind(removeMenuItem.visibleProperty(), selectionIsEmpty.not());
        removeMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            List<T> selected = new ArrayList<>(tv.getSelectionModel().getSelectedItems());
            tv.getItems().removeAll(selected);
            tv.getSelectionModel().clearSelection();
            refresh();
        }));

        var renameSingleMenuItem = new MenuItem("Rename");
        getBindingManager().bind(renameSingleMenuItem.visibleProperty(), selectionIsMono.and(selectionIsEmpty.not()));
        renameSingleMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            tv.edit(tv.getSelectionModel().getSelectedIndex(), tv.getColumns().get(2));
        }));

        var copyPathMenuItem = new MenuItem("Path");
        getBindingManager().bind(copyPathMenuItem.visibleProperty(), selectionIsEmpty.not());
        copyPathMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            Clipboard.getSystemClipboard().setContent(Map.of(
                    DataFormat.PLAIN_TEXT, tv.getSelectionModel().getSelectedItems().stream()
                            .map(s -> s.getBinding().getTreeHierarchy())
                            .collect(Collectors.joining("\n"))));
        }));


        var copyAllMenuItem = new MenuItem("All Series Info");
        getBindingManager().bind(copyAllMenuItem.visibleProperty(), selectionIsEmpty.not());
        copyAllMenuItem.setOnAction(getBindingManager().registerHandler(e -> {
            Clipboard.getSystemClipboard().setContent(Map.of(
                    DataFormat.PLAIN_TEXT, tv.getSelectionModel().getSelectedItems().stream()
                            .map(s -> String.join("\t",
                                    s.getDisplayName(),
                                    s.getProcessor().getMinValue().toString(),
                                    s.getProcessor().getMaxValue().toString(),
                                    s.getProcessor().getAverageValue().toString(),
                                    s.getBinding().getTreeHierarchy()))
                            .collect(Collectors.joining("\n"))));
        }));

        var copyMenu = new Menu("Copy");
        getBindingManager().bind(copyMenu.visibleProperty(), selectionIsEmpty.not());
        copyMenu.getItems().addAll(copyPathMenuItem, copyAllMenuItem);

        var inferNameItem = new MenuItem("Automatically Infer Names");
        getBindingManager().bind(inferNameItem.visibleProperty(), selectionIsMono.not());
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

        var inferColorItem = new MenuItem("Automatically Select Color");
        getBindingManager().bind(inferColorItem.visibleProperty(), selectionIsEmpty.not());
        inferColorItem.setOnAction(getBindingManager().registerHandler(e -> {
            tv.getSelectionModel().getSelectedItems().forEach(s -> {
                s.setDisplayColor(s.getBinding().getAutoColor(s.getDisplayName()));
            });
        }));

        var separator = new SeparatorMenuItem();
        getBindingManager().bind(separator.visibleProperty(), selectionIsEmpty.not());
        var separator2 = new SeparatorMenuItem();
        getBindingManager().bind(separator2.visibleProperty(), selectionIsEmpty.not());

        menu.getItems().setAll(
                selectAllMenuItem,
                copyMenu,
                separator,
                removeMenuItem,
                renameSingleMenuItem,
                separator2,
                inferNameItem,
                inferColorItem);
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
                T draggedseries = tv.getItems().remove(draggedIndex);
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
