package eu.fthevenet.binjr.dialogs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Popup;

/**
 * @author Frederic Thevenet
 */
public class ChartPropertiesPopup extends Popup {

    public ChartPropertiesPopup() {
        Label label = new Label("Hello wolrd"); //$NON-NLS-1$
        label.setPrefSize(200, 200);
        label.setPadding(new Insets(4));
        contentNode.set(label);
    }

    private final ObjectProperty<Node> contentNode = new SimpleObjectProperty<Node>(
            this, "contentNode") { //$NON-NLS-1$
        @Override
        public void setValue(Node node) {
            if (node == null) {
                throw new IllegalArgumentException(
                        "content node can not be null"); //$NON-NLS-1$
            }
        }

        ;
    };

}
