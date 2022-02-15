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

package eu.binjr.core.data.workspace;

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.DataAdapterException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public interface Rangeable<T> {

    /**
     * Returns all the {@link TimeSeriesInfo} for this the {@link Rangeable}
     *
     * @return all the {@link TimeSeriesInfo} for this the {@link Rangeable}
     */
    List<? extends TimeSeriesInfo<T>> getSeries();

    /**
     * Returns the preferred time range to initialize a new {@link Rangeable} with.
     *
     * @return the preferred time range to initialize a new {@link Rangeable} with.
     * @throws DataAdapterException if an error occurs while fetching data from an adapter.
     */
    default TimeRange getInitialTimeRange() throws DataAdapterException {
        ZonedDateTime end = null;
        ZonedDateTime beginning = null;

        Map<DataAdapter<T>, List<TimeSeriesInfo<T>>> bindingsByAdapters =
                getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            var adapter = byAdapterEntry.getKey();
            // Group all queries with the same adapter and path
            var bindingsByPath =
                    byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
            for (var byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                var timeRange = adapter.getInitialTimeRange(path, byPathEntry.getValue());
                if (end == null || timeRange.getEnd().isAfter(end)) {
                    end = timeRange.getEnd();
                }
                if (beginning == null || timeRange.getEnd().isBefore(beginning)) {
                    beginning = timeRange.getBeginning();
                }
            }
        }
        return TimeRange.of(
                beginning == null ? ZonedDateTime.now().minusHours(24) : beginning,
                end == null ? ZonedDateTime.now() : end);
    }
}
