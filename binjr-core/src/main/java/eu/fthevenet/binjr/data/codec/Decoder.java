/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.binjr.data.codec;

import eu.fthevenet.binjr.data.adapters.SerializedDataAdapter;
import eu.fthevenet.binjr.data.exceptions.DecodingDataFromAdapterException;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides the means to decode data retreived from a data source via a {@link SerializedDataAdapter}
 *
 * @author Frederic Thevenet
 */
public interface Decoder<T> {

    /**
     * Decode a stream of data into a map of {@link TimeSeriesProcessor} instances.
     *
     * @param in          the input stream to decode.
     * @param seriesNames the name of the series to extract from the stream
     * @return a map of {@link TimeSeriesProcessor} instances.
     * @throws IOException                      in the event of an IO error
     * @throws DecodingDataFromAdapterException in the event of an decoding error
     */
    Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> decode(InputStream in, List<TimeSeriesInfo<T>> seriesNames) throws IOException, DecodingDataFromAdapterException;
}
