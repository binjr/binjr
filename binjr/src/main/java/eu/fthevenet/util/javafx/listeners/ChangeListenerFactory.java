/*
 *    Copyright 2018 Frederic Thevenet
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

package eu.fthevenet.util.javafx.listeners;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChangeListenerFactory implements AutoCloseable {
    private final Map<Property, Map<ChangeListener, WeakChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<Property, Map<InvalidationListener, WeakInvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, Map<ListChangeListener, WeakListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();

    public void attachListener(Property<?> property, ChangeListener<?> listener) {
        property.addListener(changeListeners.computeIfAbsent(property, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(listener, WeakChangeListener::new));
    }

    public void attachListener(Property<?> property, InvalidationListener listener) {
        property.addListener(invalidationListeners.computeIfAbsent(property, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(listener, WeakInvalidationListener::new));
    }

    public void attachListener(ObservableList<?> list, ListChangeListener<?> listener) {
        list.addListener(listChangeListeners.computeIfAbsent(list, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(listener, WeakListChangeListener::new));
    }

    public void detachListener(Property<?> property, ChangeListener<Object> listener) {
        property.removeListener(listener);

    }

    public void detachListener(Property<?> property, InvalidationListener listener) {
        property.removeListener(listener);
    }

    public void detachListener(ObservableList<?> list, ListChangeListener<Object> listener) {
        list.removeListener(listener);
    }

    public void detachAllListeners(Property<?> property) {
        detachAllChangeListeners(property);
        detachAllInvalidationListeners(property);
    }

    public void detachAllInvalidationListeners(Property<?> property) {
        invalidationListeners.get(property).forEach((k, v) -> property.removeListener(v));
    }

    public void detachAllChangeListeners(Property<?> property) {
        changeListeners.get(property).forEach((k, v) -> property.removeListener(v));
    }

    public void detachAllListChangeListeners(ObservableList<?> list) {
        Map<ListChangeListener, WeakListChangeListener> m = listChangeListeners.get(list);
        m.values().forEach(v -> {
            list.removeListener(v);
        });
        //      .forEach((k, v) -> list.removeListener(v));
    }

    @Override
    public void close() {
        invalidationListeners.keySet().forEach(this::detachAllChangeListeners);
        invalidationListeners.clear();

        changeListeners.keySet().forEach(this::detachAllChangeListeners);
        changeListeners.clear();

        //listChangeListeners.keySet().forEach(this::detachAllListChangeListeners);
        listChangeListeners.forEach((list, map) -> map.values().forEach(list::removeListener));
        //  listChangeListeners.clear();
    }
}
