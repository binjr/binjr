/*
 * Copyright 2023 Frederic Thevenet
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

import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class LabelWithInlineHelp extends HBox {

    private final Label label;

    private final StringProperty inlineHelp = new SimpleStringProperty();


    public LabelWithInlineHelp() {
        var helpPopup = new Tooltip();
        helpPopup.textProperty().bind(inlineHelpProperty());
        helpPopup.setAutoHide(true);
        helpPopup.setHideOnEscape(true);
        ButtonBase helpButton = new ToolButtonBuilder<>()
                .setStyleClass("dialog-button")
                .setHeight(20.0)
                .setWidth(20.0)
                .setIconStyleClass("help-small-icon")
                .setTooltip("")
                .setAction(event -> {
                    if (helpPopup.isShowing()) {
                        helpPopup.hide();
                    } else {
                        Node owner = (Node) event.getSource();
                        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
                        helpPopup.show(owner, bounds.getMaxX(), bounds.getMinY());
                        PauseTransition pt = new PauseTransition(Duration.millis(3000));
                        pt.setOnFinished(e -> {
                            helpPopup.hide();
                        });
                        pt.play();
                    }
                    event.consume();
                })
                .build(Button::new);
        helpButton.setAlignment(Pos.TOP_RIGHT);
        helpButton.getTooltip().textProperty().bind(inlineHelpProperty());
        helpButton.visibleProperty().bind(UserPreferences.getInstance().showInlineHelpButtons.property());
        helpButton.managedProperty().bind(UserPreferences.getInstance().showInlineHelpButtons.property());
        label = new Label();
        label.setAlignment(Pos.TOP_LEFT);
        label.setWrapText(true);
        var spacer = new Pane();
        // HBox.setHgrow(label, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        // HBox.setHgrow(helpButton, Priority.ALWAYS);
        this.getChildren().addAll(label, spacer, helpButton);
    }


    public String getInlineHelp() {
        return inlineHelp.get();
    }

    public StringProperty inlineHelpProperty() {
        return inlineHelp;
    }

    public void setInlineHelp(String inlineHelp) {
        this.inlineHelp.set(inlineHelp);
    }


    public String getText() {
        return label.getText();
    }

    public StringProperty textProperty() {
        return label.textProperty();
    }

    public void setText(String text) {
        this.label.setText(text);
    }
}
