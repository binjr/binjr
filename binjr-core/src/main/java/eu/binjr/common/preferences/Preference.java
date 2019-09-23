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

import javafx.beans.property.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.prefs.Preferences;

/**
 * An abstract class that defines a preference that can be accessed via a {@link Property} and is persisted as
 * use {@link Preferences}
 *
 * @param <T> The type of the inner value of the preference
 * @author Frederic Thevenet
 */
public abstract class Preference<T> implements ReloadableStore.Reloadable {
    private static final Logger logger = LogManager.getLogger(Preference.class);
    private final String key;
    private final T defaultValue;
    private final Preferences backingStore;
    private final Property<T> backingProperty;
    private final Class<T> innerType;

    public Preference(Class<T> innerType, String key, T defaultValue, Preferences backingStore) {
        this.backingStore = backingStore;
        this.key = key;
        this.defaultValue = defaultValue;
        this.innerType = innerType;

        backingProperty = makeProperty(failSafeLoad());
        backingProperty.addListener((observable, oldValue, newValue) -> failSafeSave(newValue));

    }

    @Override
    public void reload() {
        backingProperty.setValue(failSafeLoad());
    }

    public T get() {
        return backingProperty.getValue();
    }

    public Property<T> property() {
        return backingProperty;
    }

    public void set(T property) {
        this.backingProperty.setValue(property);
    }

    protected abstract Property<T> makeProperty(T value);

    protected abstract T loadFromBackend();

    protected abstract void saveToBackend(T value);

    protected Preferences getBackingStore() {
        return backingStore;
    }

    protected String getKey() {
        return key;
    }

    protected T getDefaultValue() {
        return defaultValue;
    }

    protected Class<T> getInnerType() {
        return innerType;
    }

    private void failSafeSave(T value) {
        try {
            saveToBackend(value);
        } catch (Throwable t) {
            logger.error("Failed to save preference " + key + " to backend: " + t.getMessage(), t);
        }
    }

    private T failSafeLoad() {
        try {
            return loadFromBackend();
        } catch (Throwable t) {
            logger.error("Failed to load preference " + key + " from backend: " + t.getMessage(), t);
            return defaultValue;
        }
    }

}
