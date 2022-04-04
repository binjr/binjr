/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.preferences.UserPreferences;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * An abstract implementation of {@link SerializedDataAdapter} that manages a cache in between the adapter and the data source.
 * <p>This is an on-heap memory cache  with a finite capacity and an LRU eviction policy</p>
 * <p>Furthermore, values uses {@link java.lang.ref.SoftReference} to give the GC a chance to collect the cached elements
 * if the memory pressure becomes too high.</p>
 *
 * @author Frederic Thevenet
 */
public abstract class SimpleCachingDataAdapter<T> extends SerializedDataAdapter<T> {
    private static final Logger logger = Logger.create(SimpleCachingDataAdapter.class);
    private final Cache<String, ByteBuffer> cache;

    /**
     * Initializes a new instance of the {@link SimpleCachingDataAdapter} class
     */
    public SimpleCachingDataAdapter() {
        this(UserPreferences.getInstance().dataAdapterFetchCacheMaxSizeMiB.get().longValue() * 1024L);
    }

    /**
     * Initializes a new instance of the {@link SimpleCachingDataAdapter} class with the specified maximum number of entries
     *
     * @param maxCacheSizeInByte the  maximum number of entries in the cache
     */
    public SimpleCachingDataAdapter(long maxCacheSizeInByte) {
        this.cache = Caffeine.newBuilder()
                .recordStats()
                .maximumWeight(maxCacheSizeInByte)
                .weigher((String key, ByteBuffer buffer) -> buffer.array().length)
                .build();
    }

    @Override
    public InputStream fetchRawData(String path, Instant begin, Instant end, boolean bypassCache) throws DataAdapterException {
        final String cacheEntryKey = String.format("%s%d%d", path, begin.toEpochMilli(), end.toEpochMilli());
        if (bypassCache) {
            cache.invalidate(cacheEntryKey);
        }
        var is = new ByteArrayInputStream(cache.get(cacheEntryKey, CheckedLambdas.wrap(key -> {
            var data = onCacheMiss(path, begin, end);
            logger.perf(() -> String.format(
                    "%s for entry %s %s %s - payload size=%d",
                    bypassCache ? "Cache was explicitly bypassed" : "Cache miss",
                    path,
                    begin,
                    end,
                    data.length));
            return ByteBuffer.wrap(data);
        })).array());
        logger.perf(this::printCacheStats);
        return is;
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

    private String printCacheStats() {
        var stats = cache.stats();
        return String.format("""
                        Data adapters fetch cache statistics:
                         - requestCount=%d
                         - hitCount=%d
                         - hitRate=%f
                         - missCount=%d
                         - missRate=%f
                         - evictionCount=%d
                         - evictionWeight=%d
                        """,
                stats.requestCount(),
                stats.hitCount(),
                stats.hitRate(),
                stats.missCount(),
                stats.missRate(),
                stats.evictionCount(),
                stats.evictionWeight()
        );
    }

    @Override
    public void close() {
        try {
            this.cache.invalidateAll();
        } catch (Exception e) {
            logger.error("Error closing SimpleCacheAdapter", e);
        }
        super.close();
    }

}
