/*
 *    Copyright 2019-2024 Frederic Thevenet
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

package eu.binjr.core.appearance;

import javafx.application.Platform;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An enumeration of built-in user interface themes
 *
 * @author Frederic Thevenet
 */
public enum BuiltInUserInterfaceThemes implements UserInterfaceThemes {
    SYSTEM("System", () -> switch (Platform.getPreferences().colorSchemeProperty().get()) {
        case DARK -> "/eu/binjr/css/Dark.css";
        case LIGHT -> "/eu/binjr/css/Light.css";
    }),
    LIGHT("Light", () -> "/eu/binjr/css/Light.css"),
    DARK("Dark", () -> "/eu/binjr/css/Dark.css"),
    CLASSIC("Classic", () -> "/eu/binjr/css/Classic.css");

    private final Supplier<String> cssPath;
    private final String label;

    BuiltInUserInterfaceThemes(String label, Supplier<String> cssPath) {
        this.label = label;
        this.cssPath = cssPath;
    }

    /**
     * Returns the path of the css for this theme.
     *
     * @return the path of the css for this theme.
     */
    public String getCssPath() {
        return cssPath.get();
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Returns the enum entry corresponding to the provided string is it exists, otherwise returns the specified default.
     *
     * @param name         the string to try and get a corresponding an enum for.
     * @param defaultValue the default to return is no entry matches the provided name.
     * @return the enum entry corresponding to the provided string is it exists, otherwise returns the specified default.
     */
    public static BuiltInUserInterfaceThemes valueOf(String name, BuiltInUserInterfaceThemes defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value cannot be null.");
        try {
            return BuiltInUserInterfaceThemes.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
