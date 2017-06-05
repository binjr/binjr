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
import eu.fthevenet.util.cache.LRUMap;
import eu.fthevenet.util.io.IOUtils;
import eu.fthevenet.util.logging.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An abstract implementation of {@link DataAdapter} that manages a cache in between the adapter and the data source.
 * <p>This is a on-heap memory cache  with a finite capacity and an LRU eviction policy</p>
 * <p>Furthermore, values uses {@link java.lang.ref.SoftReference} to give the JVM a change to collect the cached elements,
 * should the memory pressure become to high.</p>
 *
 * @author Frederic Thevenet
 */
public abstract class SimpleCachingDataAdapter<T extends Number> extends DataAdapter<T> {
    private static final Logger logger = LogManager.getLogger(SimpleCachingDataAdapter.class);
    public static final int CACHE_SIZE = 32;

    private final Map<String, SoftReference<ByteArrayOutputStream>> cache;

    /**
     * Initializes a new instance of the {@link SimpleCachingDataAdapter} class
     */
    public SimpleCachingDataAdapter() {
        cache = new LRUMap<>(CACHE_SIZE);
    }

    @Override
    public InputStream getData(String path, Instant begin, Instant end) throws DataAdapterException {
        String cacheEntryKey = String.format("%s%d%d", path, begin.toEpochMilli(), end.toEpochMilli());
        SoftReference<ByteArrayOutputStream> cacheHit = cache.get(cacheEntryKey);
        ByteArrayOutputStream cached = cacheHit != null ? cacheHit.get() : null;
        if (cached == null) {
            logger.debug(() -> String.format("Cache miss for entry %s %s %s", path, begin.toString(), end.toString()));
            InputStream in = onCacheMiss(path, begin, end);
            try {
                cached = new ByteArrayOutputStream();
                AtomicLong copied = new AtomicLong(0);
                try (Profiler p = Profiler.start((e) -> logger.trace(() -> "Copied " + copied.get() + " bytes in " + e.getMicros() + "µs"))) {
                    copied.set(IOUtils.copyStreams(in, cached));
                }
                cache.put(cacheEntryKey, new SoftReference<>(cached));
            } catch (IOException e) {
                logger.error("Error while caching source data to cache.", e);
                try {
                    in.close();
                } catch (IOException e1) {
                    // failed attempt to close stream
                }
                logger.warn("Attempting to return data from source without caching");
                return onCacheMiss(path, begin, end);
            }
        }
        else {
            logger.debug(() -> String.format("Data successfully retrieved from cache for %s %s %s", path, begin.toString(), end.toString()));
        }
        return new ByteArrayInputStream(cached.toByteArray());
    }

    /**
     * Gets raw data from the source as an output stream, for the time interval specified.
     *
     * @param path  the path of the data in the source
     * @param begin the start of the time interval.
     * @param end   the end of the time interval.
     * @return the output stream in which to return data.
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    public abstract InputStream onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException;

    @Override
    public void close() {
        try {
            this.cache.clear();
        } catch (Exception e) {
            logger.error("Error closing SimpleCacheAdapter", e);
        }
    }

}
