package eu.fthevenet.util.ui.controls;


import javafx.scene.control.*;
import javafx.util.Callback;

/**
 * A fully fleshed out class that allows for context menus to be shown on right click.
 */
public class ContextMenuTreeViewCell<T> extends TreeCell<T> {

    public static <T> Callback<TreeView<T>,TreeCell<T>> forTreeView(ContextMenu contextMenu) {
        return forTreeView(contextMenu, null);
    }

    public static <T> Callback<TreeView<T>,TreeCell<T>> forTreeView(final ContextMenu contextMenu, final Callback<TreeView<T>,TreeCell<T>> cellFactory) {
        return treeView -> {
            TreeCell<T> cell;
            if (cellFactory == null) {
                cell = new TreeCell<T>();
                cell.itemProperty().addListener((observable, oldValue, newValue) -> {

                    cell.setText(newValue == null ? null : newValue.toString());
                });
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