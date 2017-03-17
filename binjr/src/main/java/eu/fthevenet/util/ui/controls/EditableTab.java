package eu.fthevenet.util.ui.controls;

import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;

/**
 * A Tab control that can be renamed on double-click
 *
 * @author Frederic Thevenet
 */
public class EditableTab extends Tab {
    private final Label label;

    public String getName() {
        return label.textProperty().getValue();
    }

    public Property<String> nameProperty() {
        return label.textProperty();
    }

    public void setName(String tabName) {
        label.textProperty().setValue(tabName);
    }

    /**
     * Initializes a new instance of the {@link EditableTab} instance.
     *
     * @param text the title for the tab.
     */
    public EditableTab(String text) {
        super();
        label = new Label(text);
        label.textProperty();

        setGraphic(label);

        final TextField textField = new TextField();
        label.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                textField.setText(label.getText());
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        });

        textField.setOnAction(event -> {
            label.setText(textField.getText());
            setGraphic(label);
        });

        Button b = new Button();

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                label.setText(textField.getText());
                setGraphic(label);
            }
        });

    }



    /**
     * Renames the tab
     *
     * @param text the new name for the tab.
     */
    public void rename(String text) {
        label.setText(text);
    }
}
