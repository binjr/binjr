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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A TabPane container with a button to add a new tab
 *
 * @author Frederic Thevenet
 */
public class TearableTabPane extends TabPane {
    private static final Logger logger = LogManager.getLogger(TearableTabPane.class);
    private Supplier<Optional<Tab>> newTabFactory = () -> Optional.of(new Tab());
    private EventHandler<ActionEvent> onNewTabAction;


    private Tab currentTab;
    private final List<Tab> originalTabs = new ArrayList<>();
    ;
    private final Map<Integer, Tab> tapTransferMap = new HashMap<>();
    private String[] stylesheets = new String[]{};
    private final BooleanProperty alwaysOnTop = new SimpleBooleanProperty();

    public TearableTabPane() {
        this((Tab[]) null);
    }

    public TearableTabPane(Tab... tabs) {
        super(tabs);

        originalTabs.addAll(this.getTabs());
        for (int i = 0; i < this.getTabs().size(); i++) {
            tapTransferMap.put(i, this.getTabs().get(i));
        }
        this.getTabs().stream().forEach(t -> {
            t.setClosable(false);
        });
        this.setOnDragDetected(
                (MouseEvent event) -> {
                    if (event.getSource() instanceof TabPane) {
                        Pane rootPane = (Pane) this.getScene().getRoot();
                        rootPane.setOnDragOver((DragEvent event1) -> {
                            event1.acceptTransferModes(TransferMode.ANY);
                            event1.consume();
                        });
                        currentTab = this.getSelectionModel().getSelectedItem();
                        SnapshotParameters snapshotParams = new SnapshotParameters();
                        snapshotParams.setTransform(Transform.scale(0.4, 0.4));
                        WritableImage snapshot = currentTab.getContent().snapshot(snapshotParams, null);
                        Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.put(DataFormat.PLAIN_TEXT, "");
                        db.setDragView(snapshot, 40, 40);
                        db.setContent(clipboardContent);
                    }
                    event.consume();
                }
        );
        this.setOnDragDone(
                (DragEvent event) -> {
                    openTabInStage(currentTab);
                    this.setCursor(Cursor.DEFAULT);
                    event.consume();
                }
        );

        Platform.runLater(this::positionNewTabButton);

        // Prepare to change the button on screen position if the tabs side changes
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
                newTabFactory.get().ifPresent(newTab -> {
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

    public Supplier<Optional<Tab>> getNewTabFactory() {
        return newTabFactory;
    }

    public void setNewTabFactory(Supplier<Optional<Tab>> newTabFactory) {
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
        if (tab == null) {
            return;
        }
        int originalTab = originalTabs.indexOf(tab);
        tapTransferMap.remove(originalTab);
        Node content = tab.getContent();
        if (content == null) {
            throw new IllegalArgumentException("Can not detach Tab '" + tab.getText() + "': content is empty (null).");
        }
        tab.setContent(null);
        final Scene scene = new Scene((Parent) content, content.prefWidth(0), content.prefHeight(0));
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(tab.getText());
        Point p = MouseInfo.getPointerInfo().getLocation(); // MouseRobot.getMousePosition();
        stage.setX(p.getX());
        stage.setY(p.getY());
        stage.setOnCloseRequest((WindowEvent t) -> {
            StageAppearanceManager.getInstance().unregister(stage);
            stage.close();
            tab.setContent(content);
            int originalTabIndex = originalTabs.indexOf(tab);
            tapTransferMap.put(originalTabIndex, tab);
            int index = 0;
            SortedSet<Integer> keys = new TreeSet<>(tapTransferMap.keySet());
            for (Integer key : keys) {
                Tab value = tapTransferMap.get(key);
                if (!this.getTabs().contains(value)) {
                    this.getTabs().add(index, value);
                }
                index++;
            }
            this.getSelectionModel().select(tab);
        });
        stage.setOnShown((WindowEvent t) -> {
            tab.getTabPane().getTabs().remove(tab);
        });
        StageAppearanceManager.getInstance().register(stage);

        stage.show();
    }
}
