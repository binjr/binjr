/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.core.data.adapters;

import eu.binjr.common.logging.Logger;
import eu.binjr.common.plugins.ServiceLoaderHelper;
import eu.binjr.common.version.Version;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.ReflectionHelper;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.Node;
import javafx.scene.control.Dialog;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Defines methods to discover and create new instances of available {@link DataAdapter} classes
 *
 * @author Frederic Thevenet
 */
public class DataAdapterFactory {
    private static final Logger logger = Logger.create(DataAdapterFactory.class);
    private final Map<String, DataAdapterInfo> registeredAdapters = new ConcurrentHashMap<>();
    private final static Version MINIMUM_API_LEVEL = Version.parseVersion(AppEnvironment.MINIMUM_PLUGIN_API_LEVEL);
    private final static Version CURRENT_API_LEVEL = Version.parseVersion(AppEnvironment.PLUGIN_API_LEVEL);

    /**
     * Initializes a new instance if the {@link DataAdapterFactory} class.
     */
    private DataAdapterFactory() {
        // An exception here could prevent the app from  starting
        try {
            var pluginPaths = new ArrayList<Path>();
            var adapters = new HashSet<DataAdapterInfo>();
            // Load from classpath
            ServiceLoaderHelper.loadFromClasspath(DataAdapterInfo.class, adapters);
            // Add system plugin location
            pluginPaths.add(AppEnvironment.getInstance().getSystemPluginPath());
            // Add user plugin location
            if (UserPreferences.getInstance().loadPluginsFromExternalLocation.get()) {
                pluginPaths.add(UserPreferences.getInstance().userPluginsLocation.get());
            }
            var ucl = ServiceLoaderHelper.loadFromPaths(DataAdapterInfo.class,adapters,  pluginPaths);
            // Scan loaded plugins for serialisable types
            ReflectionHelper.INSTANCE.scanClassLoader(ucl);
            // register adapters
            for (var dataAdapterInfo : adapters) {
                if (dataAdapterInfo.getApiLevel().compareTo(MINIMUM_API_LEVEL) < 0) {
                    logger.warn("Cannot load plugin " +
                            dataAdapterInfo.getName() +
                            ": Its target API level (" +
                            dataAdapterInfo.getApiLevel() +
                            ") is less than the minimum required (" +
                            MINIMUM_API_LEVEL + ")");
                } else if (dataAdapterInfo.getApiLevel().compareTo(CURRENT_API_LEVEL) > 0) {
                    logger.warn("Cannot load plugin " +
                            dataAdapterInfo.getName() +
                            ": Its target API level (" +
                            dataAdapterInfo.getApiLevel() +
                            ") is higher than this version's level (" +
                            CURRENT_API_LEVEL + ")");
                } else {
                    registeredAdapters.put(dataAdapterInfo.getKey(), dataAdapterInfo);
                }
            }
        } catch (Throwable e) {
            logger.error("Error loading adapters", e);
        }
    }

    /**
     * Gets the singleton instance of {@link DataAdapterFactory}
     *
     * @return the singleton instance of {@link DataAdapterFactory}
     */
    public static DataAdapterFactory getInstance() {
        return DataAdapterFactoryHolder.instance;
    }

    /**
     * Gets a collection of {@link DataAdapterInfo} for all active (enabled) {@link DataAdapter}
     *
     * @return a collection of {@link DataAdapterInfo} for all active (enabled) {@link DataAdapter}
     */
    public Collection<DataAdapterInfo> getActiveAdapters() {
        return registeredAdapters.values().stream()
                .filter(DataAdapterInfo::isEnabled)
                .sorted(Comparator.comparing(DataAdapterInfo::getName))
                .collect(Collectors.toList());
    }

    /**
     * Gets a collection of {@link DataAdapterInfo} for all registered {@link DataAdapter}
     *
     * @return a collection of {@link DataAdapterInfo} for all registered {@link DataAdapter}
     */
    public Collection<DataAdapterInfo> getAllAdapters() {
        return registeredAdapters.values().stream()
                .sorted(Comparator.comparing(DataAdapterInfo::getName))
                .collect(Collectors.toList());
    }

    /**
     * Returns a new instance of a registered {@link DataAdapter} as identified by the specified key
     *
     * @param info a {@link DataAdapterInfo} instance used as a key
     * @return a new instance of {@link DataAdapter}
     * @throws NoAdapterFoundException              if no registered {@link DataAdapter} could be found for the provided key
     * @throws CannotInitializeDataAdapterException if an error occurred while trying to create a new instance.
     */
    public DataAdapter newAdapter(DataAdapterInfo info) throws NoAdapterFoundException, CannotInitializeDataAdapterException {
        return newAdapter(info.getKey());
    }

    /**
     * Returns an instance of {@link DataAdapterDialog} used to gather source access parameters from the end user.
     *
     * @param key  a string that uniquely identify the type of the {@link DataAdapter}
     * @param root the root node to act as the owner of the Dialog
     * @return an instance of {@link DataAdapterDialog} used to gather source access parameters from the end user.
     * @throws NoAdapterFoundException              if no adapter matching the provided key could be found
     * @throws CannotInitializeDataAdapterException if an error occurred while trying to create a new instance.
     */
    public Dialog<Collection<DataAdapter>> getDialog(String key, Node root)
            throws NoAdapterFoundException, CannotInitializeDataAdapterException {
        try {
            return getDataAdapterInfo(key).getAdapterDialog().getDeclaredConstructor(Node.class).newInstance(root);

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of DataAdapterDialog for " + key, e);
        }
    }

    /**
     * Returns a new instance of a registered {@link DataAdapter} as identified by the specified key
     *
     * @param key a string used as a key
     * @return a new instance of {@link DataAdapter}
     * @throws NoAdapterFoundException              if no registered {@link DataAdapter} could be found for the provided key
     * @throws CannotInitializeDataAdapterException if an error occurred while trying to create a new instance.
     */
    public DataAdapter<?> newAdapter(String key) throws NoAdapterFoundException, CannotInitializeDataAdapterException {
        try {
            return getDataAdapterInfo(key).getAdapterClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of adapter " + key, e);
        }
    }

    /**
     * Returns the {@link DataAdapterInfo} for the specified key
     *
     * @param key the key that identifies the {@link DataAdapterInfo} to return.
     * @return the {@link DataAdapterInfo} for the specified key
     * @throws NoAdapterFoundException if no registered adapter could be found for the specified key.
     */
    public DataAdapterInfo getDataAdapterInfo(String key) throws NoAdapterFoundException {
        Objects.requireNonNull(key, "The parameter 'key' cannot be null!");
        DataAdapterInfo info = registeredAdapters.get(key);
        if (info == null) {
            throw new NoAdapterFoundException("Could not find a registered adapter for key " + key);
        }
        return info;
    }

    /**
     * Returns the {@link DataAdapterPreferences} for the specified key
     *
     * @param key the key that identifies the {@link DataAdapterPreferences} to return.
     * @return the {@link DataAdapterPreferences} for the specified key
     * @throws NoAdapterFoundException if no registered adapter could be found for the specified key.
     */
    public DataAdapterPreferences getAdapterPreferences(String key) throws NoAdapterFoundException {
        return getDataAdapterInfo(key).getPreferences();
    }

    private static class DataAdapterFactoryHolder {
        private static final DataAdapterFactory instance = new DataAdapterFactory();
    }

}
