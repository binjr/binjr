/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.binjr.dialogs;

import java.util.Objects;

/**
 * An enumeration of supported user interface themes
 *
 * @author Frederic Thevenet
 */
public enum UserInterfaceThemes {
    LIGHT("Light", "/css/Light.css"),
    DARK("Dark", "/css/Dark.css"),
    CLASSIC("Classic", "/css/Classic.css");

    private final String cssPath;
    private String label;

    UserInterfaceThemes(String label, String cssPath) {
        this.label = label;
        this.cssPath = cssPath;
    }

    /**
     * Returns the path of the css for this theme.
     *
     * @return the path of the css for this theme.
     */
    public String getCssPath() {
        return cssPath;
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
    public static UserInterfaceThemes valueOf(String name, UserInterfaceThemes defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value cannot be null.");
        try {
            return UserInterfaceThemes.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
