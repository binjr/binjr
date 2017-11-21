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
import eu.fthevenet.binjr.sources.csv.adapters.CsvFileAdapter;
import eu.fthevenet.binjr.sources.csv.adapters.CsvFileAdapterDialog;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsAdapterDialog;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsDataAdapter;
import javafx.scene.Node;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines methods to discover and create new instances of available {@link DataAdapter} classes
 *
 * @author Frederic Thevenet
 */
public class DataAdapterFactory {
    private final Map<String, DataAdapterInfo> registeredAdapters;

    private static class DataAdapterFactoryHolder {
        private static final DataAdapterFactory instance = new DataAdapterFactory();
    }

    private DataAdapterFactory() {
        registeredAdapters = new HashMap<>();
        this.loadAdapters();
    }

    private void loadAdaptersFromJar(URI location) {
        //TODO: Make it possible to load adapters from external jars
    }

    private void loadAdapters() {
        //TODO: Use introspection to dynamically discover adapters
        DataAdapterInfo[] info = new DataAdapterInfo[]{
                new DataAdapterInfo("JRDS", "JRDS Data Adapter", JrdsDataAdapter.class, null, JrdsAdapterDialog.class),
                new DataAdapterInfo("CSV File", "CSV File Data Adapter", CsvFileAdapter.class, null, CsvFileAdapterDialog.class)
        };
        for (DataAdapterInfo i : info) {
            registeredAdapters.put(i.getKey(), i);
        }
    }

    private DataAdapterInfo getInfo(String key) throws NoAdapterFoundException {
        DataAdapterInfo info = registeredAdapters.get(Objects.requireNonNull(key, "The parameter 'key' cannot be null!"));
        if (info == null) {
            throw new NoAdapterFoundException("Could not find a registered adapter for key " + key);
        }
        return info;
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

    public DataAdapterDialog getDialog(String key, Node root) throws NoAdapterFoundException, CannotInitializeDataAdapterException {

        try {
            return getInfo(key).getAdapterDialog().getDeclaredConstructor(Node.class).newInstance(root);


        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of adapter " + key, e);
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
            return getInfo(key).getAdapterClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of adapter " + key, e);
        }
    }

    /**
     * An immutable representation of a {@link DataAdapter}'s metadata
     *
     * @author Frederic Thevenet
     */
    public class DataAdapterInfo {
        private final String name;
        private final String description;
        private final Class<? extends DataAdapter> adapterClass;
        private final URI jarUri;
        private final Class<? extends DataAdapterDialog> adapterDialog;

        public DataAdapterInfo(String name, String description, Class<? extends DataAdapter> adapterClass, URI jarUri, Class<? extends DataAdapterDialog> dialogClass) {
            this.name = name;
            this.description = description;
            this.adapterClass = adapterClass;
            this.jarUri = jarUri;
            this.adapterDialog = dialogClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public URI getJarUri() {
            return jarUri;
        }

        public Class<? extends DataAdapter> getAdapterClass() {
            return adapterClass;
        }

        public String getKey() {
            return adapterClass.getName();
        }

        public Class<? extends DataAdapterDialog> getAdapterDialog() {
            return adapterDialog;
        }
    }
}
