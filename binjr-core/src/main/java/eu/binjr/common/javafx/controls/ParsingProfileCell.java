/*
 *    Copyright 2017-2022 Frederic Thevenet
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
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.dialogs.ParsingProfileDialog;
import javafx.scene.control.*;

/**
 * A {@link TableCell} implementation that shows a {@link Button}
 *
 * @param <T> The type of the TableView generic type
 * @author Frederic Thevenet
 */
public class ParsingProfileCell<T> extends TableCell<T, ParsingProfile> {
    private final ButtonBase button;
    private ParsingProfile current;

    public ParsingProfileCell(TableColumn<T, ParsingProfile> column,
                              BindingManager bindingManager,
                              Runnable onEdit) {
        button = new ToolButtonBuilder<>(bindingManager)
                .setText("")
                .setTooltip("Edit")
                .setStyleClass("dialog-button")
                .setIconStyleClass("settings-icon", "small-icon")
                .setFocusTraversable(false)
                .setAction(event -> {
                   // getTableView().getSelectionModel().select(getTableRow().getIndex());
                    var dlg = new ParsingProfileDialog(Dialogs.getStage(getTableView()), current);
                    dlg.showAndWait().ifPresent(selection -> {
                        getTableView().edit(getTableRow().getIndex(), column);
                        commitEdit(selection);
                        onEdit.run();
                    });
                })
                .build(Button::new);
        setContentDisplay(ContentDisplay.LEFT);
    }


    @Override
    protected void updateItem(ParsingProfile item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
            setText("");
        } else {
            if (item != null) {
                setText(item.toString());
                current = item;
                setGraphic(button);
            }
        }
    }
}