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

package eu.binjr.core.data.adapters;

import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.common.preferences.ObservablePreferenceFactory;
import eu.binjr.core.preferences.UserPreferences;

public class DataAdapterPreferences extends ObservablePreferenceFactory {

    public final ObservablePreference<Boolean> enabled;

    public DataAdapterPreferences(Class<? extends DataAdapter> dataAdapterClass){
        this(dataAdapterClass, true);
    }

    public DataAdapterPreferences(Class<? extends DataAdapter> dataAdapterClass, boolean enabledByDefault) {
        super(UserPreferences.BINJR_GLOBAL + "/adapters/" + dataAdapterClass.getName());
        enabled = booleanPreference("adapterEnabled", enabledByDefault);
    }
}
