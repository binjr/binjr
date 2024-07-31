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

package eu.binjr.common.javafx.controls;

import javafx.scene.Node;
import javafx.scene.control.*;

/**
 * A {@link TableCell} implementation that shows an icon if the bound property is true
 *
 * @param <T> The type of the TableView generic type
 * @author Frederic Thevenet
 */
public class IconTableCell<T> extends TableCell<T, Boolean> {
    private final Node icon;

    public IconTableCell(Node icon) {
        this.icon = icon;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        if (empty || !item) {
            setGraphic(null);
        } else {
            setGraphic(icon);
        }
    }
}