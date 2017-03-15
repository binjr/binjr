package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.logging.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An abstract implementation of {@link DataAdapter} that manages a cache in between the adapter and the data source.
 * <p>This is a naive, by-value, on-heap memory cache based on a map of {@link java.lang.ref.SoftReference} </p>
 *
 * @author Frederic Thevenet
 */
public abstract class SimpleCachingDataAdapter<T extends Number> implements DataAdapter<T> {
    private static final Logger logger = LogManager.getLogger(SimpleCachingDataAdapter.class);
    private static final int COPY_BUFFER_SIZE = 32 * 1024;
    public static final int EOF = -1;
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
                try(Profiler p = Profiler.start("Byte streams copied (" + copied.get() + " bytes)", logger::trace)) {
                    copied.set(copyStreams(in, cached));
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

    private long fastCopyStreams(InputStream input, OutputStream output) throws IOException {
        ReadableByteChannel src = Channels.newChannel(input);
        WritableByteChannel dest = Channels.newChannel(output);
        ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_SIZE);
        long count = 0;
        while (src.read(buffer) != -1) {
            buffer.flip();
            count += dest.write(buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            count += dest.write(buffer);
        }
        return count;
    }

    private long copyStreams(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
