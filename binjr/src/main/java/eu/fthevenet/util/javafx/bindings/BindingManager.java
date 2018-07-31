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

package eu.fthevenet.util.javafx.bindings;

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

/**
 * A class that provide methods to centralize and register the attachment of listeners
 * and bindings onto {@link javafx.beans.Observable} instances.
 * <p>
 * This makes it possible to remove all listeners and bindings attached to to
 * registered {@link Observable} instances in a determinist fashion (on invoking {@code close()})
 * and helps alleviate the potential for references leaks in some scenarios.
 * </p>
 *
 * @author Frederic Thevenet
 */
public class BindingManager implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(BindingManager.class);
    private final Map<ObservableValue, List<ChangeListener>> changeListeners = new ConcurrentHashMap<>();
    private final Map<ObservableValue, List<InvalidationListener>> invalidationListeners = new ConcurrentHashMap<>();
    private final Map<ObservableList, List<ListChangeListener>> listChangeListeners = new ConcurrentHashMap<>();
    private final Set<Property<?>> boundProperties = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Binds the specified {@link ObservableValue} onto the specified {@link Property} and registers the resulting binding.
     *
     * @param property the {@link Property} to bind
     * @param binding  the {@link ObservableValue} to bind to the {@link Property}
     * @param <T>      the type common to both {@link Property} and {@link ObservableValue}
     */
    public <T> void bind(Property<T> property, ObservableValue<T> binding) {
        Objects.requireNonNull(property, "property parameter cannot be null");
        Objects.requireNonNull(binding, "binding parameter cannot be null");
        logger.trace(() -> "Binding " + binding.toString() + " to " + property.toString());
        property.bind(binding);
        boundProperties.add(property);
    }

    /**
     * Unbinds all registered bindings
     */
    public void unbindAll() {
        boundProperties.forEach(property -> {
            logger.trace(() -> "Unbinding property " + property.toString());
            property.unbind();
        });
    }

    /**
     * Attach a {@link ChangeListener} to an {@link ObservableValue} and registers the resulting binding.
     *
     * @param observable the {@link ObservableValue} to attach the listener to.
     * @param listener   the {@link ChangeListener} to attach
     */
    public void attachListener(ObservableValue<?> observable, ChangeListener<?> listener) {
        attachListener(observable, listener, changeListeners, ObservableValue::addListener);
    }

    /**
     * Attach a {@link InvalidationListener} to an {@link ObservableValue} and registers the resulting binding.
     *
     * @param observable the {@link ObservableValue} to attach the listener to.
     * @param listener   the {@link InvalidationListener} to attach
     */
    public void attachListener(ObservableValue<?> observable, InvalidationListener listener) {
        attachListener(observable, listener, invalidationListeners, ObservableValue::addListener);
    }

    /**
     * Attach a {@link ListChangeListener} to an {@link ObservableList} and registers the resulting binding.
     *
     * @param observable the {@link ObservableList} to attach the listener to.
     * @param listener   the {@link ListChangeListener} to attach
     */
    public void attachListener(ObservableList<?> observable, ListChangeListener listener) {
        attachListener(observable, listener, listChangeListeners, ObservableList::addListener);
    }

    /**
     * Remove a specific {@link ChangeListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove the listener from.
     * @param listener   the {@link ChangeListener} to remove
     */
    public void detachListener(ObservableValue<?> observable, ChangeListener listener) {
        detachListener(observable, listener, changeListeners, ObservableValue::removeListener);
    }

    /**
     * Remove a specific {@link InvalidationListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove the listener from.
     * @param listener   the {@link InvalidationListener} to remove
     */
    public void detachListener(ObservableValue<?> observable, InvalidationListener listener) {
        detachListener(observable, listener, invalidationListeners, ObservableValue::removeListener);
    }

    /**
     * Remove a specific {@link ListChangeListener} from an {@link ObservableList}.
     *
     * @param observable the {@link ObservableList} to remove the listener from.
     * @param listener   the {@link ListChangeListener} to remove
     */
    public void detachListener(ObservableList<?> observable, ListChangeListener<?> listener) {
        detachListener(observable, listener, listChangeListeners, ObservableList::removeListener);
    }

    /**
     * Remove <u>all</u> {@link InvalidationListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove all listeners from.
     */
    public void detachAllInvalidationListeners(ObservableValue<?> observable) {
        detachAllListener(observable, invalidationListeners, ObservableValue::removeListener);
    }

    /**
     * Remove <u>all</u> {@link ChangeListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove all listeners from.
     */
    public void detachAllChangeListeners(ObservableValue<?> observable) {
        detachAllListener(observable, changeListeners, ObservableValue::removeListener);
    }

    /**
     * Remove <u>all</u> {@link ListChangeListener} from an {@link ObservableList}.
     *
     * @param observable the {@link ObservableList} to remove all listeners from.
     */
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
        attachAction.accept(observable, listener);
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

    private <T, U> void detachAllListener(T observable, Map<T, List<U>> map, BiConsumer<T, U> detachAction) {
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
