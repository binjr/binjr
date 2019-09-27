
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

import javafx.beans.property.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A factory to create and manage preferences exposed as {@link Property} and backed by {@link Preferences}
 *
 * @author Frederic Thevenet
 */
public class PreferenceFactory extends ReloadableItemStore<Preference<?>> {
    private static final Logger logger = LogManager.getLogger(PreferenceFactory.class);

    public PreferenceFactory(Preferences backingStore) {
        super(backingStore);
    }

    public Preference<Boolean> booleanPreference(String key, Boolean defaultValue) {
        var p = new Preference<>(Boolean.class, key, defaultValue, backingStore) {
            @Override
            protected Property<Boolean> makeProperty(Boolean value) {
                return new SimpleBooleanProperty(value);
            }

            @Override
            protected Boolean loadFromBackend() {
                return getBackingStore().getBoolean(getKey(), getDefaultValue());
            }

            @Override
            protected void saveToBackend(Boolean value) {
                getBackingStore().putBoolean(getKey(), value);
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public Preference<String> stringPreference(String key, String defaultValue) {
        var p = new Preference<String>(String.class, key, defaultValue, backingStore) {
            @Override
            protected Property<String> makeProperty(String value) {
                return new SimpleStringProperty(value);
            }

            @Override
            protected String loadFromBackend() {
                return getBackingStore().get(getKey(), getDefaultValue());
            }

            @Override
            protected void saveToBackend(String value) {
                getBackingStore().put(getKey(), value);
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public Preference<Number> integerPreference(String key, Integer defaultValue) {
        var p = new Preference<>(Number.class, key, defaultValue, backingStore) {
            @Override
            protected Property<Number> makeProperty(Number value) {
                return new SimpleIntegerProperty(value.intValue());
            }

            @Override
            protected Integer loadFromBackend() {
                return getBackingStore().getInt(getKey(), getDefaultValue().intValue());
            }

            @Override
            protected void saveToBackend(Number value) {
                getBackingStore().putInt(getKey(), value.intValue());
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public Preference<Number> longPreference(String key, Long defaultValue) {
        var p = new Preference<>(Number.class, key, defaultValue, backingStore) {
            @Override
            protected Property<Number> makeProperty(Number value) {
                return new SimpleLongProperty(value.longValue());
            }

            @Override
            protected Long loadFromBackend() {
                return getBackingStore().getLong(getKey(), getDefaultValue().longValue());
            }

            @Override
            protected void saveToBackend(Number value) {
                getBackingStore().putLong(getKey(), value.longValue());
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public Preference<Number> doublePreference(String key, Double defaultValue) {
        var p = new Preference<>(Number.class, key, defaultValue, backingStore) {
            @Override
            protected Property<Number> makeProperty(Number value) {
                return new SimpleDoubleProperty(value.doubleValue());
            }

            @Override
            protected Double loadFromBackend() {
                return getBackingStore().getDouble(getKey(), getDefaultValue().doubleValue());
            }

            @Override
            protected void saveToBackend(Number value) {
                getBackingStore().putDouble(getKey(), value.doubleValue());
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public Preference<Path> pathPreference(String key, Path defaultValue) {
        var p = new Preference<>(Path.class, key, defaultValue, backingStore) {
            @Override
            protected Property<Path> makeProperty(Path value) {
                return new SimpleObjectProperty<>(value);
            }

            @Override
            protected Path loadFromBackend() {
                return Path.of(getBackingStore().get(getKey(), getDefaultValue().toString()));
            }

            @Override
            protected void saveToBackend(Path value) {
                getBackingStore().put(getKey(), value.toString());
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public <E extends Enum<E>> Preference<E> enumPreference(Class<E> type, String key, E defaultValue) {
        var p = new Preference<E>(type, key, defaultValue, backingStore) {
            @Override
            protected Property<E> makeProperty(E value) {
                return new SimpleObjectProperty<E>(value);
            }

            @Override
            protected E loadFromBackend() {
                return Enum.valueOf(getInnerType(), getBackingStore().get(getKey(), getDefaultValue().name()));
            }

            @Override
            protected void saveToBackend(E value) {
                getBackingStore().put(getKey(), value.name());
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public <T> Preference<T> objectPreference(Class<T> type,
                                              String key,
                                              T defaultValue,
                                              Function<T, String> convertToString,
                                              Function<String, T> parseFromString) {
        var p = new Preference<T>(type, key, defaultValue, backingStore) {
            @Override
            protected Property<T> makeProperty(T value) {
                return new SimpleObjectProperty<T>(value);
            }

            @Override
            protected T loadFromBackend() {
                return parseFromString.apply(getBackingStore().get(getKey(), convertToString.apply(getDefaultValue())));
            }

            @Override
            protected void saveToBackend(T value) {
                getBackingStore().put(getKey(), convertToString.apply(value));
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public <U> Optional<Preference<U>> getByName(String name, Class<U> type) {
        var p = storedItems.get(name);
        if (p == null || !type.isAssignableFrom(p.getInnerType())) {
            return Optional.empty();
        }
        return Optional.of((Preference<U>) p);
    }

    @Override
    public String toString() {
        return storedItems.values().stream().map(p -> p.getKey() + "=" + p.get()).collect(Collectors.joining("\n"));
    }

}
