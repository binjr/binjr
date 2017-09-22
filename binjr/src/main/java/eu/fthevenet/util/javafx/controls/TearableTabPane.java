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

import eu.fthevenet.binjr.dialogs.StageAppearanceManager;
import javafx.application.Platform;
import javafx.collections.*;
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
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A TabPane container with a button to add a new tab
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane {
    private static final Logger logger = LogManager.getLogger(TearableTabPane.class);

    private boolean tearable;
    private boolean reorderable;
    private Function<ActionEvent, Optional<Tab>> newTabFactory = (e) -> Optional.of(new Tab());
    private EventHandler<ActionEvent> onNewTabAction;
    private final Map<Tab, TabState> tearableTabMap = new HashMap<>();
    private final ObservableSet<Tab> tabsSet = FXCollections.observableSet(tearableTabMap.keySet());
    private final TabPaneManager manager;


    public ObservableSet<Tab> getTearableTabs() {
        return tabsSet;
    }

    public TearableTabPane() {
        this(new TabPaneManager(), false, false, (Tab[]) null);
    }


    public TearableTabPane(TabPaneManager manager, boolean reorderable, boolean tearable, Tab... tabs) {
        super(tabs);
        this.manager = manager;
        this.tearable = tearable;
        this.reorderable = reorderable;


        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.manager.setSelectedTab(newValue);
            }
        });

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
            logger.trace("Tearable tabs in tab pane: " + tearableTabMap.keySet().stream().map(tab -> tab.getText() == null ? tab.toString() : tab.getText()).reduce((s, s2) -> s + " " + s2).orElse("null"));
        });

        this.setOnDragDetected(
                (MouseEvent event) -> {
                    if (!this.tearable) {
                        return;
                    }
                    if (event.getSource() instanceof TabPane) {
                        Tab currentTab = this.getSelectionModel().getSelectedItem();
                        SnapshotParameters snapshotParams = new SnapshotParameters();
                        WritableImage snapshot = currentTab.getContent().snapshot(snapshotParams, null);
                        Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.put(manager.getDragAndDropFormat(), manager.getId(currentTab));
                        db.setDragView(snapshot, -5, -5);
                        db.setContent(clipboardContent);
                        manager.startDragAndDrop();
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
            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    manager.setSelectedTab(this.getSelectionModel().getSelectedItem());
                }
            });
        });

        // Prepare to change the button on screen position if the tearableTabMap side changes
        sideProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                positionNewTabButton();
            }
        });
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
        logger.debug("headersRegion.getHeight() = " + headersRegion.getHeight());
        logger.debug("headersRegion.getPrefHeight = " + headersRegion.getPrefHeight());
        newTabButton.getStyleClass().add("add-tab-button");
        SVGPath icon = new SVGPath();
        icon.setContent("m 31.25,54.09375 0,2.4375 -2.46875,0 0,0.375 2.46875,0 0,2.46875 0.375,0 0,-2.46875 2.46875,0 0,-0.375 -2.46875,0 0,-2.4375 -0.375,0 z");
        icon.getStyleClass().add("add-tab-button-icon");
        newTabButton.setGraphic(icon);
        newTabButton.setAlignment(Pos.CENTER);
        if (onNewTabAction != null) {
            newTabButton.setOnAction(onNewTabAction);
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
                newTabButton.translateXProperty().bind(
                        headersRegion.widthProperty()
                );
                break;
            case LEFT:
                newTabButton.translateXProperty().bind(
                        tabHeaderBg.widthProperty()
                                .subtract(headersRegion.widthProperty())
                                .subtract(newTabButton.widthProperty())
                );
                break;
            case BOTTOM:
                newTabButton.translateXProperty().bind(
                        tabHeaderBg.widthProperty()
                                .subtract(headersRegion.widthProperty())
                                .subtract(newTabButton.widthProperty())
                );
                break;
            case RIGHT:
                newTabButton.translateXProperty().bind(
                        headersRegion.widthProperty()
                );
                break;
            default:
                throw new IllegalStateException("Invalid value for side enum");
        }
    }

    public Function<ActionEvent, Optional<Tab>> getNewTabFactory() {
        return newTabFactory;
    }

    public void setNewTabFactory(Function<ActionEvent, Optional<Tab>> newTabFactory) {
        this.newTabFactory = newTabFactory;
    }

    public EventHandler<ActionEvent> getOnNewTabAction() {
        return onNewTabAction;
    }

    public void setOnNewTabAction(EventHandler<ActionEvent> onNewTabAction) {
        this.onNewTabAction = onNewTabAction;
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
        stage.setTitle("binjr");
        Point p = MouseInfo.getPointerInfo().getLocation();
        stage.setX(p.getX());
        stage.setY(p.getY());
        detachedTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.getList().size() == 0) {
                stage.close();
            }
        });
        StageAppearanceManager.getInstance().register(stage);
        stage.show();
        detachedTabPane.getSelectionModel().select(tab);
        stage.setOnCloseRequest(event -> {
            detachedTabPane.getTabs().removeAll(detachedTabPane.getTabs());
        });
    }

    public boolean isTearable() {
        return tearable;
    }

    public void setTearable(boolean tearable) {
        this.tearable = tearable;
    }

    public boolean isReorderable() {
        return reorderable;
    }

    public void setReorderable(boolean reorderable) {
        this.reorderable = reorderable;
    }

    public Tab getSelectedTab() {
        return manager.getSelectedTab();
    }

    public TabPane getSelectedTabPane() {
        if (manager.getSelectedTab() == null || manager.tabToPaneMap.get(manager.getSelectedTab()) == null) {
            return this;
        }
        return manager.tabToPaneMap.get(getSelectedTab());
    }

    public ObservableList<Tab> getGlobalTabs() {
        return manager.getGlobalTabList();
    }

    public void clearAllTabs() {
        manager.clearAllTabs();
    }

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
            logger.trace(() -> {
                if (selectedTab == null) {
                    return "Selected tab: null)";
                }
                return "Selected Tab: " + selectedTab.toString() + " " + getId(selectedTab) + " " + tabToPaneMap.get(selectedTab);
            });
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
