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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.Objects;

public class TearableTabPaneSkin extends TabPaneSkin {

    final Button newTabButton;
    final StackPane dropZone;
    final StackPane newPaneDropZone;
    final Label addWorksheetLabel;

    public TearableTabPaneSkin(TabPane control) {
        super(control);
        newTabButton = new Button();
        // newTabButton.visibleProperty().bind(manager.newTabButtonVisible);
        newTabButton.setId("newTabButton");
        newTabButton.setFocusTraversable(false);
        newTabButton.getStyleClass().add("add-tab-button");
        SVGPath icon = new SVGPath();
        icon.setContent("m 31.25,54.09375 0,2.4375 -2.46875,0 0,0.375 2.46875,0 0,2.46875 0.375,0 0,-2.46875 2.46875,0 0,-0.375 -2.46875,0 0,-2.4375 -0.375,0 z");
        icon.getStyleClass().add("add-tab-button-icon");
        newTabButton.setGraphic(icon);
        newTabButton.setAlignment(Pos.CENTER);
        TearableTabPane tabPane = (TearableTabPane) getSkinnable();
        Pane tabHeaderBg = (Pane) tabPane.lookup(".tab-header-background");

        dropZone = new StackPane(ToolButtonBuilder.makeIconNode(Pos.CENTER, 0, 0, "new-tab-icon"));
        dropZone.getStyleClass().add("drop-zone");
        newPaneDropZone = new StackPane(dropZone);
        newPaneDropZone.getStyleClass().add("chart-viewport-parent");
        newPaneDropZone.setPrefHeight(34);
        newPaneDropZone.setMaxHeight(34);
        newPaneDropZone.setAlignment(Pos.CENTER);

        Side tabPosition = tabPane.getSide();
        Pane headersRegion = (Pane) tabPane.lookup(".headers-region");
        Region headerArea = (Region) tabPane.lookup(".tab-header-area");

        StackPane.setAlignment(newTabButton, Pos.CENTER_LEFT);
        switch (tabPosition) {
            case TOP, BOTTOM -> {
                //newTabButton.setTranslateX( headersRegion.getWidth()+ headerArea.getInsets().getLeft());
                newTabButton.translateXProperty().bind(
                        headersRegion.widthProperty()
                                .add(Bindings.createDoubleBinding(() -> headerArea.getInsets().getLeft(), headerArea.insetsProperty()))
                );
         }
            case LEFT, RIGHT -> newTabButton.translateXProperty().bind(
                    tabHeaderBg.widthProperty()
                            .subtract(headersRegion.widthProperty())
                            .subtract(newTabButton.widthProperty())
                            .subtract(Bindings.createDoubleBinding(() -> headerArea.getInsets().getTop(), headerArea.insetsProperty()))
            );
            default -> throw new IllegalStateException("Invalid value for side enum");
        }

        addWorksheetLabel = new Label();
      //  addWorksheetLabel.setAlignment(Pos.CENTER);

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
        tabHeaderBg.getChildren().addAll(newTabButton, newPaneDropZone);
        this.getChildren().add(addWorksheetLabel);
    }


    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        addWorksheetLabel.setTranslateX(w/2);//Double.POSITIVE_INFINITY);
        addWorksheetLabel.setTranslateY(h/2);
    }
}
