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

import com.sun.istack.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * A Tab control that can be renamed on double-click
 *
 * @author Frederic Thevenet
 */
public class EditableTab extends Tab {
    private final Label titleLabel;

    private final BooleanProperty editable = new SimpleBooleanProperty(false);

    public String getName() {
        return titleLabel.textProperty().getValue();
    }

    private final TextField textField = new TextField();

    public Property<String> nameProperty() {
        return titleLabel.textProperty();
    }

    public void setName(String tabName) {
        titleLabel.textProperty().setValue(tabName);
    }

    public EditableTab(String text) {
        this(text, (ButtonBase) null);
    }

    /**
     * Initializes a new instance of the {@link EditableTab} instance.
     *
     * @param text    the title for the tab.
     * @param buttons A custom {@link Button} instance used to close the tab
     */
    public EditableTab(String text, @Nullable ButtonBase... buttons) {
        this(null, text, buttons);
    }

    /**
     * Initializes a new instance of the {@link EditableTab} instance.
     *
     * @param text    the title for the tab.
     * @param buttons A custom {@link Button} instance used to close the tab
     * @param graphic A node used as an icon oon the tab
     */
    public EditableTab(Node graphic, String text, @Nullable ButtonBase... buttons) {
        super();
        titleLabel = new Label(text);
        titleLabel.setPadding(new javafx.geometry.Insets(0, 4, 0, 4));
        var toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_LEFT);
        if (graphic != null) {
            toolbar.getChildren().add(graphic);
        }
        toolbar.getChildren().add(titleLabel);
        if (buttons != null) {
            setClosable(false);
            toolbar.getChildren().addAll(buttons);
        }

        // We need to wrap the hbox used for layout into a Label
        // otherwise the tabs names in the overflow menu is empty.
        var wrappingLabel = new Label();
        wrappingLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        wrappingLabel.textProperty().bind(titleLabel.textProperty());
        wrappingLabel.setGraphic(toolbar);
        setGraphic(wrappingLabel);

        editable.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                textField.setText(titleLabel.getText());
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            } else {
                if (!textField.getText().isEmpty()) {
                    titleLabel.setText(textField.getText());
                }
                setGraphic(wrappingLabel);
            }
        });


        titleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editable.setValue(true);
            }
        });

        textField.setOnAction(event -> {
            editable.setValue(false);
        });

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                editable.setValue(false);
            }
        });
    }

    /**
     * Renames the tab
     *
     * @param text the new name for the tab.
     */
    public void rename(String text) {
        titleLabel.setText(text);
    }

    public boolean isEditable() {
        return editable.get();
    }

    public BooleanProperty editableProperty() {
        return editable;
    }

    public void setEditable(boolean editable) {
        if (editable) {
            this.textField.requestFocus();
        }
        this.editable.set(editable);
    }
}
