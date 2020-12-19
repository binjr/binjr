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

package eu.binjr.sources.netdata.adapters;

import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;
import eu.binjr.sources.netdata.api.GroupingMethod;

/**
 * Defines the preferences associated with the Netdata adapter.
 *
 * @author Frederic Thevenet
 */
public class NetdataAdapterPreferences extends DataAdapterPreferences {
    /**
     * Set to true to disable server-side down-sampling (aka "grouping"). Client-side down-sampling will still be applied.
     */
    public ObservablePreference<Boolean> disableServerSideDownsampling = booleanPreference("disableServerSideDownsampling", false);

    /**
     * Set to true to disable Netdata alignment of all series on the same time-frame.
     */
    public ObservablePreference<Boolean> disableTimeFrameAlignment = booleanPreference("disableTimeFrameAlignment", true);

    /**
     * Netdata's grouping (i.e. down-sampling) method: If multiple collected values are to be grouped in order to
     * return fewer points, this parameters defines the method of grouping.
     */
    public ObservablePreference<GroupingMethod> groupingMethod = enumPreference(GroupingMethod.class, "groupingMethod", GroupingMethod.AVERAGE);

    /**
     * The grouping number of seconds.
     * This is used in conjunction with group=average to change the units of metrics
     * (ie when the data is per-second, setting gtime=60 will turn them to per-minute).
     */
    public ObservablePreference<Number> groupingTime = integerPreference("groupingTime", 0);

    /**
     * The amount of time in seconds to read after the specified time interval
     */
    public final ObservablePreference<Number> fetchReadBehindSeconds = integerPreference("fetchReadBehindSeconds", 10);

    /**
     *  The amount of time in seconds to read before the specified time interval
     */
    public final ObservablePreference<Number> fetchReadAheadSeconds = integerPreference("fetchReadAheadSeconds", 10);

    /**
     * The maximum number of samples to recover from Netdata. 0 means every available samples will be returned.
     */
    public ObservablePreference<Number> maxSamplesAllowed = integerPreference("maxSamplesAllowed", 10000);

    /**
     * Initialize a new instance of the {@link NetdataAdapterPreferences} class associated to
     * a {@link DataAdapter} instance.
     *
     * @param dataAdapterClass the associated {@link DataAdapter}
     */
    public NetdataAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
