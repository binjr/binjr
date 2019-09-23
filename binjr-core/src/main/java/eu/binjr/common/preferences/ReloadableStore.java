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

package eu.binjr.common.preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

/**
 * Provides methods to access and store, import and export items that are persisted into a {@link Preferences} sub tree.
 *
 * @param <T> The type of items managed by the store
 * @author Frederic Thevenet
 */
public abstract class ReloadableStore<T extends ReloadableStore.Reloadable> {
    private static final Logger logger = LogManager.getLogger(PreferenceFactory.class);
    protected final Preferences backingStore;
    protected final Map<String, T> storedItems = new ConcurrentHashMap<>();

    ReloadableStore(Preferences backingStore) {
        this.backingStore = backingStore;
    }

    /**
     * Resets all managed items to their default values.
     *
     * @throws BackingStoreException if an error occurs while reloading items from the backing store
     */
    public void reset() throws BackingStoreException {
        backingStore.clear();
        for (var n:backingStore.childrenNames()){
            backingStore.node(n).removeNode();
        };
        storedItems.values().forEach(T::reload);
    }

    /**
     * Reloads all managed items from the backing store.
     */
    public void reload() {
        storedItems.values().forEach(T::reload);
    }

    /**
     * Export all managed items to a file.
     *
     * @param savePath the path to export the data to.
     * @throws IOException           if an IO error occrurs.
     * @throws BackingStoreException if an error occurs while accessing the backing store.
     */
    public void exportToFile(Path savePath) throws IOException, BackingStoreException {
        try (var os = Files.newOutputStream(savePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
            backingStore.flush();
            backingStore.exportSubtree(os);
        }
    }

    /**
     * Imports items into the store from a file.
     *
     * @param savePath the path to import data from.
     * @throws IOException                       if an IO error occrurs.
     * @throws InvalidPreferencesFormatException if an error occurs while accessing the backing store.
     */
    public void importFromFile(Path savePath) throws IOException, InvalidPreferencesFormatException {
        try (var is = Files.newInputStream(savePath, StandardOpenOption.READ)) {
            backingStore.importPreferences(is);
            storedItems.values().forEach(T::reload);
        }
    }

    /**
     * Returns all managed items.
     *
     * @return all managed items.
     */
    public Collection<T> getAll() {
        return storedItems.values();
    }

    /**
     * Request a specific item by its name.
     *
     * @param name the name of the item to retrieve/
     * @return an optional of the requested item, Optional.empty if no such item exists.
     */
    public Optional<T> getByName(String name) {
        var p = storedItems.get(name);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    /**
     * Defines a items whose value can be stored into  a {@link ReloadableStore} instance.
     */
    public static interface Reloadable {
        /**
         * Reloads the value of the item from the backing store.
         */
        void reload();
    }
}
