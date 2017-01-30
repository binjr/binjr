package eu.fthevenet.binjr.commons.controls;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class  FormattedNumberTableCellFactory<S, T extends Number> implements Callback<TableColumn<S, T>, TableCell<S, T>> {
    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        return new FormattedNumberTableCell();
    }
}
