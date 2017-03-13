package eu.fthevenet.binjr.controls;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;


public class ColorTableCell<T> extends TableCell<T, Color> {
    private final ColorPicker colorPicker;

    public ColorTableCell(TableColumn<T, Color> column) {
        colorPicker = new ColorPicker();
        colorPicker.getStyleClass().add("button");
        colorPicker.getStylesheets().add(getClass().getResource("/css/ColorPickerCell.css").toExternalForm());
        colorPicker.editableProperty().bind(column.editableProperty());
        colorPicker.disableProperty().bind(column.editableProperty().not());
        colorPicker.setOnShowing(event -> {
            TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
        });
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(isEditing()) {
                commitEdit(newValue);
            }
        });
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Color item, boolean empty) {
        super.updateItem(item, empty);

        setText(null);
        if(empty) {
            setGraphic(null);
        } else {
            colorPicker.setValue(item);
            setGraphic(this.colorPicker);
        }
    }
}