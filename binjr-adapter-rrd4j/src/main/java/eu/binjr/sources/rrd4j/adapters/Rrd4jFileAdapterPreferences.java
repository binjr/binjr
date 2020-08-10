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

package eu.binjr.sources.rrd4j.adapters;

import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;


/**
 * Defines the preferences associated with the RRD4J adapter.
 *
 * @author Frederic Thevenet
 */
public class Rrd4jFileAdapterPreferences extends DataAdapterPreferences {

    public final ObservablePreference<Rrd4jBackendType> rrd4jBackend =
            enumPreference(Rrd4jBackendType.class, "rrd4jBackend", Rrd4jBackendType.NIO);

    public Rrd4jFileAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
