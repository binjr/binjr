/*
 *    Copyright 2017-2023 Frederic Thevenet
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

package eu.binjr.common.javafx.controls;


import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.logging.Logger;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;


/**
 * A TabPane container with a button to add a new tab that also supports tearing away tabs into a separate window.
 * <p>It relies on the {@link TabPaneManager} class to keep track of all tabs spread over many windows and {@link TabPane} instances</p>
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane implements AutoCloseable {
    private static final PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    private static final Logger logger = Logger.create(TearableTabPane.class);
    private boolean tearable;
    private boolean reorderable;
    private Function<ActionEvent, Optional<Tab>> newTabFactory = (e) -> Optional.of(new Tab());
    private final Map<Tab, TabState> tearableTabMap = new HashMap<>();
    private final TabPaneManager manager;
    private EventHandler<ActionEvent> onAddNewTab;
    private EventHandler<WindowEvent> onOpenNewWindow;
    private EventHandler<WindowEvent> onClosingWindow;
    private final BindingManager bindingManager = new BindingManager();
    private final Property<StageStyle> detachedStageStyle;
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(true);
    private ContextMenu newTabContextMenu;


    /**
     * Initializes a new instance of the {@link TearableTabPane} class.
     */
    public TearableTabPane() {
        this(new TabPaneManager(), false, false, StageStyle.DECORATED, (Tab[]) null);
    }

    /**
     * Initializes a new instance of the {@link TearableTabPane} class.
     *
     * @param manager     the {@link TabPaneManager} manager instance
     * @param reorderable true if tabs can be reordered, false otherwise.
     * @param tearable    true if tabs are teared away from the pane, false otherwise.
     * @param style       the {@link StageStyle} for the detached tab window.
     * @param tabs        tabs to attached to the TabPane.
     */
    public TearableTabPane(TabPaneManager manager, boolean reorderable, boolean tearable, StageStyle style, Tab... tabs) {
        super(tabs);
        this.manager = manager;
        this.tearable = tearable;
        this.reorderable = reorderable;
        this.detachedStageStyle = new SimpleObjectProperty<>(style);
        bindingManager.attachListener(this.getSelectionModel().selectedItemProperty(), (ChangeListener<Tab>)
                (observable, oldValue, newValue) -> this.manager.setSelectedTab(newValue)
        );


        bindingManager.attachListener(this.getTabs(), (ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab newTab : c.getAddedSubList()) {
                        this.tearableTabMap.put(newTab, new TabState(true));
                        this.manager.addTab(newTab, this);
                        Node target = newTab.getGraphic();
                        if (target.getOnDragEntered() == null) {
                            target.setOnDragEntered(bindingManager.registerHandler(event -> {
                                if (event.getDragboard().hasContent(manager.getDragAndDropFormat())) {
                                    findTabHeader(newTab).ifPresent(node -> node.pseudoClassStateChanged(HOVER_PSEUDO_CLASS, true));
                                }
                            }));
                        }
                        if (target.getOnDragExited() == null) {
                            target.setOnDragExited(bindingManager.registerHandler(event -> {
                                if (event.getDragboard().hasContent(manager.getDragAndDropFormat())) {
                                    findTabHeader(newTab).ifPresent(node -> node.pseudoClassStateChanged(HOVER_PSEUDO_CLASS, false));
                                }
                            }));
                        }
                        if (target.getOnDragDropped() == null) {
                            target.setOnDragDropped(bindingManager.registerHandler(event -> {
                                Dragboard db = event.getDragboard();
                                if (db.hasContent(manager.getDragAndDropFormat())) {
                                    logger.trace(() -> "setOnDragDropped fired");
                                    if (manager.completeDragAndDrop()) {
                                        String id = (String) db.getContent(manager.getDragAndDropFormat());
                                        Tab draggedTab = manager.getTab(id);
                                        if (draggedTab != null) {
                                            TearableTabPane draggedTabPane = (TearableTabPane) manager.getTabPane(draggedTab);
                                            if (draggedTabPane.isReorderable()) {
                                                manager.setMovingTab(true);
                                                try {
                                                    var draggedIndex = draggedTabPane.getTabs().indexOf(draggedTab);
                                                    var droppedTabPane = manager.getTabPane(newTab);
                                                    var dropIndex = droppedTabPane.getTabs().indexOf(newTab);
                                                    if (!newTab.equals(draggedTab)) {
                                                        draggedTabPane.getTabs().remove(draggedIndex);
                                                        droppedTabPane.getTabs().add(dropIndex, draggedTab);
                                                        droppedTabPane.getSelectionModel().clearAndSelect(dropIndex);
                                                    }
                                                } finally {
                                                    manager.setMovingTab(false);
                                                }
                                            } else {
                                                logger.debug(() -> "Tabs on this pane cannot be reordered");
                                            }
                                        } else {
                                            logger.debug(() -> "Failed to retrieve targetTab with id " + (id != null ? id : "null"));
                                        }
                                    }
                                    event.consume();
                                }
                            }));
                        }
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
        this.setOnDragDetected(bindingManager.registerHandler((MouseEvent event) -> {
                    if (!this.tearable) {
                        return;
                    }
                    if (event.getSource() instanceof TabPane) {
                        Tab currentTab = this.getSelectionModel().getSelectedItem();
                        if (currentTab != null) {
                            Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                            ClipboardContent clipboardContent = new ClipboardContent();
                            clipboardContent.put(manager.getDragAndDropFormat(), manager.getId(currentTab));
                            db.setDragView(NodeUtils.scaledSnapshot(currentTab.getContent()));
                            db.setContent(clipboardContent);
                            manager.startDragAndDrop();
                        }
                    }
                    event.consume();
                }
        ));
        this.setOnDragOver(bindingManager.registerHandler(event -> {
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
        }));
        this.setOnDragDone(bindingManager.registerHandler(
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
        ));
        this.setOnDragDropped(bindingManager.registerHandler(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(manager.getDragAndDropFormat())) {
                logger.trace(() -> "setOnDragDropped fired");
                if (manager.completeDragAndDrop()) {
                    String id = (String) db.getContent(manager.getDragAndDropFormat());
                    Tab t = manager.getTab(id);
                    if (t != null) {
                        TabPane p = manager.getTabPane(t);
                        if (!this.equals(p)) {
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
                    } else {
                        logger.debug(() -> "Failed to retrieve tab with id " + (id != null ? id : "null"));
                    }
                }
                event.consume();
            }
        }));
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

        bindingManager.attachListener(this.getTabs(), ((InvalidationListener) observable -> {
            empty.setValue(this.getTabs().size() == 0);
        }));
    }


    public void detachTab(Tab t) {
        Objects.requireNonNull(t, "Tab to detach cannot be null");
        logger.trace(() -> "Detaching tab " + t.getId() + " " + t.getText());
        manager.setMovingTab(true);
        try {
            tearOffTab(t);
        } finally {
            manager.setMovingTab(false);
        }
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

    public void setNewTabContextMenu(ContextMenu menu) {
        this.newTabContextMenu = menu;
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

    public boolean isEmpty() {
        return empty.get();
    }

    public ReadOnlyBooleanProperty emptyProperty() {
        return empty.getReadOnlyProperty();
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
        newTabButton.visibleProperty().bind(manager.newTabButtonVisible);
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
        if (newTabContextMenu != null) {
            newTabButton.setContextMenu(newTabContextMenu);
        }
        if (onAddNewTab != null) {
            newTabButton.setOnAction(bindingManager.registerHandler(onAddNewTab));
        } else {
            newTabButton.setOnAction(bindingManager.registerHandler(event -> {
                newTabFactory.apply(event).ifPresent(newTab -> {
                    getTabs().add(newTab);
                    this.getSelectionModel().select(newTab);
                });
            }));
        }
        tabHeaderBg.getChildren().add(newTabButton);
        StackPane.setAlignment(newTabButton, Pos.CENTER_LEFT);
        switch (getSide()) {
            case TOP, BOTTOM -> newTabButton.translateXProperty().bind(
                    headersRegion.widthProperty()
                            .add(Bindings.createDoubleBinding(() -> headerArea.getInsets().getLeft(), headerArea.insetsProperty()))
            );
            case LEFT, RIGHT -> newTabButton.translateXProperty().bind(
                    tabHeaderBg.widthProperty()
                            .subtract(headersRegion.widthProperty())
                            .subtract(newTabButton.widthProperty())
                            .subtract(Bindings.createDoubleBinding(() -> headerArea.getInsets().getTop(), headerArea.insetsProperty()))
            );
            default -> throw new IllegalStateException("Invalid value for side enum");
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

    private Optional<Node> findTabHeader(Tab t) {
        Styleable styleable = t.getGraphic();
        while (styleable != null && !styleable.getStyleClass().contains("tab")) {
            styleable = styleable.getStyleableParent();
        }
        if (styleable instanceof Node node) {
            return Optional.of(node);
        } else {
            return Optional.empty();
        }
    }

    private void tearOffTab(Tab tab) {
        TearableTabPane detachedTabPane = new TearableTabPane(this.manager, isReorderable(), true, this.getDetachedStageStyle());
        detachedTabPane.setId("tearableTabPane");
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
                detachedTabPane.close();
            }
        });
        if (onOpenNewWindow != null) {
            onOpenNewWindow.handle(new WindowEvent(stage, WindowEvent.WINDOW_SHOWING));
        }
        stage.initStyle(this.getDetachedStageStyle());
        stage.show();
        detachedTabPane.getSelectionModel().select(tab);
        stage.setOnCloseRequest(bindingManager.registerHandler(event -> {
            detachedTabPane.getTabs().removeAll(detachedTabPane.getTabs());
        }));
    }

    @Override
    public void close() {
        getTabs().forEach(tab -> tab.setContextMenu(null));
        logger.trace(() -> "Closing down TearableTabPane instance");
        bindingManager.close();
    }

    public StageStyle getDetachedStageStyle() {
        return detachedStageStyle.getValue();
    }

    public Property<StageStyle> detachedStageStyleProperty() {
        return detachedStageStyle;
    }

    public void setDetachedStageStyle(StageStyle detachedStageStyle) {
        this.detachedStageStyle.setValue(detachedStageStyle);
    }

    public boolean isNewTabButtonVisible() {
        return manager.newTabButtonVisible.get();
    }

    public BooleanProperty newTabButtonVisibleProperty() {
        return manager.newTabButtonVisible;
    }

    public void setNewTabButtonVisible(boolean newTabButtonVisible) {
        manager.newTabButtonVisible.set(newTabButtonVisible);
    }

    /**
     * Represents the state of a single tab
     */
    private record TabState(boolean attached) {
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
        private final BooleanProperty newTabButtonVisible = new SimpleBooleanProperty(true);


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
            logger.trace(() -> "Removing tab " + tab.getText() + " (movingTab=" + movingTab + ")");
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
            logger.trace(() -> "Selected Tab: " + ((selectedTab == null) ? "null" : selectedTab + " " + getId(selectedTab) + " " + tabToPaneMap.get(selectedTab)));
        }

        public void setMovingTab(boolean movingTab) {
            this.movingTab = movingTab;
        }

        public void clearAllTabs() {
            tabToPaneMap.values()
                    .stream()
                    .distinct()
                    .toList()
                    .forEach(p -> p.getTabs().clear());
        }


    }
}
