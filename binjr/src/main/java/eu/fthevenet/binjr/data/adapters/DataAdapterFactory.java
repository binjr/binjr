/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.data.adapters;


import eu.fthevenet.binjr.data.exceptions.CannotInitializeDataAdapterException;
import eu.fthevenet.binjr.data.exceptions.NoAdapterFoundException;
import eu.fthevenet.binjr.dialogs.DataAdapterDialog;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.scene.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Defines methods to discover and create new instances of available {@link DataAdapter} classes
 *
 * @author Frederic Thevenet
 */
public class DataAdapterFactory {
    private static final Logger logger = LogManager.getLogger(DataAdapterFactory.class);
    private final Map<String, DataAdapterInfo> registeredAdapters;


    private static class DataAdapterFactoryHolder {
        private static final DataAdapterFactory instance = new DataAdapterFactory();
    }

    /**
     * Initializes a new instance if the {@link DataAdapterFactory} class.
     */
    private DataAdapterFactory() {
        registeredAdapters = new HashMap<>();

        try {
            this.loadAdapters();
        } catch (IOException e) {
            Dialogs.notifyException("Fail to load plugin", e);
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
     * Gets a collection of {@link DataAdapterInfo} for all registered {@link DataAdapter}
     *
     * @return a collection of {@link DataAdapterInfo} for all registered {@link DataAdapter}
     */
    public Collection<DataAdapterInfo> getAdapterInfo() {
        return registeredAdapters.values();
    }

    /**
     * Returns a new instance of a registered {@link DataAdapter} as identified by the specified key
     *
     * @param info a {@link DataAdapterInfo} instance used as a key
     * @return a new instance of {@link DataAdapter}
     * @throws NoAdapterFoundException              if no registered {@link DataAdapter} could be found for the provided key
     * @throws CannotInitializeDataAdapterException if an error occurred while trying to create a new instance.
     */
    public DataAdapter<?, ?> newAdapter(DataAdapterInfo info) throws NoAdapterFoundException, CannotInitializeDataAdapterException {
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
    public DataAdapterDialog getDialog(String key, Node root) throws NoAdapterFoundException, CannotInitializeDataAdapterException {
        try {
            return retrieveAdapterInfo(key).getAdapterDialog().getDeclaredConstructor(Node.class).newInstance(root);

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
    public DataAdapter<?, ?> newAdapter(String key) throws NoAdapterFoundException, CannotInitializeDataAdapterException {
        try {
            return retrieveAdapterInfo(key).getAdapterClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of adapter " + key, e);
        }
    }

    private void loadAdapters() throws IOException {
        List<URL> urls = new ArrayList<>();
        if (Files.exists(GlobalPreferences.getInstance().getPluginsLocation())) {
            logger.debug(() -> "Looking for plugins in " + GlobalPreferences.getInstance().getPluginsLocation());
            PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");
            Files.walkFileTree(GlobalPreferences.getInstance().getPluginsLocation(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (jarMatcher.matches(file)) {
                        logger.debug(() -> "Inspecting " + file.getFileName() + " for DataAdapter service implementations");
                        urls.add(file.toUri().toURL());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        else {
            logger.debug("Plugins location " + GlobalPreferences.getInstance().getPluginsLocation() + " does not exist.");
        }
        Iterator<DataAdapterInfo> adapterInfoIterator = ServiceLoader.load(DataAdapterInfo.class, new URLClassLoader(urls.toArray(new URL[0]))).iterator();
        while (adapterInfoIterator.hasNext()) {
            try {
                DataAdapterInfo adapterInfo = adapterInfoIterator.next();
                registeredAdapters.put(adapterInfo.getKey(), adapterInfo);
                logger.debug(() -> "Successfully registered DataAdapterInfo " + adapterInfo.toString() + " from external JAR.");
            } catch (ServiceConfigurationError sce) {
                logger.error("Failed to load DataAdapter", sce);
            } catch (Exception e) {
                logger.error("Unexpected error while loading DataAdapter", e);
            }
        }
    }

    private DataAdapterInfo retrieveAdapterInfo(String key) throws NoAdapterFoundException {
        DataAdapterInfo info = registeredAdapters.get(Objects.requireNonNull(key, "The parameter 'key' cannot be null!"));
        if (info == null) {
            throw new NoAdapterFoundException("Could not find a registered adapter for key " + key);
        }
        return info;
    }

}
