/*
 *    Copyright 2025 Frederic Thevenet
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

import eu.binjr.common.colors.ColorUtils;
import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import org.fxmisc.richtext.CodeArea;

import java.util.function.Function;
import java.util.function.Predicate;

public class CodeAreaValidator {

    public static void succeed(CodeArea textField) {
        textField.setStyle("");
    }

    public static void fail(CodeArea textField,
                            boolean autoReset,
                            ObservableValue<?>... resetProperties) {
        fail(textField, null, autoReset, resetProperties);
    }

    public static void fail(CodeArea textField, String reason,
                            boolean autoReset,
                            ObservableValue<?>... resetProperties) {
        textField.setStyle(String.format("-fx-background-color: %s;",
                ColorUtils.toHex(UserPreferences.getInstance().invalidInputColor.get())));
        if (reason != null) {
            Dialogs.notifyError("Invalid input", reason, Pos.BOTTOM_RIGHT, textField);
        }
        if (autoReset) {
            BindingManager manager = new BindingManager();
            InvalidationListener autoResetListener = obs -> {
                succeed(textField);
                manager.close();
            };
            manager.attachListener(textField.textProperty(), autoResetListener);
            if (resetProperties != null) {
                for (ObservableValue<?> p : resetProperties) {
                    manager.attachListener(p, autoResetListener);
                }
            }
        }
    }

    public static boolean validate(CodeArea textField,
                                   boolean autoReset,
                                   Predicate<String> validator,
                                   ObservableValue<?>... resetProperties) {
        if (validator.test(textField.getText())) {
            succeed(textField);
            return true;
        } else {
            fail(textField, autoReset, resetProperties);
            return false;
        }
    }

    public static boolean validate(CodeArea textField,
                                   boolean autoReset,
                                   Function<String, ValidationStatus> validator,
                                   ObservableValue<?>... resetProperties) {
        var status = validator.apply(textField.getText());
        if (status.isValid()) {
            succeed(textField);
            return true;
        } else {
            fail(textField, status.reason, autoReset, resetProperties);
            return false;
        }
    }

    public record ValidationStatus(boolean isValid, String reason) {

    }

}
