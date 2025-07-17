/*
 *    Copyright 2017-2025 Frederic Thevenet
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
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


/**
 * A TabPane container with a button to add a new tab that also supports tearing away tabs into a separate window.
 * <p>It relies on the {@link TabPaneManager} class to keep track of all tabs spread over many windows and {@link TabPane} instances</p>
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane implements AutoCloseable {
    private static final PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    private static final Logger logger = Logger.create(TearableTabPane.class);
    public static final Object DETACHED_STAGE_MARKER = new Object() {
        @Override
        public String toString() {
            return "DETACHED_STAGE_MARKER";
        }
    };

    private boolean tearable;
    private boolean reorderable;
    private Supplier<Optional<Tab>> newTabFactory = () -> Optional.of(new Tab());
    private final Map<Tab, TabState> tearableTabMap = new HashMap<>();
    private final TabPaneManager manager;

    private EventHandler<WindowEvent> onOpenNewWindow;
    private EventHandler<WindowEvent> onClosingWindow;

    private EventHandler<DragEvent> onDragDroppedOnTabArea;
    private EventHandler<DragEvent> onDragOverOnTabArea;
    private EventHandler<DragEvent> onDragExitedTabArea;
    private EventHandler<DragEvent> onDragEnteredTabArea;

    private final BindingManager bindingManager = new BindingManager();
    private final Property<StageStyle> detachedStageStyle;
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(true);
    private ContextMenu newTabContextMenu;

    private final BooleanProperty dragAndDropInProgress = new SimpleBooleanProperty(false);
    private final ReadOnlyBooleanWrapper hasSibling = new ReadOnlyBooleanWrapper(false);
    static private final BooleanProperty closeIfEmpty = new SimpleBooleanProperty(true);
    ;

    /**
     * Initializes a new instance of the {@link TearableTabPane} class.
     */
    public TearableTabPane() {
        this(new TabPaneManager(), false, false, StageStyle.DECORATED, (Tab[]) null);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        var skin = new TearableTabPaneSkin(this);
        if (newTabContextMenu != null) {
            skin.newTabButton.setContextMenu(newTabContextMenu);
        }
        skin.newTabButton.setOnAction(bindingManager.registerHandler(event -> newTabFactory.get().ifPresent(newTab -> {
            getTabs().add(newTab);
            this.getSelectionModel().select(newTab);
        })));
        skin.closeSplitPane.setOnAction(bindingManager.registerHandler(event -> {
            var tearableTabPane = findParentTearableTabPane((Button) event.getSource());
            if (tearableTabPane != null) {
                tearableTabPane.closePane();
            }
            event.consume();
        }));
        skin.splitRightButton.setOnAction(bindingManager.registerHandler(event -> {
            split(Orientation.HORIZONTAL);
            event.consume();
        }));
        skin.splitDownButton.setOnMouseClicked(bindingManager.registerHandler(event -> {
            split(Orientation.VERTICAL);
            event.consume();
        }));
        skin.dropZone.setOnDragDropped(bindingManager.registerHandler(onDragDroppedOnTabArea));
        skin.dropZone.setOnDragOver(bindingManager.registerHandler(onDragOverOnTabArea));
        skin.dropZone.setOnDragExited(bindingManager.registerHandler(onDragExitedTabArea));
        skin.dropZone.setOnDragEntered(bindingManager.registerHandler(onDragEnteredTabArea));
        skin.newPaneDropZone.managedProperty().bind(dragAndDropInProgress);
        skin.newPaneDropZone.visibleProperty().bind(dragAndDropInProgress);

        skin.addWorksheetLabel.setOnMouseClicked(bindingManager.registerHandler(event -> newTabFactory.get().ifPresent(newTab -> {
            getTabs().add(newTab);
            this.getSelectionModel().select(newTab);
        })));
        skin.addWorksheetLabel.setOnDragDropped(bindingManager.registerHandler(onDragDroppedOnTabArea));
        skin.addWorksheetLabel.setOnDragOver(bindingManager.registerHandler(onDragOverOnTabArea));

        skin.addWorksheetLabel.getGraphic().setOnDragExited(bindingManager.registerHandler(onDragExitedTabArea));
        skin.addWorksheetLabel.getGraphic().setOnDragEntered(bindingManager.registerHandler(onDragEnteredTabArea));
        this.getScene().addEventFilter(KeyEvent.KEY_PRESSED, bindingManager.registerHandler(e -> {
            if (e.getCode() == KeyCode.ALT) {
                skin.splitRightButton.setVisible(false);
            }
        }));
        this.getScene().addEventFilter(KeyEvent.KEY_RELEASED, bindingManager.registerHandler(e -> {
            if (e.getCode() == KeyCode.ALT) {
                skin.splitRightButton.setVisible(true);
            }
        }));
        return skin;
    }

    private void closePane() {
        var splitPane = findParentSplitPane(this);
        if (splitPane == null) {
            return;
        }
        final TearableTabPane sibling = findSibling(splitPane, this);
        if (sibling == null) {
            this.hasSibling.set(false);
            return;
        }
        splitPane.getItems().remove(this);
        reduceSplitPane(splitPane);
        balanceSplitPanesDividers(splitPane);
    }

    private TearableTabPane findSibling(SplitPane sp, TearableTabPane tabPaneToRemove) {
        for (final Node sibling : sp.getItems()) {
            if (tabPaneToRemove != sibling && sibling instanceof TearableTabPane siblingSplitPane) {
                return siblingSplitPane;
            }
        }
        for (final Node sibling : sp.getItems()) {
            if (sibling instanceof SplitPane siblingSplitPane) {
                return findSibling(siblingSplitPane, tabPaneToRemove);
            }
        }
        return null;
    }

    private void reduceSplitPane(SplitPane sp) {
        if (sp.getItems().size() != 1) {
            return;
        }
        final Node content = sp.getItems().getFirst();
        final SplitPane parent = findParentSplitPane(sp);
        if (parent != null && parent.getItems().contains(sp)) {
            int index = parent.getItems().indexOf(sp);
            parent.getItems().remove(sp);
            parent.getItems().add(index, content);
            reduceSplitPane(parent);
        } else {
            if (sp.getItems().getFirst() instanceof TearableTabPane p) {
                p.hasSibling.set(false);
            }
        }
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

        bindingManager.attachListener(this.getTabs(), ((InvalidationListener) observable -> {
            empty.setValue(this.getTabs().isEmpty());
        }));

        bindingManager.attachListener(this.getTabs(), ((ListChangeListener<Tab>) c -> {
            if (c.getList().isEmpty()) {
                if (TearableTabPane.isCloseIfEmpty()) {
                    this.closePane();
                    if (!this.getHasSibling() && NodeUtils.getStage(this) instanceof TabPaneDetachedStage paneStage) {
                        paneStage.close();
                    }
                }
            }
        }));

        Platform.runLater(() -> {
            var scene = this.getScene();
            if (scene != null) {
                if (scene.getWindow() instanceof Stage stage) {
                    bindingManager.attachListener(stage.focusedProperty(),
                            (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                                if (newValue) {
                                    manager.setSelectedTab(this.getSelectionModel().getSelectedItem());
                                }
                            });
                }
            }
        });
    }

    public void detachTab(Tab t) {
        detachTab(t, null);
    }

    public void detachTab(Tab t, Orientation orientation) {
        Objects.requireNonNull(t, "Tab to detach cannot be null");
        logger.trace(() -> "Detaching tab " + t.getId() + " " + t.getText());
        manager.setMovingTab(true);
        try {
            tearOffTab(t, orientation);
        } finally {
            manager.setMovingTab(false);
        }
    }

    public void split(Orientation orientation) {
        logger.trace(() -> "Split tab pane " + orientation);
        tearOffTab(null, orientation);
    }

    /**
     * Returns the factory for creating new tabs
     *
     * @return the factory for creating new tabs
     */
    public Supplier<Optional<Tab>> getNewTabFactory() {
        return newTabFactory;
    }

    /**
     * Sets the factory for creating new tabs
     *
     * @param newTabFactory the factory for creating new tabs
     */
    public void setNewTabFactory(Supplier<Optional<Tab>> newTabFactory) {
        this.newTabFactory = newTabFactory;
    }

    public void setNewTabContextMenu(ContextMenu menu) {
        this.newTabContextMenu = menu;
    }

    /**
     * Returns true if tabs can be teared away from the pane, false otherwise.
     *
     * @return true if tabs can be teared away from the pane, false otherwise.
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
     * Returns the stage containing the tab currently selected.
     *
     * @return the stage containing the tab currently selected.
     */
    public Stage getSelectedTabStage() {
        return NodeUtils.getStage(getSelectedTabPane());
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

    public void setOnDragDroppedOnTabArea(EventHandler<DragEvent> onDragDroppedOnTabArea) {
        this.onDragDroppedOnTabArea = onDragDroppedOnTabArea;
    }

    public void setOnDragOverOnTabArea(EventHandler<DragEvent> onDragOverOnTabArea) {
        this.onDragOverOnTabArea = onDragOverOnTabArea;
    }

    public void setOnDragExitedTabArea(EventHandler<DragEvent> onDragExitedTabArea) {
        this.onDragExitedTabArea = onDragExitedTabArea;
    }

    public void setOnDragEnteredTabArea(EventHandler<DragEvent> onDragEnteredTabArea) {
        this.onDragEnteredTabArea = onDragEnteredTabArea;
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

    public boolean getDragAndDropInProgress() {
        return dragAndDropInProgress.get();
    }

    public void setDragAndDropInProgress(boolean dragAndDropInProgress) {
        this.dragAndDropInProgress.set(dragAndDropInProgress);
    }

    public BooleanProperty dragAndDropInProgressProperty() {
        return dragAndDropInProgress;
    }

    public boolean getHasSibling() {
        return hasSibling.get();
    }

    public ReadOnlyBooleanProperty hasSiblingProperty() {
        return hasSibling.getReadOnlyProperty();
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

    public static boolean isCloseIfEmpty() {
        return closeIfEmpty.get();
    }

    public static void setCloseIfEmpty(boolean val) {
        closeIfEmpty.set(val);
    }

    public static BooleanProperty closeIfEmptyProperty() {
        return closeIfEmpty;
    }

    public enum TabPosition {
        INSIDE,
        DETACHED,
        UP,
        RIGHT,
        BOTTOM,
        LEFT
    }

    private void tearOffTab(Tab tab) {
        tearOffTab(tab, null);
    }

    private void tearOffTab(@Nullable Tab tab, @Nullable Orientation orientation) {
        TearableTabPane detachedTabPane = new TearableTabPane(this.manager, isReorderable(), true, this.getDetachedStageStyle());
        detachedTabPane.setId("tearableTabPane");
        detachedTabPane.setOnDragDroppedOnTabArea(this.onDragDroppedOnTabArea);
        detachedTabPane.setOnDragOverOnTabArea(this.onDragOverOnTabArea);
        detachedTabPane.setOnDragExitedTabArea(this.onDragExitedTabArea);
        detachedTabPane.setOnDragEnteredTabArea(this.onDragEnteredTabArea);
        detachedTabPane.dragAndDropInProgressProperty().bind(this.dragAndDropInProgress);
        detachedTabPane.setNewTabFactory(this.getNewTabFactory());
        detachedTabPane.setNewTabContextMenu(this.newTabContextMenu);
        detachedTabPane.setOnOpenNewWindow(this.onOpenNewWindow);

        if (orientation == null) {
            Pane root = new AnchorPane(detachedTabPane);
            AnchorPane.setBottomAnchor(detachedTabPane, 0.0);
            AnchorPane.setLeftAnchor(detachedTabPane, 0.0);
            AnchorPane.setRightAnchor(detachedTabPane, 0.0);
            AnchorPane.setTopAnchor(detachedTabPane, 0.0);
            final Scene scene = new Scene(root, this.getWidth(), this.getHeight());
            Stage stage = new TabPaneDetachedStage();
            stage.setScene(scene);
            Point p = MouseInfo.getPointerInfo().getLocation();
            stage.setX(p.getX());
            stage.setY(p.getY());
            if (onOpenNewWindow != null) {
                onOpenNewWindow.handle(new WindowEvent(stage, WindowEvent.WINDOW_SHOWING));
            }
            stage.initStyle(this.getDetachedStageStyle());
            stage.show();
            stage.setOnCloseRequest(bindingManager.registerHandler(event -> {
                manager.tabToPaneMap.values().stream()
                        .distinct()
                        .filter(tabPane -> stage.equals(NodeUtils.getStage(tabPane)))
                        .toList()
                        .forEach(TearableTabPane::close);
                if (onClosingWindow != null) {
                    onClosingWindow.handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                }
            }));
        } else {
            var splitPane = findParentSplitPane(this);
            if (splitPane != null) {
                if (splitPane.getItems().size() == 1) {
                    splitPane.setOrientation(orientation);
                }
                if (splitPane.getOrientation() == orientation) {
                    splitPane.getItems().add(detachedTabPane);
                    balanceSplitPanesDividers(splitPane);
                } else {
                    int indexTabPane = splitPane.getItems().indexOf(TearableTabPane.this);
                    splitPane.getItems().remove(TearableTabPane.this);
                    SplitPane innerSplitpane = new SplitPane();
                    splitPane.getItems().add(indexTabPane, innerSplitpane);
                    innerSplitpane.setOrientation(orientation);
                    innerSplitpane.getItems().add(TearableTabPane.this);
                    innerSplitpane.getItems().add(detachedTabPane);
                    innerSplitpane.getItems().forEach(n -> {
                        if (n instanceof TearableTabPane p) {
                            p.hasSibling.set(true);
                        }
                    });
                }
                splitPane.getItems().forEach(n -> {
                    if (n instanceof TearableTabPane p) {
                        p.hasSibling.set(true);
                    }
                });
            } else {
                if (this.getParent() instanceof Pane pane) {
                    splitPane = new SplitPane();
                    splitPane.setOrientation(orientation);
                    int index = pane.getChildren().indexOf(TearableTabPane.this);
                    pane.getChildren().remove(TearableTabPane.this);
                    pane.getChildren().add(index, splitPane);
                    splitPane.getItems().add(TearableTabPane.this);
                    splitPane.getItems().add(detachedTabPane);
                    AnchorPane.setTopAnchor(splitPane, 0.0);
                    AnchorPane.setRightAnchor(splitPane, 0.0);
                    AnchorPane.setBottomAnchor(splitPane, 0.0);
                    AnchorPane.setLeftAnchor(splitPane, 0.0);
                    splitPane.getItems().forEach(n -> {
                        if (n instanceof TearableTabPane p) {
                            p.hasSibling.set(true);
                        }
                    });
                } else {
                    logger.error("Parent control is not a Pane");
                    // throw new UnsupportedOperationException("Parent control is not a SplitPane");
                }
            }
        }
        if (tab != null) {
            this.getTabs().remove(tab);
            detachedTabPane.getTabs().add(tab);
            detachedTabPane.getSelectionModel().select(tab);
        }
    }


    private SplitPane findParentSplitPane(Node node) {
        if (node == null) {
            return null;
        }
        var parent = node.getParent();
        if (parent == null) {
            return null;
        }
        if (parent instanceof SplitPane splitPane) {
            return splitPane;
        } else {
            return findParentSplitPane(parent);
        }
    }

    private TearableTabPane findParentTearableTabPane(Node node) {
        if (node == null) {
            return null;
        }
        var parent = node.getParent();
        if (parent == null) {
            return null;
        }
        if (parent instanceof TearableTabPane splitTearableTabPanePane) {
            return splitTearableTabPanePane;
        } else {
            return findParentTearableTabPane(parent);
        }
    }

    private void balanceSplitPanesDividers(SplitPane splitPane) {
        int itemCount = splitPane.getItems().size();
        double[] positions = new double[itemCount];
        var divFactor = 1d / itemCount;
        for (int i = 0; i < positions.length; i++) {
            positions[i] = (i + 1) * divFactor;
        }
        splitPane.setDividerPositions(positions);
    }

    @Override
    public void close() {
        logger.debug(() -> "Closing down TearableTabPane instance");
        getTabs().forEach(tab -> tab.setContextMenu(null));
        this.getTabs().clear();
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
        private final ObservableMap<Tab, TearableTabPane> tabToPaneMap;
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

        public void addTab(Tab tab, TearableTabPane pane) {
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

        public void setSelectedTab(Tab selected) {
            this.selectedTab = selected;
            logger.debug(() -> "Selected Tab: " + (selected == null ? "null" :
                    (selected instanceof EditableTab editable ? editable.getName() :
                            selected.getText())));
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

    private static class TabPaneDetachedStage extends Stage {
    }
}
