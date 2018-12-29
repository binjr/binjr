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

package eu.fthevenet.util.javafx.controls;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import java.text.DecimalFormat;

/**
 * A table cell factory aiming to format cells containing decimal numbers.
 *
 * @param <S> The type of the TableView generic type
 * @param <T> The type of the item contained within the Cell
 * @author Frederic Thevenet
 */
public class DecimalFormatTableCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private TextAlignment alignment;
    private DecimalFormat formatter;
    private String formatPattern;

    /**
     * Gets the alignment of text in the cell
     *
     * @return the alignment of text in the cell
     */
    public TextAlignment getAlignment() {
        return alignment;
    }

    /**
     * Sets the alignment of text in the cell
     *
     * @param alignment the alignment of text in the cell
     */
    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment;
    }

    /**
     * Gets the pattern to use to format decimal numbers to text
     *
     * @return the pattern to use to format decimal numbers to text
     */
    public String getPattern() {
        return formatPattern;
    }

    /**
     * Sets the pattern to use to format decimal numbers to text
     *
     * @param format the pattern to use to format decimal numbers to text
     */
    public void setPattern(String format) {
        formatPattern = format;
        this.formatter = new DecimalFormat(formatPattern);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TableCell<S, T> call(TableColumn<S, T> p) {
        TableCell<S, T> cell = new TableCell<S, T>() {

            @Override
            public void updateItem(Object item, boolean empty) {
                if (item == getItem()) {
                    return;
                }
                super.updateItem((T) item, empty);
                if (item == null) {
                    super.setText(null);
                    super.setGraphic(null);
                } else if (formatter != null) {
                    super.setText(formatter.format(item));
                } else if (item instanceof Node) {
                    super.setText(null);
                    super.setGraphic((Node) item);
                } else {
                    super.setText(item.toString());
                    super.setGraphic(null);
                }
            }
        };
        cell.setTextAlignment(alignment);
        switch (alignment) {
            case CENTER:
                cell.setAlignment(Pos.CENTER);
                break;
            case RIGHT:
                cell.setAlignment(Pos.CENTER_RIGHT);
                break;
            default:
                cell.setAlignment(Pos.CENTER_LEFT);
                break;
        }
        return cell;
    }
}