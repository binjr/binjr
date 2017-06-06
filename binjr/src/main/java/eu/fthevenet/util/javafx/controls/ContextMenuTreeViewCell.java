/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.util.javafx.controls;


import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

/**
 * An implementation of {@link TreeCell} with a context menu attached
 */
public class ContextMenuTreeViewCell<T> extends TreeCell<T> {

    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(ContextMenu contextMenu) {
        return forTreeView(contextMenu, null);
    }

    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(final ContextMenu contextMenu, final Callback<TreeView<T>, TreeCell<T>> cellFactory) {
        return treeView -> {
            TreeCell<T> cell;
            if (cellFactory == null) {
                cell = new TreeCell<T>();
                cell.itemProperty().addListener((observable, oldValue, newValue) -> cell.setText(newValue == null ? null : newValue.toString()));
            }
            else {
                cell = cellFactory.call(treeView);
            }
            cell.setContextMenu(contextMenu);

            return cell;
        };
    }

    public ContextMenuTreeViewCell(ContextMenu contextMenu) {
        setContextMenu(contextMenu);
    }
}