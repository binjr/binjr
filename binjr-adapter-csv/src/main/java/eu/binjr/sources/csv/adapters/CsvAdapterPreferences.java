/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;


import com.google.gson.Gson;
import eu.binjr.common.auth.JfxKrb5LoginModule;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.sources.csv.data.parsers.BuiltInCsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CustomCsvParsingProfile;

/**
 * Defines the preferences associated with the Log files adapter.
 *
 * @author Frederic Thevenet
 */
public class CsvAdapterPreferences extends DataAdapterPreferences {
    private static final Gson gson = new Gson();
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
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    /**
     * The filters used to prune file extensions to scan in the source filesystem.
     */
    public ObservablePreference<String[]> fileExtensionFilters = objectPreference(String[].class,
            "extensionFilters",
            new String[]{"*.log", "*.txt"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    /**
     * The most recently used {@link ParsingProfile}
     */
    public ObservablePreference<String> mostRecentlyUsedParsingProfile =
            stringPreference("mruCsvParsingProfile", BuiltInCsvParsingProfile.ISO.getProfileId());

    public ObservablePreference<CsvParsingProfile[]> csvTimestampParsingProfiles =
            objectPreference(CsvParsingProfile[].class,
                    "csvTimestampParsingProfiles",
                    new CsvParsingProfile[0],
                    s -> gson.toJson(s),
                    s -> gson.fromJson(s, CustomCsvParsingProfile[].class)
            );
    public ObservablePreference<String> mruEncoding = stringPreference("mruEncoding", "utf-8");

    public ObservablePreference<String> mruCsvSeparator = stringPreference("mruCsvSeparator", ";");

    public ObservablePreference<Number> mruDateColumnPosition = integerPreference("mruDateColumnPosition", 0);

    /**
     * Initialize a new instance of the {@link CsvAdapterPreferences} class associated to
     * a {@link DataAdapter} instance.
     *
     * @param dataAdapterClass the associated {@link DataAdapter}
     */
    public CsvAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
