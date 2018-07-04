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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChangeListenerFactory implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ChangeListenerFactory.class);
    private final Map<ObservableValue, List<ChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<ObservableValue, List<InvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, List<ListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();
    private Hashtable<String, String> h;

    @SuppressWarnings("unchecked")
    public void attachListener(ObservableValue<?> property, ChangeListener listener) {
        changeListeners.computeIfAbsent(property, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching ChangeListener " + listener.toString() + " to property " + property.toString());
        property.addListener(listener);
    }

    public void attachListener(ObservableValue<?> property, InvalidationListener listener) {
        invalidationListeners.computeIfAbsent(property, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching InvalidationListener " + listener.toString() + " to property " + property.toString());
        property.addListener(listener);

    }

    @SuppressWarnings("unchecked")
    public void attachListener(ObservableList<?> list, ListChangeListener listener) {
        listChangeListeners.computeIfAbsent(list, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching ListChangeListener " + listener.toString() + " to list " + list.toString());
        list.addListener(listener);
    }

    @SuppressWarnings("unchecked")
    public void detachListener(ObservableValue<?> property, ChangeListener listener) {
        property.removeListener(listener);
        if (changeListeners.get(property) != null) {
            Optional<ChangeListener> found = changeListeners.get(property).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                changeListeners.get(property).remove(found);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void detachListener(ObservableValue<?> property, InvalidationListener listener) {
        property.removeListener(listener);
        if (invalidationListeners.get(property) != null) {
            Optional<InvalidationListener> found = invalidationListeners.get(property).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                invalidationListeners.get(property).remove(found);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void detachListener(ObservableList<?> list, ListChangeListener<Object> listener) {
        list.removeListener(listener);
        if (listChangeListeners.get(list) != null) {
            Optional<ListChangeListener> found = listChangeListeners.get(list).stream().filter(l -> l.equals(listener)).findFirst();
            if (found.isPresent()) {
                listChangeListeners.get(list).remove(found);
            }
        }
    }

    public void detachAllInvalidationListeners(ObservableValue<?> property) {
        invalidationListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing InvalidationListener " + listener.toString() + " to property " + property.toString());
        });
        invalidationListeners.remove(property);
    }

    @SuppressWarnings("unchecked")
    public void detachAllChangeListeners(ObservableValue<?> property) {
        changeListeners.get(property).forEach(listener -> {
            property.removeListener(listener);
            logger.trace(() -> "Removing ChangeListener " + listener.toString() + " to property " + property.toString());
        });
        changeListeners.remove(property);
    }

    @SuppressWarnings("unchecked")
    public void detachAllListChangeListeners(ObservableList<?> list) {
        listChangeListeners.get(list).forEach(listener -> {
            list.removeListener(listener);
            logger.trace(() -> "Removing ListChangeListener " + listener.toString() + " to property " + list.toString());
        });
        listChangeListeners.remove(list);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void close() {
        listChangeListeners.forEach((list, set) -> set.forEach((listener -> {
            list.removeListener(listener);
            logger.trace(() -> "Removing ListChangeListener " + listener.toString() + " to property " + list.toString());
        })));
        listChangeListeners.clear();
        invalidationListeners.forEach((k, v) -> v.forEach(k::removeListener));
        invalidationListeners.clear();
        changeListeners.forEach((k, v) -> v.forEach(k::removeListener));
        changeListeners.clear();
    }
}
