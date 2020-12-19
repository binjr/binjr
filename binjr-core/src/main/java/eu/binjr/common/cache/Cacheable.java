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

/**
 * Indicates that the implementing class can be cached using {@link LRUMapCapacityBound} or {@link LRUMapSizeBound}
 *
 * @param <T> The type for cached values.
 */
public interface Cacheable<T> {
    /**
     * Returns the cached value
     *
     * @return the cached value/
     */
    T getValue();

    /**
     * Returns the size in bytes of the cached value.
     *
     * @return The size in bytes of the cached value.
     */
    long getSize();

}
