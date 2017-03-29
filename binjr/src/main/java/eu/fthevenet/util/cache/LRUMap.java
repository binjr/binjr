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

    public LRUMap(int cacheSize) {
        super(16, 0.75f, true);
        this.cacheSize = cacheSize;
    }

    public LRUMap(int cacheSize, Map<? extends K, ? extends V> values) {
        this(cacheSize);
        putAll(values);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > cacheSize;
    }
}