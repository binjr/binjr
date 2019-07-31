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
        int lastIdx = table.getColumns().size() - 1;
        if (lastIdx > 0) {
            ReadOnlyDoubleProperty[] widthProperties = new ReadOnlyDoubleProperty[lastIdx + 1];
            for (int i = 0; i < lastIdx; i++) {
                widthProperties[i] = table.getColumns().get(i).widthProperty();
            }
            widthProperties[lastIdx] = table.widthProperty();
            table.getColumns().get(lastIdx).prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                double width = widthProperties[lastIdx].getValue() - SCROLL_BAR_WIDTH;
                for (int i = 0; i < lastIdx; i++) {
                    width -= widthProperties[i].getValue();
                }
                return Math.max(width, 100.0);
            }, widthProperties));
        }
    }
}
