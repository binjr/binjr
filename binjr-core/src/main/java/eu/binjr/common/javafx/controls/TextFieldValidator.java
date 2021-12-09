/*
 *    Copyright 2021 Frederic Thevenet
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
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.InvalidationListener;
import javafx.scene.control.TextField;

import java.util.function.Predicate;

public class TextFieldValidator {

    public static void succeed(TextField textField) {
        textField.setStyle("");
    }

    public static void fail(TextField textField, boolean autoReset) {
        textField.setStyle(String.format("-fx-background-color: %s;",
                ColorUtils.toHex(UserPreferences.getInstance().invalidInputColor.get())));
        if (autoReset) {
            BindingManager manager = new BindingManager();
            InvalidationListener autoResetListener = obs -> {
                succeed(textField);
                manager.close();
            };
            manager.attachListener(textField.textProperty(), autoResetListener);
        }
    }

    public static boolean validate(TextField textField, boolean autoReset, Predicate<String> validator) {
        if (validator.test(textField.getText())) {
            succeed(textField);
            return true;
        } else {
            fail(textField, autoReset);
            return false;
        }
    }

}
