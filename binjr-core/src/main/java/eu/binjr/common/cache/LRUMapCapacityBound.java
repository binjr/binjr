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
 * Finite capacity map with a Least Recent Used eviction policy
 *
 * @param <K> type of keys
 * @param <V> type of values
 * @author Frederic Thevenet
 */
public class LRUMapCapacityBound<K, V> extends LinkedHashMap<K, V> {
    private int cacheSize;

    /**
     * Initializes a new instance of the {@link LRUMapCapacityBound} class with the specified capacity
     *
     * @param capacity the maximum capacity for the {@link LRUMapCapacityBound}
     */
    public LRUMapCapacityBound(int capacity) {
        super(16, 0.75f, true);
        this.cacheSize = capacity;
    }

    /**
     * Initializes a new instance of the {@link LRUMapCapacityBound} class with the specified capacity and initial values
     *
     * @param capacity the maximum capacity for the {@link LRUMapCapacityBound}
     * @param values   initial values to populate the {@link LRUMapCapacityBound}
     */
    public LRUMapCapacityBound(int capacity, Map<? extends K, ? extends V> values) {
        this(capacity);
        putAll(values);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > cacheSize;
    }
}