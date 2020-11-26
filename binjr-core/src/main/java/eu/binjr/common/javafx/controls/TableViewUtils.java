/*
 *    Copyright 2019 Frederic Thevenet
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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.TableView;

public final class TableViewUtils {
    //FIXME try and retrieve the actual scrollbar width at runtime
    private static final double SCROLL_BAR_WIDTH = 20.0;

    public static void autoFillTableWidthWithLastColumn(TableView<?> table) {
        autoFillTableWidthWithColumn(table, table.getColumns().size() - 1);
    }

    public static void autoFillTableWidthWithColumn(TableView<?> table, int columnIdx) {
        if (columnIdx > 0) {
            ReadOnlyDoubleProperty[] widthProperties = new ReadOnlyDoubleProperty[table.getColumns().size()];
            for (int i = 0; i < table.getColumns().size(); i++) {
                if (i != columnIdx) {
                    widthProperties[i] = table.getColumns().get(i).widthProperty();
                }
            }

            widthProperties[columnIdx] = table.widthProperty();
            table.getColumns().get(columnIdx).prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                double width = widthProperties[columnIdx].getValue() - SCROLL_BAR_WIDTH;
                for (int i = 0; i < table.getColumns().size(); i++) {
                    if (i != columnIdx) {
                        width -= widthProperties[i].getValue();
                    }
                }
                return Math.max(width, 100.0);
            }, widthProperties));
        }
    }

}
