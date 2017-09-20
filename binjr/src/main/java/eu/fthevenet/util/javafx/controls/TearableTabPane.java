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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
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
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * A TabPane container with a button to add a new tab
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane {
    private static final Logger logger = LogManager.getLogger(TearableTabPane.class);
    private static final DataFormat TEARABLE_TAB_FORMAT = new DataFormat("TearableTabFormat");
    ;
    private boolean tearable;
    private boolean draggable;
    private Function<ActionEvent, Optional<Tab>> newTabFactory = (e) -> Optional.of(new Tab());
    private EventHandler<ActionEvent> onNewTabAction;

    private Tab currentTab;
    private final List<Tab> originalTabs = new ArrayList<>();

    private final Map<Integer, Tab> tapTransferMap = new HashMap<>();
    private final Map<Tab, TabState> tearableTabMap = new HashMap<>();
    private final ObservableSet<Tab> tabsSet = FXCollections.observableSet(tearableTabMap.keySet());

    public ObservableSet<Tab> getTearableTabs() {
        return tabsSet;
    }

    public TearableTabPane() {
        this(false, false, (Tab[]) null);
    }


    public TearableTabPane(boolean draggable, boolean tearable, Tab... tabs) {
        super(tabs);
        this.tearable = tearable;
        this.draggable = draggable;
        originalTabs.addAll(this.getTabs());
        for (int i = 0; i < this.getTabs().size(); i++) {
            tapTransferMap.put(i, this.getTabs().get(i));
        }

        this.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(t -> {
                        this.tearableTabMap.put(t, new TabState(true));
                    });
                }
                if (c.wasRemoved()) {
                    c.getRemoved().forEach((this.tearableTabMap::remove));
                }
            }
            //   logger.trace("Tearable tabs in tab pane: " + tearableTabMap.keySet().stream().map(tab -> tab.getText() == null ? tab.toString() : tab.getText()).reduce((s, s2) -> s + " " + s2).orElse("null"));
        });

        this.setOnDragDetected(
                (MouseEvent event) -> {
                    if (!this.tearable) {
                        return;
                    }
                    if (event.getSource() instanceof TabPane) {
                        Pane rootPane = (Pane) this.getScene().getRoot();
//                        rootPane.setOnDragOver((DragEvent event1) -> {
//                            event1.acceptTransferModes(TransferMode.ANY);
//                            event1.consume();
//                        });
                        currentTab = this.getSelectionModel().getSelectedItem();
                        SnapshotParameters snapshotParams = new SnapshotParameters();
//                      snapshotParams.setTransform(Transform.scale(0.4,0.4));
                        WritableImage snapshot = currentTab.getContent().snapshot(snapshotParams, null);
                        Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.put(TEARABLE_TAB_FORMAT, "TEARABLE_TAB_FORMAT");
                        db.setDragView(snapshot, 40, 40);
                        db.setContent(clipboardContent);
                        ReferenceClipboard.getInstance().put(TEARABLE_TAB_FORMAT, currentTab);

                    }
                    event.consume();
                }
        );

        this.setOnDragOver(event -> {
            if (!this.tearable) {
                return;
            }
            Dragboard db = event.getDragboard();
            if (db.hasContent(TEARABLE_TAB_FORMAT)) {
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
                    if (db.hasContent(TEARABLE_TAB_FORMAT)) {
                        logger.trace(() -> "setOnDragDone fired");

                        openTabInStage(currentTab);

                        this.getTabs().remove(currentTab);

                        //  this.setCursor(Cursor.DEFAULT);
                    }
                    ReferenceClipboard.getInstance().clear();
                    event.consume();
                }
        );


        this.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(TEARABLE_TAB_FORMAT)) {
                logger.trace(() -> "setOnDragDropped fired");
                try {

                    Tab t = (Tab) ReferenceClipboard.getInstance().get(TEARABLE_TAB_FORMAT);
                    if (t != null) {

                        this.getTabs().add(t);
                    }
                } catch (Exception e) {
                    logger.error("error", e);
                } finally {
                    db.clear();
                }
                event.consume();
            }
        });

        Platform.runLater(this::positionNewTabButton);

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


    /**
     * Opens the content of the given {@link Tab} in a separate Stage. While the content is removed from the {@link Tab} it is
     * added to the root of a new {@link Stage}. The Window title is set to the name of the {@link Tab};
     *
     * @param tab The {@link Tab} to get the content from.
     */
    public void openTabInStage(final Tab tab) {

        int originalTab = originalTabs.indexOf(tab);
        tapTransferMap.remove(originalTab);
//        Node content = tab.getContent();
//        if (content == null) {
//            throw new IllegalArgumentException("Can not detach Tab '" + tab.getText() + "': content is empty (null).");
//        }
        //tab.setContent(null);
        TearableTabPane detachedTabPane = new TearableTabPane(true, true, tab);
        detachedTabPane.setNewTabFactory(this.getNewTabFactory());

        Pane root = new AnchorPane(detachedTabPane);
        AnchorPane.setBottomAnchor(detachedTabPane, 0.0);
        AnchorPane.setLeftAnchor(detachedTabPane, 0.0);
        AnchorPane.setRightAnchor(detachedTabPane, 0.0);
        AnchorPane.setTopAnchor(detachedTabPane, 0.0);
        final Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());


        // final Scene scene = new Scene((Parent) content, content.prefWidth(0), content.prefHeight(0));
        Stage stage = new Stage();
        stage.setScene(scene);


        stage.setTitle("binjr");
        Point p = MouseInfo.getPointerInfo().getLocation(); // MouseRobot.getMousePosition();
        stage.setX(p.getX());
        stage.setY(p.getY());
        detachedTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.getList().size() == 0) {
                stage.close();
            }
        });

        stage.setOnShown((WindowEvent t) -> {
            this.getTabs().remove(tab);
            // tab.getTabPane().getTabs().remove(tab);
        });
        StageAppearanceManager.getInstance().register(stage);


        stage.show();
        detachedTabPane.getSelectionModel().select(tab);
    }

    public boolean isTearable() {
        return tearable;
    }

    public void setTearable(boolean tearable) {
        this.tearable = tearable;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
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
}
