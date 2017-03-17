package eu.fthevenet.util.ui.controls;


import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeCell;
import javafx.util.Callback;

/**
 * A fully fleshed out class that allows for context menus to be shown on right click.
 */
public class ContextMenuTableViewCell<T> extends TreeCell<T> {

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
            }
            else {
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