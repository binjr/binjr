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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;

public class ExtendedPropertyEditorFactory extends DefaultPropertyEditorFactory {
    private static final Logger logger = LogManager.getLogger(ExtendedPropertyEditorFactory.class);

    @Override
    public PropertyEditor<?> call(PropertySheet.Item item) {
        var editor = super.call(item);
        if (editor != null) {
            return editor;
        }
        if (Path.class.isAssignableFrom(item.getType())) {
            return new FormattedPropertyEditor<Path>(item, new TextFormatter<>(new StringConverter<>() {
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
            }), (node, path) -> {
                DirectoryChooser fileChooser = new DirectoryChooser();
                fileChooser.setTitle("Select Folder");
                try {
                    Path pluginPath = path.toRealPath();
                    if (Files.isDirectory(pluginPath)) {
                        fileChooser.setInitialDirectory(pluginPath.toFile());
                    }
                } catch (Exception e) {
                    logger.debug("Could not initialize working dir for DirectoryChooser", e);
                }
                File folder = fileChooser.showDialog(Dialogs.getStage(node));
                if (folder != null) {
                    return Optional.of(folder.toPath());
                }
                return Optional.empty();
            });
        }

        if (Duration.class.isAssignableFrom(item.getType())) {
            return new FormattedPropertyEditor<Duration>(item, new TextFormatter<>(new StringConverter<>() {
                @Override
                public String toString(Duration object) {
                    if (object == null) {
                        return "";
                    }
                    return String.format("%d", (long) object.toMillis());
                }

                @Override
                public Duration fromString(String string) {
                    return Duration.millis(Long.parseLong(string));
                }
            }));
        }
        return null;
    }

    public static class FormattedPropertyEditor<T> implements PropertyEditor<T> {
        private final HBox editor;
        private final TextField textField;
        private final TextFormatter<T> textFormatter;

        FormattedPropertyEditor(PropertySheet.Item item, TextFormatter<T> textFormatter) {
            this(item, textFormatter, null);
        }

        FormattedPropertyEditor(PropertySheet.Item item, TextFormatter<T> textFormatter, BiFunction<Node, T, Optional<T>> editAction) {
            this.textFormatter = textFormatter;
            editor = new HBox();
            editor.setSpacing(5);
            AnchorPane.setLeftAnchor(editor, 0.0);
            AnchorPane.setRightAnchor(editor, 0.0);
            textField = new TextField();
            textField.setTextFormatter(textFormatter);
            item.getObservableValue().ifPresent(itemValue -> {
                itemValue.addListener((observable, oldValue, newValue) -> textFormatter.setValue((T) newValue));
            });
            textFormatter.setValue((T) item.getValue());
            textFormatter.valueProperty().addListener((observable, oldValue, newValue) -> item.setValue(newValue));
            HBox.setHgrow(textField, Priority.ALWAYS);
            editor.getChildren().add(textField);
            if (editAction != null) {
                var editButton = new Button("...");
                editButton.setOnAction(event -> {
                    editAction.apply(getEditor(), textFormatter.getValue()).ifPresent(textFormatter::setValue);
                });
                editor.getChildren().add(editButton);
            }
        }

        @Override
        public Node getEditor() {
            return editor;
        }

        @Override
        public T getValue() {
            return textFormatter.getValueConverter().fromString(textField.getText());
        }

        @Override
        public void setValue(T value) {
            textField.setText(textFormatter.getValueConverter().toString(value));
        }
    }

}
