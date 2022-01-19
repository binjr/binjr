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

package eu.binjr.common.javafx.controls;

import eu.binjr.common.javafx.bindings.BindingManager;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

/**
 * A {@link TableCell} implementation that shows a {@link ColorPicker}
 *
 * @param <T> The type of the TableView generic type
 * @author Frederic Thevenet
 */
public class ColorTableCell<T> extends TableCell<T, Color> {
    private final ColorPicker colorPicker;

    public ColorTableCell(TableColumn<T, Color> column, BindingManager bindingManager) {
        colorPicker = new ColorPicker();
        colorPicker.getStyleClass().add("button");
        colorPicker.getStyleClass().add("borderless-color-picker");
        bindingManager.bind(colorPicker.editableProperty(), column.editableProperty());
        bindingManager.bind(colorPicker.disableProperty(), column.editableProperty().not());
        colorPicker.setOnShowing(bindingManager.registerHandler(event -> {
            TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
        }));
        colorPicker.setOnAction(bindingManager.registerHandler(event -> {
            if (isEditing()) {
                commitEdit(colorPicker.getValue());
            }
        }));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Color item, boolean empty) {
        super.updateItem(item, empty);

        setText(null);
        if (empty) {
            setGraphic(null);
        } else {
            colorPicker.setValue(item);
            setGraphic(this.colorPicker);
        }
    }
}