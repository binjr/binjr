/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.util.javafx.controls;


import eu.fthevenet.util.javafx.bindings.BindingManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A TabPane container with a button to add a new tab that also supports tearing away tabs into a separate window.
 * <p>It relies on the {@link TabPaneManager} class to keep track of all tabs spread over many windows and {@link TabPane} instances</p>
 * <p><b>TODO: Reordering of the tabs is currently not implemented.</b></p>
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(TearableTabPane.class);
    private boolean tearable;
    private boolean reorderable;
    private Function<ActionEvent, Optional<Tab>> newTabFactory = (e) -> Optional.of(new Tab());
    private final Map<Tab, TabState> tearableTabMap = new HashMap<>();
    private final TabPaneManager manager;
    private EventHandler<ActionEvent> onAddNewTab;
    private EventHandler<WindowEvent> onOpenNewWindow;
    private EventHandler<WindowEvent> onClosingWindow;
    private BindingManager bindingManager = new BindingManager();

    /**
     * Initializes a new instance of the {@link TearableTabPane} class.
     */
    public TearableTabPane() {
        this(new TabPaneManager(), false, false, (Tab[]) null);
    }

    /**
     * Initializes a new instance of the {@link TearableTabPane} class.
     *
     * @param manager     the {@link TabPaneManager} manager instance
     * @param reorderable true if tabs can be reordered, false otherwise.
     * @param tearable    true if tabs are teared away from the pane, false otherwise.
     * @param tabs        tabs to attached to the TabPane.
     */
    public TearableTabPane(TabPaneManager manager, boolean reorderable, boolean tearable, Tab... tabs) {
        super(tabs);
        this.manager = manager;
        this.tearable = tearable;
        this.reorderable = reorderable;
        bindingManager.attachListener(this.getSelectionModel().selectedItemProperty(),
                (ChangeListener<Tab>) (observable, oldValue, newValue) -> this.manager.setSelectedTab(newValue));
        this.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab t : c.getAddedSubList()) {
                        this.tearableTabMap.put(t, new TabState(true));
                        this.manager.addTab(t, this);
                    }
                }
                if (c.wasRemoved()) {
                    for (Tab t : c.getRemoved()) {
                        this.tearableTabMap.remove(t);
                        this.manager.removeTab(t);
                    }
                }
            }
            logger.trace(() -> "Tearable tabs in tab pane: " +
                    tearableTabMap.keySet().stream()
                            .map(tab -> tab.getText() == null ? tab.toString() : tab.getText())
                            .reduce((s, s2) -> s + " " + s2).orElse("null"));
        });
        this.setOnDragDetected(
                (MouseEvent event) -> {
                    if (!this.tearable) {
                        return;
                    }
                    if (event.getSource() instanceof TabPane) {
                        Tab currentTab = this.getSelectionModel().getSelectedItem();
                        if (currentTab != null) {
                            SnapshotParameters snapshotParams = new SnapshotParameters();
                            WritableImage snapshot = currentTab.getContent().snapshot(snapshotParams, null);
                            Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                            ClipboardContent clipboardContent = new ClipboardContent();
                            clipboardContent.put(manager.getDragAndDropFormat(), manager.getId(currentTab));
                            db.setDragView(snapshot, -5, -5);
                            db.setContent(clipboardContent);
                            manager.startDragAndDrop();
                        }
                    }
                    event.consume();
                }
        );
        this.setOnDragOver(event -> {
            if (!this.tearable) {
                return;
            }
            Dragboard db = event.getDragboard();
            if (db.hasContent(manager.getDragAndDropFormat())) {
                String id = (String) db.getContent(manager.getDragAndDropFormat());
                Tab t = manager.getTab(id);
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        this.setOnDragDone(
                (DragEvent event) -> {
                    if (!this.tearable || event.isDropCompleted()) {
                        return;
                    }
                    Dragboard db = event.getDragboard();
                    if (manager.completeDragAndDrop() && db.hasContent(manager.getDragAndDropFormat())) {
                        String id = (String) db.getContent(manager.getDragAndDropFormat());
                        logger.trace(() -> "setOnDragDone fired");
                        Tab t = manager.getTab(id);
                        manager.setMovingTab(true);
                        try {
                            tearOffTab(t);
                        } finally {
                            manager.setMovingTab(false);
                        }
                    }
                    event.consume();
                }
        );
        this.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(manager.getDragAndDropFormat())) {
                logger.trace(() -> "setOnDragDropped fired");
                if (manager.completeDragAndDrop()) {
                    String id = (String) db.getContent(manager.getDragAndDropFormat());
                    Tab t = manager.getTab(id);
                    if (t != null) {
                        TabPane p = manager.getTabPane(t);
                        if (reorderable || !this.equals(p)) {
                            manager.setMovingTab(true);
                            try {
                                p.getTabs().remove(t);
                                this.getTabs().add(t);
                                this.getSelectionModel().select(t);
                                bringStageToFront();
                            } finally {
                                manager.setMovingTab(false);
                            }
                        }
                        else {
                            logger.debug(() -> "Tabs on this pane cannot be reordered");
                        }
                    }
                    else {
                        logger.debug(() -> "Failed to retrieve tab with id " + (id != null ? id : "null"));
                    }
                }
                event.consume();
            }
        });
        Platform.runLater(() -> {
            positionNewTabButton();
            Stage stage = (Stage) this.getScene().getWindow();
            bindingManager.attachListener(stage.focusedProperty(),
                    (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                        if (newValue) {
                            manager.setSelectedTab(this.getSelectionModel().getSelectedItem());
                        }
                    });
        });

        // Prepare to change the button on screen position if the tearableTabMap side changes
        bindingManager.attachListener(sideProperty(),
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        positionNewTabButton();
                    }
                });
    }

    /**
     * Returns the factory for creating new tabs
     *
     * @return the factory for creating new tabs
     */
    public Function<ActionEvent, Optional<Tab>> getNewTabFactory() {
        return newTabFactory;
    }

    /**
     * Sets the factory for creating new tabs
     *
     * @param newTabFactory the factory for creating new tabs
     */
    public void setNewTabFactory(Function<ActionEvent, Optional<Tab>> newTabFactory) {
        this.newTabFactory = newTabFactory;
    }

    /**
     * Sets the action that should be fired on the addition of a tab to the pane.
     *
     * @param onAddNewTab the actions that should be fired on the addition of a tab to the pane.
     */
    public void setOnAddNewTab(EventHandler<ActionEvent> onAddNewTab) {
        this.onAddNewTab = onAddNewTab;
    }

    /**
     * Returns true if tabs can be teared away from the pane, false otherwise.
     *
     * @return true true if tabs can be teared away from the pane, false otherwise.
     */
    public boolean isTearable() {
        return tearable;
    }

    /**
     * Set to true if tabs can be teared away from the pane, false otherwise.
     *
     * @param tearable true if tabs can be teared away from the pane, false otherwise.
     */
    public void setTearable(boolean tearable) {
        this.tearable = tearable;
    }

    /**
     * Returns true if tabs can be reordered, false otherwise.
     *
     * @return true if tabs can be reordered, false otherwise.
     */
    public boolean isReorderable() {
        return reorderable;
    }

    /**
     * Set to true if tabs can be reordered, false otherwise.
     *
     * @param reorderable true if tabs can be reordered, false otherwise.
     */
    public void setReorderable(boolean reorderable) {
        this.reorderable = reorderable;
    }

    /**
     * Returns the tab currently selected.
     *
     * @return the tab currently selected.
     */
    public Tab getSelectedTab() {
        return manager.getSelectedTab();
    }

    /**
     * Returns the pane containing the tab currently selected.
     *
     * @return the pane containing the tab currently selected.
     */
    public TabPane getSelectedTabPane() {
        if (manager.getSelectedTab() == null || manager.tabToPaneMap.get(manager.getSelectedTab()) == null) {
            return this;
        }
        return manager.tabToPaneMap.get(getSelectedTab());
    }

    /**
     * Returns a list of all tabs, across of panes sharing the same {@link TabPaneManager}
     *
     * @return a list of all tabs, across of panes sharing the same {@link TabPaneManager}
     */
    public ObservableList<Tab> getGlobalTabs() {
        return manager.getGlobalTabList();
    }

    /**
     * Clears the list of tabs.
     */
    public void clearAllTabs() {
        manager.clearAllTabs();
    }

    /**
     * Sets the action to be fired on opening a new window to host tabs.
     *
     * @param action the action to be fired on opening a new window to host tabs.
     */
    public void setOnOpenNewWindow(EventHandler<WindowEvent> action) {
        this.onOpenNewWindow = action;
    }

    /**
     * Sets the action to be fired on closing a window hosting tabs.
     *
     * @param action the action to be fired on closing a window hosting tabs.
     */
    public void setOnClosingWindow(EventHandler<WindowEvent> action) {
        this.onClosingWindow = action;
    }

    /**
     * Returns the generated {@link DataFormat} used to identify drag and drop operations across panes sharing the same {@link TabPaneManager}
     *
     * @return the generated {@link DataFormat} used to identify drag and drop operations across panes sharing the same {@link TabPaneManager}
     */
    public DataFormat getDataFormat() {
        return manager.dragAndDropFormat;
    }

    private void positionNewTabButton() {
        Pane tabHeaderBg = (Pane) this.lookup(".tab-header-background");
        if (tabHeaderBg == null) {
            // TabPane is not ready
            return;
        }
        Pane tabHeaderArea = (Pane) this.lookup(".tab-header-area");
        logger.debug("tabHeaderArea.getHeight() = " + tabHeaderArea.getHeight());
        Button newTabButton = (Button) tabHeaderBg.lookup("#newTabButton");

        // Remove the button if it was already present
        if (newTabButton != null) {
            tabHeaderBg.getChildren().remove(newTabButton);
        }
        newTabButton = new Button();
        newTabButton.setId("newTabButton");
        newTabButton.setFocusTraversable(false);
        Pane headersRegion = (Pane) this.lookup(".headers-region");
        Region headerArea = (Region) this.lookup(".tab-header-area");

        logger.debug("headersRegion.getHeight() = " + headersRegion.getHeight());
        logger.debug("headersRegion.getPrefHeight = " + headersRegion.getPrefHeight());
        newTabButton.getStyleClass().add("add-tab-button");
        SVGPath icon = new SVGPath();
        icon.setContent("m 31.25,54.09375 0,2.4375 -2.46875,0 0,0.375 2.46875,0 0,2.46875 0.375,0 0,-2.46875 2.46875,0 0,-0.375 -2.46875,0 0,-2.4375 -0.375,0 z");
        icon.getStyleClass().add("add-tab-button-icon");
        newTabButton.setGraphic(icon);
        newTabButton.setAlignment(Pos.CENTER);
        if (onAddNewTab != null) {
            newTabButton.setOnAction(onAddNewTab);
        }
        else {
            newTabButton.setOnAction(event -> {
                newTabFactory.apply(event).ifPresent(newTab -> {
                    getTabs().add(newTab);
                    this.getSelectionModel().select(newTab);
                });
            });
        }
        tabHeaderBg.getChildren().add(newTabButton);
        StackPane.setAlignment(newTabButton, Pos.CENTER_LEFT);
        switch (getSide()) {
            case TOP:
            case BOTTOM:
                newTabButton.translateXProperty().bind(
                        headersRegion.widthProperty()
                                .add(Bindings.createDoubleBinding(() -> headerArea.getInsets().getLeft(), headerArea.insetsProperty()))
                );
                break;
            case LEFT:
            case RIGHT:
                newTabButton.translateXProperty().bind(
                        tabHeaderBg.widthProperty()
                                .subtract(headersRegion.widthProperty())
                                .subtract(newTabButton.widthProperty())
                                .subtract(Bindings.createDoubleBinding(() -> headerArea.getInsets().getTop(), headerArea.insetsProperty()))
                );
                break;
            default:
                throw new IllegalStateException("Invalid value for side enum");
        }
    }

    private void bringStageToFront() {
        if (this.getScene() != null) {
            Stage stage = (Stage) this.getScene().getWindow();
            if (stage != null) {
                stage.toFront();
            }
        }
    }

    private void tearOffTab(Tab tab) {
        TearableTabPane detachedTabPane = new TearableTabPane(this.manager, false, true);
        detachedTabPane.setOnOpenNewWindow(this.onOpenNewWindow);
        detachedTabPane.setNewTabFactory(this.getNewTabFactory());
        this.getTabs().remove(tab);
        detachedTabPane.getTabs().add(tab);
        Pane root = new AnchorPane(detachedTabPane);
        AnchorPane.setBottomAnchor(detachedTabPane, 0.0);
        AnchorPane.setLeftAnchor(detachedTabPane, 0.0);
        AnchorPane.setRightAnchor(detachedTabPane, 0.0);
        AnchorPane.setTopAnchor(detachedTabPane, 0.0);
        final Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());
        Stage stage = new Stage();
        stage.setScene(scene);
        Point p = MouseInfo.getPointerInfo().getLocation();
        stage.setX(p.getX());
        stage.setY(p.getY());
        detachedTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.getList().size() == 0) {
                if (onClosingWindow != null) {
                    onClosingWindow.handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                }
                stage.close();
                this.close();
            }
        });
        if (onOpenNewWindow != null) {
            onOpenNewWindow.handle(new WindowEvent(stage, WindowEvent.WINDOW_SHOWING));
        }
        stage.show();
        detachedTabPane.getSelectionModel().select(tab);
        stage.setOnCloseRequest(event -> {
            detachedTabPane.getTabs().removeAll(detachedTabPane.getTabs());
        });
    }

    @Override
    public void close() {
        logger.trace(() -> "Closing down TearableTabPane instance");
        bindingManager.close();
    }

    /**
     * Represents the state of a single tab
     */
    private class TabState {
        private boolean attached;

        public boolean isAttached() {
            return attached;
        }

        public void setAttached(boolean attached) {
            this.attached = attached;
        }

        public TabState(boolean attached) {
            this.attached = attached;
        }
    }

    /**
     * A class that represents the state of the tabs across all TabPane windows
     */
    protected static class TabPaneManager {
        private final ObservableMap<Tab, TabPane> tabToPaneMap;
        private final Map<String, Tab> idToTabMap;
        private final ObservableList<Tab> globalTabList;
        private final DataFormat dragAndDropFormat;
        private final AtomicBoolean dndComplete;
        private Tab selectedTab;
        private boolean movingTab;

        public TabPaneManager() {
            tabToPaneMap = FXCollections.observableMap(new HashMap<>());
            idToTabMap = new HashMap<>();
            globalTabList = FXCollections.observableList(new ArrayList<>());
            dragAndDropFormat = new DataFormat(UUID.randomUUID().toString());
            dndComplete = new AtomicBoolean(true);
        }

        public void startDragAndDrop() {
            dndComplete.set(false);
        }

        public boolean completeDragAndDrop() {
            return dndComplete.compareAndSet(false, true);
        }

        public void addTab(Tab tab, TabPane pane) {
            idToTabMap.put(getId(tab), tab);
            tabToPaneMap.put(tab, pane);
            if (!movingTab) {
                globalTabList.add(tab);
            }
        }

        public void removeTab(Tab tab) {
            idToTabMap.remove(getId(tab));
            tabToPaneMap.remove(tab);
            if (!movingTab) {
                globalTabList.remove(tab);
            }
        }

        public TabPane getTabPane(Tab tab) {
            return tabToPaneMap.get(tab);
        }

        public String getId(Tab tab) {
            return Integer.toString(tab.hashCode());
        }

        public Tab getTab(String id) {
            return idToTabMap.get(id);
        }

        public DataFormat getDragAndDropFormat() {
            return dragAndDropFormat;
        }

        private ObservableList<Tab> getGlobalTabList() {
            return globalTabList;
        }

        public Tab getSelectedTab() {
            return selectedTab;
        }

        public void setSelectedTab(Tab selectedTab) {
            this.selectedTab = selectedTab;
            logger.trace(() -> "Selected Tab: " + ((selectedTab == null) ? "null" : selectedTab.toString() + " " + getId(selectedTab) + " " + tabToPaneMap.get(selectedTab)));
        }

        public void setMovingTab(boolean movingTab) {
            this.movingTab = movingTab;
        }

        public void clearAllTabs() {
            tabToPaneMap.values()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList())
                    .forEach(p -> p.getTabs().clear());
        }
    }
}
