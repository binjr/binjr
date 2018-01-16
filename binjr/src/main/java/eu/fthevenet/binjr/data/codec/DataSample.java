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

package eu.fthevenet.binjr.data.codec;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single sample/record from a csv file
 *
 * @param <T> the type for data contained in cells
 */
public class DataSample<T> {
    private final ZonedDateTime timeStamp;
    private final Map<String, T> cells;

    /**
     * Initializes a new instance of the {@link DataSample} class.
     *
     * @param timeStamp the time stamp for the sample.
     */
    public DataSample(ZonedDateTime timeStamp) {
        this.timeStamp = timeStamp;
        this.cells = new HashMap<>();
    }

    /**
     * Returns the time stamp for the sample.
     *
     * @return time stamp for the sample.
     */
    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns a map of cells for the sample
     *
     * @return a map of cells for the sample
     */
    public Map<String, T> getCells() {
        return cells;
    }
}
