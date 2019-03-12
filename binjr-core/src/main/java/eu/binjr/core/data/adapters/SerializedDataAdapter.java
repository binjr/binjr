/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.core.data.codec.Decoder;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides the means to access a data source to retrieve raw time series data while decoding that data into
 * {@link TimeSeriesProcessor} instances is delegated to an associated {@link Decoder} instance.
 *
 * @author Frederic Thevenet
 */
public abstract class SerializedDataAdapter extends BaseDataAdapter {
    private static final Logger logger = LogManager.getLogger(SerializedDataAdapter.class);
    private UUID id = UUID.randomUUID();
    protected volatile boolean closed = false;

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
    public abstract InputStream fetchRawData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException;

    @Override
    public Map<TimeSeriesInfo, TimeSeriesProcessor> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo> seriesInfo, boolean bypassCache)
            throws DataAdapterException {
        if (closed) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        try (InputStream in = this.fetchRawData(path, begin, end, bypassCache)) {
            // Parse raw data obtained from adapter
            return this.getDecoder().decode(in, seriesInfo);
        } catch (IOException e) {
            throw new DataAdapterException("Error recovering data from source", e);
        }
    }

    /**
     * Gets the {@link Decoder} used to produce {@link TimeSeriesProcessor} from the source.
     *
     * @return the {@link Decoder} used to produce {@link TimeSeriesProcessor} from the source.
     */
    public abstract Decoder getDecoder();

}
