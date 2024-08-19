/*
 *    Copyright 2020-2024 Frederic Thevenet
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

import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;

import java.util.List;

public interface Reloadable<T> extends DataAdapter<T> {

    /**
     * Cause the adapter to reload the datastore for requested time series
     *
     * @param path         the path of the data in the source
     * @param seriesInfo   the series to get data from.
     * @param reloadPolicy the policy to follow when reloading series
     * @param progress     An observable property used to report progress on the current task.
     *                     Can be null.
     * @param reloadStatus a property that tracks the reload status
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    void reload(String path,
                List<TimeSeriesInfo<T>> seriesInfo,
                ReloadPolicy reloadPolicy,
                DoubleProperty progress,
                Property<ReloadStatus> reloadStatus) throws DataAdapterException;

}
