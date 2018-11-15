/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.util.javafx.controls;

import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;

/**
 * A Tab control that can be renamed on double-click
 *
 * @author Frederic Thevenet
 */
public class EditableTab extends Tab {
    private final Label label;

    public String getName() {
        return label.textProperty().getValue();
    }

    public Property<String> nameProperty() {
        return label.textProperty();
    }

    public void setName(String tabName) {
        label.textProperty().setValue(tabName);
    }

    /**
     * Initializes a new instance of the {@link EditableTab} instance.
     *
     * @param text the title for the tab.
     */
    public EditableTab(String text) {
        super();
        label = new Label(text);
        label.textProperty();
        setGraphic(label);

        final TextField textField = new TextField();
        label.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                textField.setText(label.getText());
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        });

        textField.setOnAction(event -> {
            if (!textField.getText().isEmpty()) {
                label.setText(textField.getText());
            }
            setGraphic(label);
        });

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (!textField.getText().isEmpty()) {
                    label.setText(textField.getText());
                }
                setGraphic(label);
            }
        });
    }


    /**
     * Renames the tab
     *
     * @param text the new name for the tab.
     */
    public void rename(String text) {
        label.setText(text);
    }
}
