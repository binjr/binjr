package eu.fthevenet.util.ui.controls;

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

    public ColorTableCell(TableColumn<T, Color> column) {
        colorPicker = new ColorPicker();
        colorPicker.getStyleClass().add("button");
        colorPicker.getStyleClass().add("borderless-color-picker");
        //   colorPicker.getStylesheets().add(getClass().getResource("/css/common/ColorPickerCell.css").toExternalForm());
        colorPicker.editableProperty().bind(column.editableProperty());
        colorPicker.disableProperty().bind(column.editableProperty().not());
        colorPicker.setOnShowing(event -> {
            TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
        });
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (isEditing()) {
                commitEdit(newValue);
            }
        });
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Color item, boolean empty) {
        super.updateItem(item, empty);

        setText(null);
        if (empty) {
            setGraphic(null);
        }
        else {
            colorPicker.setValue(item);
            setGraphic(this.colorPicker);
        }
    }
}