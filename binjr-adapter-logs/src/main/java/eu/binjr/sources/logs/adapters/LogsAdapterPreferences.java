/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.sources.logs.adapters;


import com.google.gson.Gson;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.common.preferences.MruFactory;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;

import eu.binjr.core.preferences.UserPreferences;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import javafx.collections.ObservableList;

import java.util.Optional;

public class LogsAdapterPreferences extends DataAdapterPreferences {
    private static final Gson gson = new Gson();
    private final MostRecentlyUsedList<ParsingProfile> defaultParsingProfiles;
    private final MostRecentlyUsedList<ParsingProfile> userParsingProfiles;
    private final ParsingRulesMruFactory mruFactory;

    public ObservablePreference<Number> defaultTextViewFontSize = integerPreference("defaultTextViewFontSize", 10);

    public ObservablePreference<String[]> folderFilters = objectPreference(String[].class,
            "folderFilters",
            new String[]{"*"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    public ObservablePreference<String[]> fileExtensionFilters = objectPreference(String[].class,
            "fileExtensionFilters",
            new String[]{".log", ".txt"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));


    public ObservablePreference<String> mostRecentlyUsedParsingProfile =
            stringPreference("mostRecentlyUsedParsingProfile", BuiltInParsingProfile.BINJR.name());

    public LogsAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
        this.mruFactory = new ParsingRulesMruFactory(UserPreferences.BINJR_GLOBAL + "/adapters/" + LogsAdapterPreferences.class.getName());
        this.defaultParsingProfiles =mruFactory.rulesMostRecentlyUsedList("defaultParsingProfiles", 100);
        this.userParsingProfiles = mruFactory.rulesMostRecentlyUsedList("userParsingProfiles", 100);
    }

    public MostRecentlyUsedList<ParsingProfile> getDefaultParsingProfiles() {
        return defaultParsingProfiles;
    }

    public MostRecentlyUsedList<ParsingProfile> getUserParsingProfiles() {
        return userParsingProfiles;
    }


    private static class ParsingRulesMruFactory extends MruFactory {
        public ParsingRulesMruFactory(String backingStoreKey) {
            super(backingStoreKey);
        }

        public MostRecentlyUsedList<ParsingProfile> rulesMostRecentlyUsedList(String key, int capacity) {
            var mru = new MostRecentlyUsedList<ParsingProfile>(key, capacity, backingStore) {
                @Override
                protected boolean validate(ParsingProfile value) {
                    return (value != null);
                }

                @Override
                protected void saveToBackend(int index, ParsingProfile value) {
                    getBackingStore().node(getKey()).put("value_" + index, gson.toJson(value));
                }

                @Override
                protected Optional<ParsingProfile> loadFromBackend(int index) {
                    var p = getBackingStore().node(getKey()).get("value_" + index, null);
                    if (p != null) {
                        return Optional.of(gson.fromJson(p, BuiltInParsingProfile.class));
                    }
                    return Optional.empty();
                }
            };
            storedItems.put(key, mru);
            return mru;
        }
    }
}
