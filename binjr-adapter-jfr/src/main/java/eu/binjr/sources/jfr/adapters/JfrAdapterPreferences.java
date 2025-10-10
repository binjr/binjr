/*
 *    Copyright 2023-2025 Frederic Thevenet
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

package eu.binjr.sources.jfr.adapters;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;

/**
 * Defines the preferences associated with the Text files adapter.
 */
public class JfrAdapterPreferences extends DataAdapterPreferences {
    private static final  Gson GSON = new GsonBuilder().serializeNulls().create();

    /**
     * The default text panel font size preference.
     */
    public ObservablePreference<Number> defaultTextViewFontSize = integerPreference("defaultTextViewFontSize", 10);

    /**
     * The filters used when scanning folders in the source filesystem.
     */
    public ObservablePreference<String[]> folderFilters = objectPreference(String[].class,
            "folderFilters",
            new String[]{"*"},
            GSON::toJson,
            s -> GSON.fromJson(s, String[].class));


    /**
     * The filters used to prune file extensions to scan in the source filesystem.
     */
    public ObservablePreference<String[]> fileExtensionFilters = objectPreference(String[].class,
            "fileExtensionFilters",
            new String[]{".jfr"},
            GSON::toJson,
            s -> GSON.fromJson(s, String[].class));

    /**
     * A list of value types for the event payload that should be included
     */
    public ObservablePreference<String[]> includedEventsDataTypes = objectPreference(String[].class,
            "includedEventsDataTypes",
            new String[]{"short", "int", "long", "float", "double"},
            GSON::toJson,
            s -> GSON.fromJson(s, String[].class));

    /**
     * A list of names of events that should be excluded
     */
    public ObservablePreference<String[]> excludedEventsNames = objectPreference(String[].class,
            "excludedEventsByName",
            new String[]{"gcId", "javaThreadId", "osThreadId", "modifiers"},
            GSON::toJson,
            s -> GSON.fromJson(s, String[].class));

    /**
     * Initialize a new instance of the {@link JfrAdapterPreferences} class associated to
     * a {@link DataAdapter} instance.
     *
     * @param dataAdapterClass the associated {@link DataAdapter}
     */
    public JfrAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
