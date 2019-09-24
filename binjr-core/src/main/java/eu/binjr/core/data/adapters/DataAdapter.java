/*
 *    Copyright 2016-2019 Frederic Thevenet
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
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.scene.control.TreeItem;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides the means to access a data source to retrieve time series data.
 *
 * @author Frederic Thevenet
 */
public interface DataAdapter extends AutoCloseable {
    /**
     * Return a hierarchical view of all the individual bindings exposed by the underlying source.
     *
     * @return a hierarchical view of all the individual bindings exposed by the underlying source.
     * @throws DataAdapterException if an error occurs while retrieving bindings.
     */
    FilterableTreeItem<TimeSeriesBinding> getBindingTree() throws DataAdapterException;

    /**
     * Gets decoded data from the source as a map of {@link TimeSeriesProcessor}, for the time interval and {@link TimeSeriesInfo} specified.
     *
     * @param path        the path of the data in the source
     * @param begin       the start of the time interval.
     * @param end         the end of the time interval.
     * @param seriesInfo  the series to get data from.
     * @param bypassCache true if adapter cache should be bypassed, false otherwise. This parameter is ignored if adapter does not support caching
     * @return the output stream in which to return data.
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    Map<TimeSeriesInfo, TimeSeriesProcessor> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo> seriesInfo, boolean bypassCache) throws DataAdapterException;

    /**
     * Gets the encoding used to decode textual data sent by the source.
     *
     * @return the encoding used to decode textual data sent by the source.
     */
    String getEncoding();

    /**
     * Gets the id of the time zone used to record dates in the source.
     *
     * @return the id of the time zone used to record dates in the source.
     */
    ZoneId getTimeZoneId();

    /**
     * Gets the name of the source.
     *
     * @return the name of the source.
     */
    String getSourceName();

    /**
     * Returns a map of all parameters required to establish a connection to the underlying data source
     *
     * @return a map of all parameters required to establish a connection to the underlying data source
     */
    Map<String, String> getParams();

    /**
     * Sets the parameters required to establish a connection to the underlying data source
     *
     * @param params the parameters required to establish a connection to the underlying data source
     * @throws DataAdapterException if an error occurs while loading parameters
     */
    void loadParams(Map<String, String> params) throws DataAdapterException;

    /**
     * An api hook that is executed once, after parameters have been loaded and before any other call to the {@link DataAdapter} is made.
     * <p>Used to initialize resources and  start-up external components.</p>
     * <p>The default implementation does nothing.</p>
     *
     * @throws DataAdapterException if an error occurs.
     */
    void onStart() throws DataAdapterException;

    /**
     * Gets the unique id for the adapter
     *
     * @return the unique id for the adapter
     */
    UUID getId();

    /**
     * Sets the unique id for the adapter
     *
     * @param id the unique id for the adapter
     */
    void setId(UUID id);

    /**
     * Returns true is the adapter is closed, false otherwise.
     *
     * @return true is the adapter is closed, false otherwise.
     */
    boolean isClosed();

    /**
     * Return true to indicate that the adapter cannot guarantee that samples will be ordered by monotonally increasing
     * timestamps, false otherwise.
     *
     * @return true to indicate that the adapter cannot guarantee that samples will be ordered by monotonally increasing
     * timestamps, false otherwise.
     */
    default boolean isSortingRequired() {
        return false;
    }

    @Override
    void close();
}
