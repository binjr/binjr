/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.adapters.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.parsers.DataParser;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import javafx.scene.control.TreeItem;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Provides the means to access a data source to retrieve raw time series data, as well as properties needed to present them in the view.
 *
 * @author Frederic Thevenet
 */
public abstract class DataAdapter<T extends Number> implements AutoCloseable {
    private UUID id = UUID.randomUUID();

    /**
     * Return a hierarchical view of all the individual bindings exposed by the underlying source.
     *
     * @return a hierarchical view of all the individual bindings exposed by the underlying source.
     * @throws DataAdapterException if an error occurs while retrieving bindings.
     */
    public abstract TreeItem<TimeSeriesBinding<T>> getBindingTree() throws DataAdapterException;

    /**
     * Gets raw data from the source as an output stream, for the time interval specified.
     *
     * @param path        the path of the data in the source
     * @param begin       the start of the time interval.
     * @param end         the end of the time interval.*
     * @param bypassCache true if adapter cache should be bypassed, false otherwise. This parameter is ignored if adapter does not support caching
     * @return the output stream in which to return data.
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    public abstract InputStream getData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException;

    /**
     * Gets the encoding used to decode textual data sent by the source.
     *
     * @return the encoding used to decode textual data sent by the source.
     */
    public abstract String getEncoding();

    /**
     * Gets the id of the time zone used to record dates in the source.
     *
     * @return the id of the time zone used to record dates in the source.
     */
    public abstract ZoneId getTimeZoneId();

    /**
     * Gets the {@link DataParser} used to produce {@link TimeSeriesProcessor} from the source.
     *
     * @return the {@link DataParser} used to produce {@link TimeSeriesProcessor} from the source.
     */
    public abstract DataParser<T> getParser();

    /**
     * Gets the name of the source.
     *
     * @return the name of the source.
     */
    public abstract String getSourceName();

    /**
     * Returns a map of all parameters required to establish a connection to the underlying data source
     *
     * @return a map of all parameters required to establish a connection to the underlying data source
     */
    public abstract Map<String, String> getParams();

    /**
     * Sets the parameters required to establish a connection to the underlying data source
     *
     * @param params the parameters required to establish a connection to the underlying data source
     */
    public abstract void setParams(Map<String, String> params);

    public abstract boolean ping();

    /**
     * Gets the unique id for the adapter
     *
     * @return the unique id for the adapter
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique id for the adapter
     *
     * @param id the unique id for the adapter
     */
    public void setId(UUID id) {
        this.id = id;
    }


}
