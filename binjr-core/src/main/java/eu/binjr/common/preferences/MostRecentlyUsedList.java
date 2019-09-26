/*
 *    Copyright 2019 Frederic Thevenet
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.prefs.Preferences;

/**
 * Manages access to a persistable, Last In First Out, auto de-duplicating collection, whose main purpose is to
 * handle "most recently used" item lists in UI applications.
 *
 * @param <T> The type of item to collect
 * @author Frederic Thevenet
 */
public abstract class MostRecentlyUsedList<T> implements ReloadableItemStore.Reloadable {
    private static final Logger logger = LogManager.getLogger(MostRecentlyUsedList.class);
    private final int capacity;
    private final String key;
    private final Preferences backingStore;
    ReadWriteLockHelper monitor = new ReadWriteLockHelper(new ReentrantReadWriteLock());
    private final Deque<T> mruList;

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
                    mruList.removeLast();
                }
                failSafeSave();
            });
        }
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
