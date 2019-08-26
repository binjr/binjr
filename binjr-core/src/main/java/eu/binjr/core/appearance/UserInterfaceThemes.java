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

package eu.binjr.core.appearance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Defines user interface themes
 *
 * @author Frederic Thevenet
 */
public interface UserInterfaceThemes {
    static final Logger logger = LogManager.getLogger(UserInterfaceThemes.class);
    Set<UserInterfaceThemes> registeredUiThemes = loadUiThemes();

    private static Set<UserInterfaceThemes> loadUiThemes() {
        Set<UserInterfaceThemes> themes = new HashSet<>(Arrays.asList(BuiltInUserInterfaceThemes.values()));
        try {
            for (UserInterfaceThemes userInterfaceTheme : ServiceLoader.load(UserInterfaceThemes.class)) {
                try {
                    themes.add(userInterfaceTheme);
                    logger.debug(() -> "Successfully registered UserInterfaceTheme " + userInterfaceTheme.toString());
                } catch (ServiceConfigurationError sce) {
                    logger.error("Failed to load UserInterfaceTheme", sce);
                } catch (Exception e) {
                    logger.error("Unexpected error while loading UserInterfaceTheme", e);
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to load UserInterfaceThemes from plugin: " + t.getMessage());
            logger.debug(() -> "Complete stack", t);
        }
        return themes;
    }

    /**
     * Returns all the registered instance of the {@link UserInterfaceThemes} interface.
     *
     * @return all the registered instance of the {@link UserInterfaceThemes} interface.
     */
    static UserInterfaceThemes[] values() {
        return registeredUiThemes.toArray(UserInterfaceThemes[]::new);
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
            return BuiltInUserInterfaceThemes.valueOf(name);
        } catch (IllegalArgumentException e) {
            for (var t : registeredUiThemes) {
                if (t.name().equals(name)) {
                    return t;
                }
            }
            return defaultValue;
        }
    }

    /**
     * Returns the path of the css for this theme.
     *
     * @return the path of the css for this theme.
     */
    String getCssPath();

    /**
     * Returns the constant name for the UI theme
     *
     * @return the constant name for the UI theme
     */
    String name();
}
