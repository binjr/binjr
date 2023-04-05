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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class LabelWithInlineHelp extends HBox {

    private final Label label;

    private final StringProperty inlineHelp = new SimpleStringProperty();

    public LabelWithInlineHelp() {
        this(null, null);
    }

    public LabelWithInlineHelp(String label) {
        this(label, null);
    }

    public LabelWithInlineHelp(String labelText, String inlineHelpText) {
        var helpPopup = new Tooltip();
        helpPopup.textProperty().bind(inlineHelpProperty());
        helpPopup.setAutoHide(true);
        helpPopup.setHideOnEscape(true);
        var tooltip = new Tooltip();
        tooltip.textProperty().bind(inlineHelpProperty());
        ButtonBase helpButton = new ToolButtonBuilder<>()
                .setStyleClass("dialog-button")
                .setHeight(20.0)
                .setWidth(20.0)
                .setIconStyleClass("help-small-icon")
                .build(ToggleButton::new);
        helpButton.setTooltip(tooltip);
        ((ToggleButton) helpButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Bounds bounds = helpButton.localToScreen(helpButton.getBoundsInLocal());
                helpPopup.show(helpButton, bounds.getMaxX(), bounds.getMinY());
                helpButton.setTooltip(null);
            } else {
                helpPopup.hide();
                helpButton.setTooltip(tooltip);
            }
        });
        helpPopup.showingProperty().addListener((observable, oldValue, newValue) -> {
            ((ToggleButton) helpButton).setSelected(newValue);
        });
        helpButton.setAlignment(Pos.TOP_RIGHT);
        helpButton.visibleProperty().bind(UserPreferences.getInstance().showInlineHelpButtons.property());
        helpButton.managedProperty().bind(UserPreferences.getInstance().showInlineHelpButtons.property());
        label = new Label();
        label.setAlignment(Pos.TOP_LEFT);
        label.setWrapText(true);
        var spacer = new Pane();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        this.getChildren().addAll(label, spacer, helpButton);
        if (labelText != null) {
            label.setText(labelText);
        }
        if (inlineHelpText != null) {
            inlineHelp.set(inlineHelpText);
        }
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
