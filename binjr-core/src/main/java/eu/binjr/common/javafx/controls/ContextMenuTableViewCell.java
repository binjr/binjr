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


import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeCell;
import javafx.util.Callback;

/**
 * An implementation of {@link TreeCell} with a context menu attached
 *
 * @author Frederic Thevenet
 */
public class ContextMenuTableViewCell<S, T> extends TableCell<S, T> {

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(ContextMenu contextMenu) {
        return forTableColumn(contextMenu, null);
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final ContextMenu contextMenu, final Callback<TableColumn<S, T>, TableCell<S, T>> cellFactory) {
        return column -> {
            TableCell<S, T> cell;
            if (cellFactory == null) {
                cell = new TableCell<S, T>();
                cell.itemProperty().addListener((observable, oldValue, newValue) -> {
                    cell.setText(newValue == null ? null : newValue.toString());
                });
            } else {
                cell = cellFactory.call(column);
            }
            cell.setContextMenu(contextMenu);
            return cell;
        };
    }

    public ContextMenuTableViewCell(ContextMenu contextMenu) {
        setContextMenu(contextMenu);
    }
}