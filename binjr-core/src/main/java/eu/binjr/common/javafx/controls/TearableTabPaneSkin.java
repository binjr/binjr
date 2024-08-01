/*
 * Copyright 2024 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.javafx.controls;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class TearableTabPaneSkin extends TabPaneSkin {

    final Button newTabButton;
    final Button splitRightButton;
    final StackPane dropZone;
    final StackPane newPaneDropZone;
    final Label addWorksheetLabel;
    final Button closeSplitPane;
    final Button splitDownButton;

    public TearableTabPaneSkin(TabPane control) {
        super(control);
        TearableTabPane tabPane = (TearableTabPane) getSkinnable();
        Pane tabHeaderBg = (Pane) tabPane.lookup(".tab-header-background");
        Side tabPosition = tabPane.getSide();
        Pane headersRegion = (Pane) tabPane.lookup(".headers-region");
        Region headerArea = (Region) tabPane.lookup(".tab-header-area");

        closeSplitPane = new ToolButtonBuilder<Button>()
                .setText("Close")
                .setTooltip("Close split pane")
                .setWidth(24)
                .setHeight(24)
                .setStyleClass("exit")
                .setIconStyleClass("cross-icon", "small-icon")
                .build(Button::new);
        closeSplitPane.visibleProperty().bind(headerArea.visibleProperty().not());
        closeSplitPane.managedProperty().bind(headerArea.visibleProperty().not());

        newTabButton = new ToolButtonBuilder<Button>()
                .setTooltip("Add a new tab")
                .setId("newTabButton")
                .setStyleClass("add-tab-button")
                .setFocusTraversable(false)
                .setIconStyleClass("add-new-tab-icon", "small-icon")
                .build(Button::new);
        splitRightButton = new ToolButtonBuilder<Button>()
                .setTooltip("Split Tab Right\n[Alt] Split Tab Down")
                .setId("splitZoneButton")
                .setFocusTraversable(false)
                .setStyleClass("add-tab-button")
                .setIconStyleClass("split-right", "small-icon")
                .build(Button::new);
        splitDownButton = new ToolButtonBuilder<Button>()
                .setTooltip("Split Tab Down")
                .setId("splitZoneButton")
                .setFocusTraversable(false)
                .setStyleClass("add-tab-button")
                .setIconStyleClass("split-down", "small-icon")
                .build(Button::new);
        var toolbar = new ToolBar();
        splitDownButton.visibleProperty().bind(splitRightButton.visibleProperty().not());
        splitDownButton.managedProperty().bind(splitDownButton.visibleProperty());
        splitRightButton.managedProperty().bind(splitRightButton.visibleProperty());

        toolbar.getItems().addAll(newTabButton, splitRightButton, splitDownButton);

        dropZone = new StackPane(ToolButtonBuilder.makeIconNode(Pos.CENTER, 0, 0, "new-tab-icon"));
        dropZone.getStyleClass().add("drop-zone");
        newPaneDropZone = new StackPane(dropZone);
        newPaneDropZone.getStyleClass().add("chart-viewport-parent");
        newPaneDropZone.setPrefHeight(34);
        newPaneDropZone.setMaxHeight(34);
        newPaneDropZone.setAlignment(Pos.CENTER);

        StackPane.setAlignment(toolbar, Pos.CENTER_LEFT);
        switch (tabPosition) {
            case TOP, BOTTOM -> {
                toolbar.translateXProperty().bind(
                        headersRegion.widthProperty()
                                .add(Bindings.createDoubleBinding(() -> headerArea.getInsets().getLeft(), headerArea.insetsProperty()))
                );
            }
            case LEFT, RIGHT -> toolbar.translateXProperty().bind(
                    tabHeaderBg.widthProperty()
                            .subtract(headersRegion.widthProperty())
                            .subtract(toolbar.widthProperty())
                            .subtract(Bindings.createDoubleBinding(() -> headerArea.getInsets().getTop(), headerArea.insetsProperty()))
            );
            default -> throw new IllegalStateException("Invalid value for side enum");
        }

        addWorksheetLabel = new Label();
        addWorksheetLabel.getStyleClass().add("add-worksheet-background-icon");
        addWorksheetLabel.setMaxHeight(Double.POSITIVE_INFINITY);
        addWorksheetLabel.setMaxWidth(Double.POSITIVE_INFINITY);

        var tooltip = new Tooltip("Click to add a new worksheet");
        tooltip.setShowDelay(Duration.millis(500));
        addWorksheetLabel.setTooltip(tooltip);
        Region graphic = new Region();
        graphic.getStyleClass().add("addWorksheet-icon");
        graphic.setScaleX(20);
        graphic.setScaleY(20);
        addWorksheetLabel.setGraphic(graphic);
        addWorksheetLabel.visibleProperty().bind(headerArea.visibleProperty().not());
        tabHeaderBg.getChildren().addAll(toolbar, newPaneDropZone);
        var pane =  new StackPane();
        pane.getChildren().addAll(closeSplitPane, addWorksheetLabel );

        this.getChildren().add(pane);
    }


    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        closeSplitPane.setTranslateX(w - (closeSplitPane.getWidth() ));
        closeSplitPane.setTranslateY(closeSplitPane.getHeight() );
        addWorksheetLabel.setTranslateX(w / 2);
        addWorksheetLabel.setTranslateY(h / 2);
    }
}
