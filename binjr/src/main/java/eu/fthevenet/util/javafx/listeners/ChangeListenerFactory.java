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
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChangeListenerFactory implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ChangeListenerFactory.class);
    private final Map<Property, Set<ChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<Property, Set<InvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, Set<ListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();

    public void attachListener(Property<?> property, ChangeListener listener) {
        if (changeListeners.computeIfAbsent(property, p -> new HashSet<>()).add(listener)) {
            logger.trace(() -> "Attaching ChangeListener " + listener.toString() + " to property " + property.toString());
            property.addListener(listener);
        }
        else {
            logger.trace(() -> "ChangeListener " + listener.toString() + " already attached to property" + property.toString());
        }
    }

    public void attachListener(Property<?> property, InvalidationListener listener) {
        if (invalidationListeners.computeIfAbsent(property, p -> new HashSet<>()).add(listener)) {
            logger.trace(() -> "Attaching InvalidationListener " + listener.toString() + " to property " + property.toString());
            property.addListener(listener);
        }
        else {
            logger.trace(() -> "InvalidationListener " + listener.toString() + " already attached to property" + property.toString());
        }
    }

    public void attachListener(ObservableList<?> list, ListChangeListener listener) {
        if (listChangeListeners.computeIfAbsent(list, p -> new HashSet<>()).add(listener)) {
            logger.trace(() -> "Attaching ListChangeListener " + listener.toString() + " to list " + list.toString());
            list.addListener(listener);
        }
        else {
            logger.trace(() -> "ListChangeListener " + listener.toString() + " already attached to list" + list.toString());
        }
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
        invalidationListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing InvalidationListener " + listener.toString() + " to property " + property.toString());
        });
    }

    public void detachAllChangeListeners(Property<?> property) {
        changeListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing ChangeListener " + listener.toString() + " to property " + property.toString());
        });
    }

    public void detachAllListChangeListeners(ObservableList<?> list) {
        listChangeListeners.get(list).forEach(listener -> {
            list.removeListener(listener);
            logger.trace(() -> "Removing ListChangeListener " + listener.toString() + " to property " + list.toString());
        });
    }

    @Override
    public void close() {
        invalidationListeners.keySet().forEach(this::detachAllChangeListeners);
        invalidationListeners.clear();
        changeListeners.keySet().forEach(this::detachAllChangeListeners);
        changeListeners.clear();
        listChangeListeners.forEach((list, set) -> set.forEach((listener -> {
            list.removeListener(listener);
            logger.trace(() -> "Removing ListChangeListener " + listener.toString() + " to property " + list.toString());
        })));
        //   listChangeListeners.clear();
    }
}
