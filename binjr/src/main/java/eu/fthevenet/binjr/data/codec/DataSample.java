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

public class DataSample<T> {
    private final ZonedDateTime timeStamp;
    private final Map<String, T> cells;

    public DataSample(ZonedDateTime timeStamp) {
        this.timeStamp = timeStamp;
        this.cells = new HashMap<>();
    }

    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }

    public Map<String, T> getCells() {
        return cells;
    }
}
