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
 * Created by FTT2 on 17/01/2017.
 */
public class EditableTab  extends Tab {
    final Label label;
    public EditableTab(String text){
        super();
         label = new Label(text);

        setGraphic(label);

        final TextField textField = new TextField();
        label.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount()==2) {
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
                if (! newValue) {
                    label.setText(textField.getText());
                    setGraphic(label);
                }
            }
        });
    }

    public void rename(String text){
        label.setText(text);
    }

//    private Tab createEditableTab(String text) {
//        final Label label = new Label(text);
//        final Tab tab = new Tab();
//        tab.setGraphic(label);
//
//        final TextField textField = new TextField();
//        label.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (event.getClickCount()==2) {
//                    textField.setText(label.getText());
//                    tab.setGraphic(textField);
//                    textField.selectAll();
//                    textField.requestFocus();
//                }
//            }
//        });
//
//
//        textField.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                label.setText(textField.getText());
//                tab.setGraphic(label);
//            }
//        });
//
//
//        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> observable,
//                                Boolean oldValue, Boolean newValue) {
//                if (! newValue) {
//                    label.setText(textField.getText());
//                    tab.setGraphic(label);
//                }
//            }
//        });
//        return tab ;
//    }
}
