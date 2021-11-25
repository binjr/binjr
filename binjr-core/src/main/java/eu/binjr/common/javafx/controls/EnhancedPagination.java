/*
 *    Copyright 2021 Frederic Thevenet
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
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


public class EnhancedPagination extends Pagination {
    @Override
    protected Skin<Pagination> createDefaultSkin() {
        return new EnhancedPaginationSkin(this);
    }

    public static class EnhancedPaginationSkin extends PaginationSkin {
        private final HBox controlBox;
        private final Button previousPageButton;
        private final Button nextPageButton;
        private final Button firstPageButton;
        private final Button lastPageButton;
        private final Label pageInformation;
        private final Button jumpToPageButton;
        private final BindingManager bindingManager = new BindingManager();

        public EnhancedPaginationSkin(Pagination pagination) {
            super(pagination);
            controlBox = (HBox) lookup(pagination, ".control-box");
            previousPageButton = (Button) lookup(pagination, ".left-arrow-button");
            nextPageButton = (Button) lookup(pagination, ".right-arrow-button");
            pageInformation = (Label) lookup(pagination, ".page-information");
            pageInformation.setVisible(false);
            pageInformation.setManaged(false);

            firstPageButton = new ToolButtonBuilder<Button>(bindingManager)
                    .setText("First")
                    .setTooltip("Jump to first page")
                    .setHeight(previousPageButton.getHeight())
                    .setStyleClass("dialog-button")
                    .setIconStyleClass("rewind-icon")
                    .bind(Node::disableProperty, previousPageButton.disableProperty())
                    .setAction(event -> pagination.setCurrentPageIndex(0))
                    .build(Button::new);

            lastPageButton = new ToolButtonBuilder<Button>(bindingManager)
                    .setText("Last")
                    .setTooltip("Jump to last page")
                    .setHeight(nextPageButton.getHeight())
                    .setStyleClass("dialog-button")
                    .setIconStyleClass("fast-forward-icon")
                    .bind(Node::disableProperty, nextPageButton.disableProperty())
                    .setAction(event -> pagination.setCurrentPageIndex(pagination.getPageCount()))
                    .build(Button::new);

            var input = new TextField();
            HBox.setHgrow(input, Priority.NEVER);
            var label = new Label("Jump to:");
            label.setMinWidth(55);
            HBox.setHgrow(label, Priority.ALWAYS);
            var hbox = new HBox();
            hbox.setSpacing(10);
            hbox.getChildren().addAll(label, input);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getStyleClass().addAll("pagination-popup");
            hbox.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));
            hbox.setPrefSize(135, 40);
            var popup = new PopupControl();
            popup.setAutoHide(true);
            popup.getScene().setRoot(hbox);
            input.setOnAction(bindingManager.registerHandler(event -> {
                try {
                    int targetPageIndex = Math.max(Math.min(Integer.parseInt(input.getText()) - 1, pagination.getPageCount()), 0);
                    pagination.setCurrentPageIndex(targetPageIndex);
                } catch (NumberFormatException e) {
                    // Ignore badly formatted numerical input
                } finally {
                    popup.hide();
                }
            }));
            jumpToPageButton = new ToolButtonBuilder<Button>(bindingManager)
                    .setText("Jump")
                    .setTooltip("Jump to page")
                    .setHeight(nextPageButton.getHeight())
                    .setStyleClass("dialog-button")
                    .setIconStyleClass("share-icon")
                    .setAction(actionEvent -> {
                        Node owner = (Node) actionEvent.getSource();
                        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
                        popup.show(owner.getScene().getWindow(), bounds.getMinX(), bounds.getMinY() - 45);
                        input.setText(Integer.toString(pagination.getCurrentPageIndex() + 1));
                        input.selectAll();
                    }).build(Button::new);

            bindingManager.attachListener(this.controlBox.getChildren(), (ListChangeListener<Node>) c -> {
                while (c.next()) {
                    if (c.wasAdded() && !c.wasRemoved() && c.getAddedSize() == 1 && c.getAddedSubList().get(0) == nextPageButton) {
                        addCustomNodes();
                    }
                }
            });
            addCustomNodes();
        }

        private Node lookup(Control parent, String key) {
            var node = parent.lookup(key);
            if (node == null) {
                throw new IllegalStateException("Failed to find a child node for lookup " + key);
            }
            return node;
        }

        protected void addCustomNodes() {
            if (firstPageButton.getParent() != controlBox) {
                controlBox.getChildren().add(0, firstPageButton);
                controlBox.getChildren().addAll(lastPageButton, jumpToPageButton);
            }
        }

        @Override
        public void dispose() {
            super.dispose();
            bindingManager.close();
        }
    }

}