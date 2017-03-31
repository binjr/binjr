package eu.fthevenet.util.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Finite capacity map with a Least Recent Used eviction policy
 *
 * @param <K> type of keys
 * @param <V> type of values
 * @author Frederic Thevenet
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private int cacheSize;

    /**
     * Initializes a new instance of the {@link LRUMap} class with the specified capacity
     *
     * @param capacity the maximum capacity for the {@link LRUMap}
     */
    public LRUMap(int capacity) {
        super(16, 0.75f, true);
        this.cacheSize = capacity;
    }

    /**
     * Initializes a new instance of the {@link LRUMap} class with the specified capacity and initial values
     *
     * @param capacity the maximum capacity for the {@link LRUMap}
     * @param values   initial values to populate the {@link LRUMap}
     */
    public LRUMap(int capacity, Map<? extends K, ? extends V> values) {
        this(capacity);
        putAll(values);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > cacheSize;
    }
}