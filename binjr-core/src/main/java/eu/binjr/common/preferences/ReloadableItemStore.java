/*
 *    Copyright 2019-2021 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.core.Binjr;
import eu.binjr.core.preferences.AppEnvironment;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
public abstract class ReloadableItemStore<T extends ReloadableItemStore.Reloadable> {
    private static final Logger logger = Logger.create(ObservablePreferenceFactory.class);
    protected final Preferences backingStore;
    protected final ObservableMap<String, T> storedItems = FXCollections.observableMap(new ConcurrentHashMap<>());
    private ObservableMap<String, T> readOnlyStoreItems = FXCollections.unmodifiableObservableMap(storedItems);


    static public boolean readRawBoolean(String backingStoreKey, String key, boolean defaultValue){
        var pref = getBackingPreference(backingStoreKey);
        return pref.getBoolean(key, defaultValue);
    }

    static public long readRawLong(String backingStoreKey, String key, long defaultValue){
        var pref = getBackingPreference(backingStoreKey);
        return pref.getLong(key, defaultValue);
    }

    static public double readRawDouble(String backingStoreKey, String key, double defaultValue){
        var pref = getBackingPreference(backingStoreKey);
        return pref.getDouble(key, defaultValue);
    }

    static public long readRawInt(String backingStoreKey, String key, int defaultValue){
        var pref = getBackingPreference(backingStoreKey);
        return pref.getInt(key, defaultValue);
    }

    static public String readRawString(String backingStoreKey, String key, String defaultValue){
       var pref = getBackingPreference(backingStoreKey);
       return pref.get(key, defaultValue);
    }

    ReloadableItemStore(String backingStoreKey) {
        this.backingStore = getBackingPreference(backingStoreKey);
        storedItems.addListener((MapChangeListener<String, T>) c -> {
            if (c.wasAdded()) {
                logger.trace(() -> "Preference added to store: " + c.getValueAdded().toString());
            }
            if (c.wasRemoved()) {
                logger.trace(() -> "Preference removed from store: " + c.getValueAdded().toString());
            }
        });
    }

    /**
     * Resets all managed items to their default values.
     *
     * @throws BackingStoreException if an error occurs while reloading items from the backing store
     */
    public void reset() throws BackingStoreException {
        clearSubTree(backingStore);
        storedItems.values().forEach(T::reload);
    }

    private static  Preferences getBackingPreference(String name) {
        if (Boolean.parseBoolean(System.getProperty(AppEnvironment.PORTABLE_PROPERTY))) {
            try {
                var jarLocation = Paths.get(Binjr.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                var configDir = jarLocation.getParent().getParent().resolve("settings");
                if (!Files.isDirectory(configDir)) {
                    Files.createDirectory(configDir);
                }
                var preferencesFile = configDir.resolve("user").toFile();
                if (!preferencesFile.exists()) {
                    preferencesFile.createNewFile();
                }
                return new FilePreferences(preferencesFile, null, "").node(name);
            } catch (Exception e) {
                logger.error("Failed to create file to store preferences" + e.getMessage());
                logger.debug(() -> "Stack Trace", e);
                logger.warn("Non portable preference store will be used instead");
            }
        }
        return Preferences.userRoot().node(name);
    }

    private void clearSubTree(Preferences node) throws BackingStoreException {
        if (node.nodeExists("")) {
            node.clear();
            for (var n : node.childrenNames()) {
                clearSubTree(node.node(n));
            }
        }
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
            Preferences.importPreferences(is);
            storedItems.values().forEach(T::reload);
        }
    }

    /**
     * Returns all managed items.
     *
     * @return all managed items.
     */
    public ObservableMap<String, T> getAll() {
        return readOnlyStoreItems;
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
     * Defines an item whose value can be stored into  a {@link ReloadableItemStore} instance.
     */
    public interface Reloadable {
        /**
         * Reloads the value of the item from the backing store.
         */
        void reload();
    }
}
