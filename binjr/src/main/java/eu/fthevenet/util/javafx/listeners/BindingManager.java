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
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class BindingManager implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(BindingManager.class);
    private final Map<ObservableValue, List<ChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<ObservableValue, List<InvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, List<ListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();
    private final Set<Property<?>> boundProperties = Collections.newSetFromMap(new WeakHashMap<>());

    public<T> void bind(Property<T> property, ObservableValue<T> binding){
        Objects.requireNonNull(property, "property parameter cannot be null");
        Objects.requireNonNull(binding, "binding parameter cannot be null");
        logger.trace(() -> "Binding " + binding.toString() + " to " + property.toString());
        property.bind(binding);
        boundProperties.add(property);
    }

    public<T> void unbindAll(){
      boundProperties.forEach(property -> {
          logger.trace(() -> "Unbinding property " + property.toString());
          property.unbind();
      });
    }

    public void attachListener(ObservableValue<?> observable, ChangeListener<?> listener) {
        attachListener(observable, listener, changeListeners, ObservableValue::addListener);
    }

    public void attachListener(ObservableValue<?> observable, InvalidationListener listener) {
        attachListener(observable, listener, invalidationListeners, ObservableValue::addListener);
    }

    public void attachListener(ObservableList<?> observable, ListChangeListener listener) {
        attachListener(observable, listener, listChangeListeners, ObservableList::addListener);
    }

    public void detachListener(ObservableValue<?> observable, ChangeListener listener) {
        detachListener(observable, listener, changeListeners,ObservableValue::removeListener);
    }

    public void detachListener(ObservableValue<?> observable, InvalidationListener listener) {
        detachListener(observable, listener, invalidationListeners, ObservableValue::removeListener);
    }

    public void detachListener(ObservableList<?> observable, ListChangeListener<?> listener) {
        detachListener(observable, listener, listChangeListeners, ObservableList::removeListener);
    }

    public void detachAllInvalidationListeners(ObservableValue<?> observable) {
        detachAllListener(observable, invalidationListeners, ObservableValue::removeListener);
    }

    public void detachAllChangeListeners(ObservableValue<?> observable) {
        detachAllListener(observable, changeListeners, ObservableValue::removeListener);
    }

    public void detachAllListChangeListeners(ObservableList<?> observable) {
        detachAllListener(observable, listChangeListeners, ObservableList::removeListener);
    }

    @Override
    public void close() {
        closeMap(listChangeListeners, ObservableList::removeListener);
        closeMap(invalidationListeners, ObservableValue::removeListener);
        closeMap(changeListeners, ObservableValue::removeListener);
        unbindAll();
    }

    private <T, U> void attachListener(T observable, U listener, Map<T, List<U>> map, BiConsumer<T, U> attachAction) {
        Objects.requireNonNull(observable, "observable parameter cannot be null");
        Objects.requireNonNull(listener, "listener parameter cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(attachAction, "attachAction parameter cannot be null");
        map.computeIfAbsent(observable, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching listener " + listener.toString() + " to observable " + observable.toString());
        attachAction.accept(observable,listener);
    }

    private <T, U> void detachListener(T observable, U listener, Map<T, List<U>> map, BiConsumer<T, U> detachAction) {
        Objects.requireNonNull(observable, "observable parameter cannot be null");
        Objects.requireNonNull(listener, "listener parameter cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(detachAction, "ifPresent parameter cannot be null");
        List<U> listeners = map.get(observable);
        if (listeners == null) {
            throw new IllegalArgumentException("Observable " + observable.toString() + " is not managed by this BindingManager instance");
        }
        listeners.stream().filter(l -> l.equals(listener)).findFirst().ifPresent(found -> map.get(observable).remove(found));
        logger.trace(() -> "Removing Listener " + listener.toString() + " from observable " + observable.toString());
        detachAction.accept(observable, listener);
    }

    private <T, U> void detachAllListener(T observable,  Map<T, List<U>> map, BiConsumer<T, U> detachAction) {
        Objects.requireNonNull(observable, "observable paramater cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(detachAction, "attachAction parameter cannot be null");
        List<U> l = map.get(observable);
        if (l == null) {
            throw new IllegalArgumentException("ObservableList " + observable.toString() + " is not managed by this BindingManager instance");
        }
        l.forEach(listener -> {
            logger.trace(() -> "Removing Listener " + listener.toString() + " from observable " + observable.toString());
            detachAction.accept(observable, listener);
        });
        map.remove(observable);
    }

    private <T, U> void closeMap(Map<T, List<U>> map, BiConsumer<T, U> detachAction) {
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(detachAction, "attachAction parameter cannot be null");
        map.forEach((observable, listeners) -> listeners.forEach(listener -> {
            logger.trace(() -> "Removing Listener " + listener.toString() + " from observable " + observable.toString());
            detachAction.accept(observable, listener);
        }));
        map.clear();
    }
}
