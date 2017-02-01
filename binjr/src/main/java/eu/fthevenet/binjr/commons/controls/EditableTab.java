package eu.fthevenet.binjr.commons.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * A Tab control that can be renamed on double-click
 *
 * @author Frederic Thevenet
 */
public class EditableTab extends Tab {
    private final Label label;

    /**
     * Initializes a new instance of the {@link EditableTab} instance.
     *
     * @param text the title for the tab.
     */
    public EditableTab(String text) {
        super();
        label = new Label(text);

        setGraphic(label);

        final TextField textField = new TextField();
        label.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    textField.setText(label.getText());
                    setGraphic(textField);
                    textField.selectAll();
                    textField.requestFocus();
                }
            }
        });

        textField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                label.setText(textField.getText());
                setGraphic(label);
            }
        });

        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    label.setText(textField.getText());
                    setGraphic(label);
                }
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
