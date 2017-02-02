package eu.fthevenet.binjr.controls;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Factory for {@link FormattedNumberTableCell} instances
 *
 * @author Frederic Thevenet
 * @param <S> The type of the TableView generic type
 * @param <T> The type of the item contained within the Cell
 */
public class  FormattedNumberTableCellFactory<S, T extends Number> implements Callback<TableColumn<S, T>, TableCell<S, T>> {
    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        return new FormattedNumberTableCell();
    }
}
