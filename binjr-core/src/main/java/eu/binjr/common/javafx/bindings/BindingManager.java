/*
 *    Copyright 2017-2019 Frederic Thevenet
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

package eu.binjr.common.javafx.bindings;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A class that provide methods to centralize and register the attachment of listeners
 * and bindings onto {@link javafx.beans.Observable} instances.
 * <p>
 * This makes it possible to remove all listeners and bindings attached to to
 * registered {@link javafx.beans.Observable} instances in a determinist fashion (on invoking {@code close()})
 * and helps alleviate the potential for references leaks in some scenarios.
 * </p>
 *
 * @author Frederic Thevenet
 */
public class BindingManager implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(BindingManager.class);
    private final Map<ObservableValue, List<ChangeListener>> changeListeners = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ObservableValue, List<InvalidationListener>> invalidationListeners = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ObservableList, List<ListChangeListener>> listChangeListeners = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Property<?>, ObservableValue> boundProperties = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Property<?>, Property> bidirectionallyBoundProperties = Collections.synchronizedMap(new WeakHashMap<>());
    private final List<EventHandler<?>> registeredHandlers = Collections.synchronizedList(new ArrayList<>());

    /**
     * Binds the specified {@link ObservableValue} onto the specified {@link Property} and registers the resulting binding.
     *
     * @param property the {@link Property} to bind
     * @param binding  the {@link ObservableValue} to bind to the {@link Property}
     * @param <T>      the type common to both {@link Property} and {@link ObservableValue}
     */
    public <T, U extends T> void bind(Property<T> property, ObservableValue<U> binding) {
        Objects.requireNonNull(property, "property parameter cannot be null");
        Objects.requireNonNull(binding, "binding parameter cannot be null");
        logger.trace(() -> "Binding " + binding.toString() + " to " + property.toString());
        property.bind(binding);
        boundProperties.put(property, binding);
    }

    /**
     * Unbinds all registered bindings
     */
    public void unbindAll() {
        boundProperties.forEach((property, binding) -> {
            logger.trace(() -> "Unbinding property " + property.toString());
            property.unbind();
        });
        boundProperties.clear();

        bidirectionallyBoundProperties.forEach((property, binding) -> {
            logger.trace(() -> "Unbinding property " + property.toString() + " from " + binding.toString());
            property.unbindBidirectional(binding);
        });
        bidirectionallyBoundProperties.clear();
    }


    public <T> void bindBidirectional(Property<T> property, Property<T> binding) {
        Objects.requireNonNull(property, "property parameter cannot be null");
        Objects.requireNonNull(binding, "binding parameter cannot be null");
        logger.trace(() -> "Binding " + binding.toString() + " to " + property.toString());
        property.bindBidirectional(binding);
        bidirectionallyBoundProperties.put(property, binding);
    }

    public <T> void unbindBidirectionnal(Property<T> property, Property<T> binding) {
        Objects.requireNonNull(property, "property parameter cannot be null");
        Objects.requireNonNull(binding, "binding parameter cannot be null");
        logger.trace(() -> "Unbinding " + binding.toString() + " from " + property.toString());
        property.unbindBidirectional(binding);
        bidirectionallyBoundProperties.remove(property, binding);
    }

    /**
     * Attach a {@link ChangeListener} to an {@link ObservableValue} and registers the resulting binding.
     *
     * @param observable the {@link ObservableValue} to attach the listener to.
     * @param listener   the {@link ChangeListener} to attach
     */
    public void attachListener(ObservableValue<?> observable, ChangeListener<?> listener) {
        register(observable, listener, changeListeners, ObservableValue::addListener);
    }

    /**
     * Attach a {@link InvalidationListener} to an {@link ObservableValue} and registers the resulting binding.
     *
     * @param observable the {@link ObservableValue} to attach the listener to.
     * @param listener   the {@link InvalidationListener} to attach
     */
    public void attachListener(ObservableValue<?> observable, InvalidationListener listener) {
        register(observable, listener, invalidationListeners, ObservableValue::addListener);
    }

    /**
     * Attach a {@link ListChangeListener} to an {@link ObservableList} and registers the resulting binding.
     *
     * @param observable the {@link ObservableList} to attach the listener to.
     * @param listener   the {@link ListChangeListener} to attach
     */
    public void attachListener(ObservableList<?> observable, ListChangeListener listener) {
        register(observable, listener, listChangeListeners, ObservableList::addListener);
    }

    /**
     * Remove a specific {@link ChangeListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove the listener from.
     * @param listener   the {@link ChangeListener} to remove
     */
    public void detachListener(ObservableValue<?> observable, ChangeListener listener) {
        unregister(observable, listener, changeListeners, ObservableValue::removeListener);
    }

    /**
     * Remove a specific {@link InvalidationListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove the listener from.
     * @param listener   the {@link InvalidationListener} to remove
     */
    public void detachListener(ObservableValue<?> observable, InvalidationListener listener) {
        unregister(observable, listener, invalidationListeners, ObservableValue::removeListener);
    }

    /**
     * Remove a specific {@link ListChangeListener} from an {@link ObservableList}.
     *
     * @param observable the {@link ObservableList} to remove the listener from.
     * @param listener   the {@link ListChangeListener} to remove
     */
    public void detachListener(ObservableList<?> observable, ListChangeListener<?> listener) {
        unregister(observable, listener, listChangeListeners, ObservableList::removeListener);
    }

    /**
     * Remove <u>all</u> {@link InvalidationListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove all listeners from.
     */
    public void detachAllInvalidationListeners(ObservableValue<?> observable) {
        unregister(observable, invalidationListeners, ObservableValue::removeListener);
    }

    /**
     * Remove <u>all</u> {@link ChangeListener} from an {@link ObservableValue}.
     *
     * @param observable the {@link ObservableValue} to remove all listeners from.
     */
    public void detachAllChangeListeners(ObservableValue<?> observable) {
        unregister(observable, changeListeners, ObservableValue::removeListener);
    }

    /**
     * Remove <u>all</u> {@link ListChangeListener} from an {@link ObservableList}.
     *
     * @param observable the {@link ObservableList} to remove all listeners from.
     */
    public void detachAllListChangeListeners(ObservableList<?> observable) {
        unregister(observable, listChangeListeners, ObservableList::removeListener);
    }

    @Override
    public synchronized void close() {
        unregisterAll(listChangeListeners, ObservableList::removeListener);
        unregisterAll(invalidationListeners, ObservableValue::removeListener);
        unregisterAll(changeListeners, ObservableValue::removeListener);
        unbindAll();
        // Release strong refs to registered event handlers, so that their
        // weak counterpart may be collected.
        registeredHandlers.clear();
    }

    public synchronized void suspend() {
        visitMap(listChangeListeners, ObservableList::removeListener);
        visitMap(invalidationListeners, ObservableValue::removeListener);
        visitMap(changeListeners, ObservableValue::removeListener);
            boundProperties.keySet().forEach(Property::unbind);
        }

    public synchronized void resume() {
        visitMap(listChangeListeners, ObservableList::addListener);
        visitMap(invalidationListeners, ObservableValue::addListener);
        visitMap(changeListeners, ObservableValue::addListener);
            boundProperties.forEach(Property::bind);
        }

    public <T extends Event> WeakEventHandler<T> registerHandler(EventHandler<T> handler) {
        // Store strong ref to handler, so it doesn't get collected prematurely.
        registeredHandlers.add(handler);
        // wrap in WeakEventHandler
        return new WeakEventHandler<T>(handler);
    }

    private <T, U> void register(T observable, U listener, Map<T, List<U>> map, BiConsumer<T, U> attachAction) {
        Objects.requireNonNull(observable, "observable parameter cannot be null");
        Objects.requireNonNull(listener, "listener parameter cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(attachAction, "attachAction parameter cannot be null");
        map.computeIfAbsent(observable, p -> new ArrayList<>()).add(listener);
        logger.trace(() -> "Attaching listener " + listener.toString() + " to observable " + observable.toString());
        attachAction.accept(observable, listener);
    }

    private <T, U> void unregister(T key, U value, Map<T, List<U>> map, BiConsumer<T, U> unregisterAction) {
        Objects.requireNonNull(key, "key parameter cannot be null");
        Objects.requireNonNull(value, "value parameter cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(unregisterAction, "unregisterAction parameter cannot be null");
        List<U> listeners = map.get(key);
        if (listeners == null) {
            logger.debug(() -> "Object " + key.toString() + " is not managed by this BindingManager instance");
            return;
        }
        listeners.stream().filter(l -> l.equals(value)).findFirst().ifPresent(found -> map.get(key).remove(found));
        logger.trace(() -> "Unregistering " + value.toString() + " from " + key.toString());
        unregisterAction.accept(key, value);
    }

    private <T, U> void unregister(T key, Map<T, List<U>> map, BiConsumer<T, U> unregisterAction) {
        Objects.requireNonNull(key, "key paramater cannot be null");
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(unregisterAction, "unregisterAction parameter cannot be null");
        List<U> l = map.get(key);
        if (l == null) {
            logger.debug(() -> "Object " + key.toString() + " is not managed by this BindingManager instance");
            return;
        }
        l.forEach(value -> {
            logger.trace(() -> "Unregistering " + value.toString() + " from " + key.toString());
            unregisterAction.accept(key, value);
        });
        map.remove(key);
    }

    private <T, U> void unregisterAll(Map<T, List<U>> map, BiConsumer<T, U> unregisterAction) {
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(unregisterAction, "unregisterAction parameter cannot be null");
            map.forEach((k, vList) -> {
                vList.forEach(v -> {
                    logger.trace(() -> "Unregistering " + v.toString() + " from " + k.toString());
                    unregisterAction.accept(k, v);
                });
            });
        map.clear();
    }

    private <T, U> void visitMap(Map<T, List<U>> map, BiConsumer<T, U> action) {
        Objects.requireNonNull(map, "map parameter cannot be null");
        Objects.requireNonNull(action, "action parameter cannot be null");
            map.forEach((observable, listeners) -> listeners.forEach(listener -> {
                logger.trace(() -> "visiting key " + listener.toString() + " value " + observable.toString());
                action.accept(observable, listener);
            }));
    }
}
