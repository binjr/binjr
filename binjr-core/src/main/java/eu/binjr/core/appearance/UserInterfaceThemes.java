/*
 *    Copyright 2019-2023 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.common.plugins.ServiceLoaderHelper;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;

import java.nio.file.Path;
import java.util.*;

/**
 * Defines user interface themes
 *
 * @author Frederic Thevenet
 */
public interface UserInterfaceThemes {
    Logger logger = Logger.create(UserInterfaceThemes.class);

    /**
     * A holder class for a singleton instance of set of {@link UserInterfaceThemes}
     */
    class UiThemesHolder {
        private final static Set<UserInterfaceThemes> instance = loadUiThemes();

        private static Set<UserInterfaceThemes> loadUiThemes() {
            Set<UserInterfaceThemes> themes = new HashSet<>(Arrays.asList(BuiltInUserInterfaceThemes.values()));
            try {
                var pluginPaths = new ArrayList<Path>();
                Set<UserInterfaceThemes> loadedThemes = new HashSet<>();
                // Load from classpath
                ServiceLoaderHelper.loadFromClasspath(UserInterfaceThemes.class, loadedThemes);
                // Add system plugin location
                pluginPaths.add(AppEnvironment.getInstance().getSystemPluginPath());
                // Add user plugin location
                if (UserPreferences.getInstance().loadPluginsFromExternalLocation.get()) {
                    pluginPaths.add(UserPreferences.getInstance().userPluginsLocation.get());
                }
                ServiceLoaderHelper.loadFromPaths(UserInterfaceThemes.class, loadedThemes, pluginPaths);
                themes.addAll(loadedThemes);
            } catch (Throwable t) {
                logger.error("Failed to load UserInterfaceThemes from plugin: " + t.getMessage());
                logger.debug(() -> "Complete stack", t);
            }
            return themes;
        }
    }

    static Set<UserInterfaceThemes> getRegisteredUiThemes() {
        return UiThemesHolder.instance;
    }

    /**
     * Returns all the registered instance of the {@link UserInterfaceThemes} interface.
     *
     * @return all the registered instance of the {@link UserInterfaceThemes} interface.
     */
    static UserInterfaceThemes[] values() {
        return getRegisteredUiThemes().toArray(UserInterfaceThemes[]::new);
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
            for (var t : getRegisteredUiThemes()) {
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
