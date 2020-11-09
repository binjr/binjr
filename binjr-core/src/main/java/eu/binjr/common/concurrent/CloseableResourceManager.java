package eu.binjr.common.concurrent;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A generic resource manager for safely initializing, sharing and disposing
 * singletons amongst multiple consumer objects, on multiple threads.
 * <p>
 * The resource manager will guarantee that only one instance for each supplied
 * key is ever is initialized and returned when required.
 * </p>
 * <p>
 * Reference counting is used to ensure that the actual closing of a resource
 * only happens once all registered consumers have released it.
 * </p>
 *
 * @param <T> Resource type
 * @author Frederic Thevenet
 */
public class CloseableResourceManager<T extends Closeable> {
    private static final Log logger = LogFactory.getLog(CloseableResourceManager.class);
    private final Map<String, ResourceHolder<T>> resources = new HashMap<>();
    private final ReadWriteLockHelper managerLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());

    /**
     * Resource holder Resource type
     *
     * @param <U> Resource type
     */
    private final class ResourceHolder<U extends AutoCloseable> {
        private AtomicInteger referenceCount = new AtomicInteger(0);
        private volatile boolean closed;
        private U instance;

        /**
         * Constructor
         *
         * @param instance Resource instance
         */
        private ResourceHolder(U instance) {
            this.instance = instance;
        }

        /**
         * Get Resource instance
         *
         * @return Resource instance
         */
        private U getInstance() {
            return instance;
        }

        /**
         * Register resource
         *
         * @return Reference count
         */
        private int register() {
            return referenceCount.incrementAndGet();
        }

        /**
         * Release resource
         *
         * @return Reference count
         * @throws Exception exception
         */
        private int release() throws Exception {
            int refLeft = referenceCount.decrementAndGet();
            if (refLeft < 1) {
                if (!closed) {
                    try {
                        instance.close();
                    } finally {
                        closed = true;
                    }
                }
            }
            return refLeft;
        }
    }

    /**
     * Registers a consumer with a resource, identified by the supplied key.
     * <p>
     * This method must only be called once per consumer (or the release methods
     * must be called accordingly), otherwise the resource may never be
     * released.
     * </p>
     *
     * @param key             the key to identify the resource to register.
     * @param resourceFactory a factory method used to build a new instance of the resource
     *                        if one doesn't already exist.
     * @return the registered resource
     */
    public T acquire(String key, Supplier<T> resourceFactory) {
        return managerLock.write().lock(() -> {
            var r = resources.computeIfAbsent(key, k -> new ResourceHolder<>(resourceFactory.get()));
            r.register();
            return r.getInstance();
        });
    }

    /**
     * Registers a consumer with a resource, identified by the supplied key.
     * <p>
     * This method must only be called once per consumer (or the release methods
     * must be called accordingly), otherwise the resource may never be
     * released.
     * </p>
     *
     * @param key      the key to identify the resource to register.
     * @param resource the resource to register
     * @return Reference count
     * @throws IndexOutOfBoundsException
     */
    public int register(String key, T resource) {
        return managerLock.write().lock(() -> {
            var r = resources.computeIfAbsent(key, k -> new ResourceHolder<>(resource));
            return r.register();
        });
    }

    /**
     * Releases the resource identified by the supplied key.
     * <p>
     * If all references to this resource are released, the resource is closed.
     * </p>
     * <p>
     * <b>Warning:</b> Registering a closed resources with the same key will
     * cause a new instance to be created.
     * </p>
     *
     * @param key the key to identify the resource to release.
     * @return the number of references left for the specified resource.
     * @throws Exception if an error occurs while closing the resource.
     */
    public int release(String key) throws Exception {
        return managerLock.write().lock(() -> {
            ResourceHolder<T> r = resources.get(key);
            if (r != null) {
                try {
                    return r.release();
                } finally {
                    if (r.closed) {
                        resources.remove(key);
                    }
                }
            } else {
                logger.warn("Trying to release a resource that is not registered.");
                return -1;
            }
        });
    }

    /**
     * Forces the release and subsequent closing of the specified resource,
     * regardless of how many references to it are still held by other
     * consumers.
     * <p>
     * Use with caution.
     * </p>
     *
     * @param key the key to identify the resource to close.
     * @throws Exception if an error occurs while closing the resource.
     */
    public void forceReleaseAndClose(String key) throws Exception {
        managerLock.write().lock(() -> {
            ResourceHolder<T> r = resources.get(key);
            if (r != null) {
                while (r.release() > 0) {
                    // Nothing
                }
            } else {
                logger.warn("Trying to close a resource that is not registered.");
            }
        });
    }

    /**
     * Returns the resource identified by the supplied key.
     *
     * @param key the key to identify the resource to release.
     * @return the resource identified by the supplied key. If there is no
     * resource mapped to the specified key, returns null.
     */
    public T get(String key) {
        return managerLock.read().lock(() -> {
            ResourceHolder<T> r = resources.get(key);
            if (r != null) {
                return r.getInstance();
            }
            return null;
        });
    }

    /**
     * Returns true if a resource identified by the provided key is registered,
     * false otherwise.
     *
     * @param key the key to identify the resource.
     * @return true if a resource identified by the provided key is registered,
     * false otherwise.
     */
    public boolean isRegistered(String key) {
        return managerLock.read().lock(() -> resources.containsKey(key));
    }
}
