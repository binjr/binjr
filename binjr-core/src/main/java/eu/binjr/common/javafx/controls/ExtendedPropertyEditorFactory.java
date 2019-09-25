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

import eu.binjr.core.dialogs.Dialogs;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtendedPropertyEditorFactory extends DefaultPropertyEditorFactory {
    private static final Logger logger = LogManager.getLogger(ExtendedPropertyEditorFactory.class);

    @Override
    public PropertyEditor<?> call(PropertySheet.Item item) {
        var editor = super.call(item);
        if (editor != null) {
            return editor;
        }
        if (Path.class.isAssignableFrom(item.getType())) {
            return new PathPropertyEditor(item);
        }


        return null;
    }

    public static class PathPropertyEditor implements PropertyEditor<Path> {
        private final HBox editor;
        private final TextField textField;

        PathPropertyEditor(PropertySheet.Item item) {
            TextFormatter<Path> pathTextFormatter = new TextFormatter<>(new StringConverter<>() {
                @Override
                public String toString(Path object) {
                    if (object == null) {
                        return "";
                    }
                    return object.toString();
                }

                @Override
                public Path fromString(String string) {
                    return Path.of(string);
                }
            });
            editor = new HBox();
          //  editor.setMaxWidth(Double.MAX_VALUE);
            editor.setSpacing(5);
            AnchorPane.setLeftAnchor(editor, 0.0);
            AnchorPane.setRightAnchor(editor, 0.0);
            textField = new TextField();
            textField.setTextFormatter(pathTextFormatter);
            item.getObservableValue().ifPresent(itemValue -> {
                itemValue.addListener((observable, oldValue, newValue) ->pathTextFormatter.setValue((Path)newValue));
            });
            pathTextFormatter.valueProperty().addListener((observable, oldValue, newValue) -> item.setValue(newValue));
            HBox.setHgrow(textField, Priority.ALWAYS);
           // textField.setMaxWidth(Double.MAX_VALUE);
            var button = new Button("Browse");
            editor.getChildren().addAll(textField, button);
            button.setOnAction(event -> {
                DirectoryChooser fileChooser = new DirectoryChooser();
                fileChooser.setTitle("Select Folder");
                try {
                    Path pluginPath = Paths.get(textField.getText()).toRealPath();
                    if (Files.isDirectory(pluginPath)) {
                        fileChooser.setInitialDirectory(pluginPath.toFile());
                    }
                } catch (Exception e) {
                    logger.debug("Could not initialize working dir for DirectoryChooser", e);
                }
                File newPluginLocation = fileChooser.showDialog(Dialogs.getStage(editor));
                if (newPluginLocation != null) {
                    textField.setText(newPluginLocation.getPath());
                }
            });
        }

        @Override
        public Node getEditor() {
            return editor;
        }

        @Override
        public Path getValue() {
            return Path.of(textField.getText());
        }

        @Override
        public void setValue(Path value) {
            textField.setText(value.toString());
        }
    }
}
