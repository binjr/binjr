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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChangeListenerFactory implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ChangeListenerFactory.class);
    private final Map<Property, List<ChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<Property, List<InvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, List<ListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();

    public void attachListener(Property<?> property, ChangeListener listener) {
        changeListeners.computeIfAbsent(property, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching ChangeListener " + listener.toString() + " to property " + property.toString());
        property.addListener(listener);
    }

    public void attachListener(Property<?> property, InvalidationListener listener) {
        invalidationListeners.computeIfAbsent(property, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching InvalidationListener " + listener.toString() + " to property " + property.toString());
        property.addListener(listener);

    }

    public void attachListener(ObservableList<?> list, ListChangeListener listener) {
        listChangeListeners.computeIfAbsent(list, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching ListChangeListener " + listener.toString() + " to list " + list.toString());
        list.addListener(listener);
    }

    public void detachListener(Property<?> property, ChangeListener<Object> listener) {
        property.removeListener(listener);
        if (changeListeners.get(property) != null) {
            Optional<ChangeListener> found = changeListeners.get(property).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                changeListeners.get(property).remove(found);
            }
        }
    }

    public void detachListener(Property<?> property, InvalidationListener listener) {
        property.removeListener(listener);
        if (invalidationListeners.get(property) != null) {
            Optional<InvalidationListener> found = invalidationListeners.get(property).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                invalidationListeners.get(property).remove(found);
            }
        }
    }

    public void detachListener(ObservableList<?> list, ListChangeListener<Object> listener) {
        list.removeListener(listener);
        if (listChangeListeners.get(list) != null) {
            Optional<ListChangeListener> found = listChangeListeners.get(list).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                listChangeListeners.get(list).remove(found);
            }
        }
    }

    public void detachAllInvalidationListeners(Property<?> property) {
        invalidationListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing InvalidationListener " + listener.toString() + " to property " + property.toString());
        });
        invalidationListeners.remove(property);
    }

    public void detachAllChangeListeners(Property<?> property) {
        changeListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing ChangeListener " + listener.toString() + " to property " + property.toString());
        });
        changeListeners.remove(property);
    }

    public void detachAllListChangeListeners(ObservableList<?> list) {
        listChangeListeners.get(list).forEach(listener -> {
            list.removeListener(listener);
            logger.trace(() -> "Removing ListChangeListener " + listener.toString() + " to property " + list.toString());
        });
        listChangeListeners.remove(list);
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
        listChangeListeners.clear();
    }
}
