package eu.fthevenet.util.ui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.layout.AnchorPane;

/**
 * Created by FTT2 on 10/04/2017.
 */
public class CommandBarPane extends AnchorPane {
    private static PseudoClass EXPANDED_PSEUDO_CLASS = PseudoClass.getPseudoClass("expanded");

    BooleanProperty expanded = new BooleanPropertyBase(false) {
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
