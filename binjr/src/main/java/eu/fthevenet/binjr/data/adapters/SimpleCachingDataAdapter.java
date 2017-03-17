package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.util.io.IOUtils;
import eu.fthevenet.util.logging.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.ref.SoftReference;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An abstract implementation of {@link DataAdapter} that manages a cache in between the adapter and the data source.
 * <p>This is a naive, by-value, on-heap memory cache based on a map of {@link java.lang.ref.SoftReference} </p>
 *
 * @author Frederic Thevenet
 */
public abstract class SimpleCachingDataAdapter<T extends Number> implements DataAdapter<T> {
    private static final Logger logger = LogManager.getLogger(SimpleCachingDataAdapter.class);

    private final Map<String, SoftReference<ByteArrayOutputStream>> cache;

    public SimpleCachingDataAdapter() {
        cache = new HashMap<>();
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
                try(Profiler p = Profiler.start((e)-> logger.trace(()-> "Copied " + copied.get() + " bytes in " + e.getMicros() + "ms"))) {
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

    public abstract InputStream onCacheMiss(String path, Instant begin, Instant end) throws DataAdapterException;

    @Override
    public void close() throws Exception {
        this.cache.clear();
    }

}
