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

package eu.binjr.common.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map witch evicts the least recently used element when a maximum size of stored element is reached.
 *
 * <p><i><b>NB: </b>This implementation recalculates the combined size of all stored elements each time a new element is added,
 * so it is not well suited to pushing a large number of cached items at a high rate.</i></p>
 *
 * @param <K> type of keys
 * @param <V> type of values
 * @author Frederic Thevenet
 */
@Deprecated
public class LRUMapSizeBound<K, V extends Cacheable> extends LinkedHashMap<K, V> {
    private final long maxSize;

    /**
     * Initializes a new instance of the {@link LRUMapSizeBound} class with the specified capacity
     *
     * @param maxSize the maximum capacity for the {@link LRUMapSizeBound}
     */
    public LRUMapSizeBound(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    /**
     * Initializes a new instance of the {@link LRUMapSizeBound} class with the specified capacity and initial values
     *
     * @param maxSize the maximum capacity for the {@link LRUMapSizeBound}
     * @param values  initial values to populate the {@link LRUMapSizeBound}
     */
    public LRUMapSizeBound(int maxSize, Map<? extends K, ? extends V> values) {
        this(maxSize);
        putAll(values);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.values().stream().map(Cacheable::getSize).reduce(0L, Long::sum) > maxSize;
    }
}