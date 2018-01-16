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

import eu.fthevenet.binjr.data.codec.Decoder;
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.util.cache.LRUMapCapacityBound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.time.Instant;
import java.util.Map;

/**
 * An abstract implementation of {@link DataAdapter} that manages a cache in between the adapter and the data source.
 * <p>This is an on-heap memory cache  with a finite capacity and an LRU eviction policy</p>
 * <p>Furthermore, values uses {@link java.lang.ref.SoftReference} to give the GC a chance to collect the cached elements
 * if the memory pressure becomes too high.</p>
 *
 * @author Frederic Thevenet
 */
public abstract class SimpleCachingDataAdapter<T, A extends Decoder<T>> extends DataAdapter<T, A> {
    public static final int DEFAULT_CACHE_SIZE = 128;
    private static final Logger logger = LogManager.getLogger(SimpleCachingDataAdapter.class);
    private final Map<String, SoftReference<byte[]>> cache;

    /**
     * Initializes a new instance of the {@link SimpleCachingDataAdapter} class
     */
    public SimpleCachingDataAdapter() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Initializes a new instance of the {@link SimpleCachingDataAdapter} class with the specified maximum number of entries
     *
     * @param maxCacheEntries the  maximum number of entries in the cache
     */
    public SimpleCachingDataAdapter(int maxCacheEntries) {
        cache = new LRUMapCapacityBound<>(maxCacheEntries);
    }

    @Override
    public InputStream fetchRawData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException {
        byte[] payload = null;
        String cacheEntryKey = String.format("%s%d%d", path, begin.toEpochMilli(), end.toEpochMilli());
        if (!bypassCache) {
            SoftReference<byte[]> cacheHit = cache.get(cacheEntryKey);
            payload = cacheHit != null ? cacheHit.get() : null;
        }
        if (payload == null) {
            logger.trace(() -> String.format(
                    "%s for entry %s %s %s",
                    bypassCache ? "Cache was explicitly bypassed" : "Cache miss",
                    path,
                    begin.toString(),
                    end.toString()));
            payload = onCacheMiss(path, begin, end);
            cache.put(cacheEntryKey, new SoftReference<>(payload));
        }
        else {
            logger.trace(() -> String.format("Data successfully retrieved from cache for %s %s %s", path, begin.toString(), end.toString()));
        }
        return new ByteArrayInputStream(payload);
    }

    /**
     * Gets raw data from the source as an output stream, for the time interval specified.
     *
     * @param path  the path of the data in the source
     * @param begin the start of the time interval.
     * @param end   the end of the time interval.
     * @return the data to store in the cache.
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    public abstract byte[] onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException;

    @Override
    public void close() {
        try {
            this.cache.clear();
        } catch (Exception e) {
            logger.error("Error closing SimpleCacheAdapter", e);
        }
    }

}
