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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.layout.AnchorPane;

/**
 * An {@link AnchorPane} that can slide in or out from a side of its attached window.
 * @author Frederic Thevenet
 */
public class CommandBarPane extends AnchorPane {
    private static PseudoClass EXPANDED_PSEUDO_CLASS = PseudoClass.getPseudoClass("expanded");

    private BooleanProperty expanded = new BooleanPropertyBase(false) {
        public void invalidated() {
            pseudoClassStateChanged(EXPANDED_PSEUDO_CLASS, get());
        }

        @Override
        public Object getBean() {
            return CommandBarPane.this;
        }

        @Override
        public String getName() {
            return "expanded";
        }
    };

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public boolean isExpanded() {
        return expanded.get();
    }


}
