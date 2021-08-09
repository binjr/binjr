/*
 *    Copyright 2019-2020 Frederic Thevenet
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

package eu.binjr.common.preferences;

import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.logging.Logger;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Manages access to a persistent, Last In First Out, auto de-duplicating collection, whose main purpose is to
 * handle "most recently used" item lists in UI applications.
 *
 * @param <T> The type of item to collect
 * @author Frederic Thevenet
 */
public abstract class MostRecentlyUsedList<T> implements ReloadableItemStore.Reloadable {
    private static final Logger logger = Logger.create(MostRecentlyUsedList.class);
    private final int capacity;
    private final String key;
    private final Preferences backingStore;
    ReadWriteLockHelper monitor = new ReadWriteLockHelper(new ReentrantReadWriteLock());
    private final Deque<T> mruList;
    private Consumer<T> onItemEvicted;

    public void setOnItemEvicted(Consumer<T> onItemEvicted) {
        this.onItemEvicted = onItemEvicted;
    }

    protected Preferences getBackingStore() {
        return backingStore;
    }

    protected String getKey() {
        return key;
    }

    protected MostRecentlyUsedList(String key, int capacity, Preferences backingStore) {
        this.key = key;
        this.backingStore = backingStore;
        this.capacity = capacity;
        mruList = failSafeLoad();
    }

    protected abstract boolean validate(T value);

    /**
     * Push an item to the top of the stack.
     * <p>
     * If the value is already present in the inner stack, it is removed from its current location and replaced on top.
     * <p>
     * If the defined capacity has been reached, the tail of the stack is culled.
     *
     * @param value the item to push on the stack.
     */
    public void push(T value) {
        if (validate(value)) {
            monitor.write().lock(() -> {
                mruList.remove(value);
                mruList.push(value);
                if (mruList.size() > capacity) {
                    var evicted = mruList.pollLast();
                    if (onItemEvicted != null && evicted != null) {
                        onItemEvicted.accept(evicted);
                    }
                }
                failSafeSave();
            });
        } else {
            logger.debug("Invalid value to push in " + key);
        }
    }

    /**
     * Returns true is the list contains the specified value, false otherwise.
     * @param value the value whose presence is to be tested
     * @return true is the list contains the specified value, false otherwise
     */
    public boolean contains(T value) {
        return monitor.read().lock(() -> mruList.contains(value));
    }

    /**
     * Remove a value from the most recently used list.
     *
     * @param value the value to remove
     */
    public void remove(T value) {
        monitor.write().lock(() -> {
            mruList.remove(value);
            failSafeSave();
        });
    }

    /**
     * Get the head of the stack, i.e. the last pushed item.
     *
     * @return the head of the stack
     */
    public Optional<T> peek() {
        var mru = monitor.read().lock(mruList::peek);
        return mru != null ? Optional.of(mru) : Optional.empty();
    }

    @Override
    public void reload() {
        monitor.write().lock(() -> {
            mruList.clear();
            mruList.addAll(failSafeLoad());
        });
    }

    /**
     * Return all the current items on the stack.
     *
     * @return all the current items on the stack.
     */
    public Collection<T> getAll() {
        return monitor.read().lock(() -> new ArrayList<>(mruList));
    }

    private void failSafeSave() {
        try {
            getBackingStore().node(getKey()).clear();
            int i = 0;
            for (T t : getAll()) {
                saveToBackend(i++, t);
            }
        } catch (Throwable t) {
            logger.error("Failed to save preference " + key + " to backend: " + t.getMessage(), t);
        }
    }

    protected abstract void saveToBackend(int index, T value);

    private Deque<T> failSafeLoad() {
        var mru = new LinkedList<T>();
        try {
            for (int i = 0; i < capacity; i++) {
                loadFromBackend(i).ifPresent(mru::add);
            }
        } catch (Throwable t) {
            logger.error("Failed to load preference " + key + " from backend: " + t.getMessage(), t);
        }
        return mru;
    }

    protected abstract Optional<T> loadFromBackend(int index);

}
