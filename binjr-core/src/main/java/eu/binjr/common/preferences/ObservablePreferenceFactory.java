
/*
 *    Copyright 2019-2020 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import javafx.beans.property.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A factory to create and manage preferences exposed as {@link Property} and backed by {@link Preferences}
 *
 * @author Frederic Thevenet
 */
public class ObservablePreferenceFactory extends ReloadableItemStore<ObservablePreference<?>> {
    private static final Logger logger = Logger.create(ObservablePreferenceFactory.class);

    public ObservablePreferenceFactory(String backingStoreKey) {
        super(backingStoreKey);
    }

    public ObservablePreference<Boolean> booleanPreference(String key, Boolean defaultValue) {
        var p = new ObservablePreference<>(Boolean.class, key, defaultValue, backingStore) {
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

    public ObservablePreference<String> stringPreference(String key, String defaultValue) {
        var p = new ObservablePreference<String>(String.class, key, defaultValue, backingStore) {
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

    public ObservablePreference<Number> integerPreference(String key, Integer defaultValue) {
        var p = new ObservablePreference<>(Number.class, key, defaultValue, backingStore) {
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

    public ObservablePreference<ZonedDateTime> zoneDateTimePreference(String key, ZonedDateTime defaultValue) {
        var p = new ObservablePreference<>(ZonedDateTime.class, key, defaultValue, backingStore) {
            @Override
            protected Property<ZonedDateTime> makeProperty(ZonedDateTime value) {
                return new SimpleObjectProperty<>(value);
            }

            @Override
            protected ZonedDateTime loadFromBackend() {
                return ZonedDateTime.parse(getBackingStore().get(getKey(),
                        getDefaultValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            @Override
            protected void saveToBackend(ZonedDateTime value) {
                getBackingStore().put(getKey(), value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public ObservablePreference<LocalDateTime> localDateTimePreference(String key, LocalDateTime defaultValue) {
        var p = new ObservablePreference<>(LocalDateTime.class, key, defaultValue, backingStore) {
            @Override
            protected Property<LocalDateTime> makeProperty(LocalDateTime value) {
                return new SimpleObjectProperty<>(value);
            }

            @Override
            protected LocalDateTime loadFromBackend() {
                return LocalDateTime.parse(getBackingStore().get(getKey(),
                        getDefaultValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            @Override
            protected void saveToBackend(LocalDateTime value) {
                getBackingStore().put(getKey(), value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    public ObservablePreference<Number> longPreference(String key, Long defaultValue) {
        var p = new ObservablePreference<>(Number.class, key, defaultValue, backingStore) {
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

    public ObservablePreference<Number> doublePreference(String key, Double defaultValue) {
        var p = new ObservablePreference<>(Number.class, key, defaultValue, backingStore) {
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

    public ObservablePreference<Path> pathPreference(String key, Path defaultValue) {
        var p = new ObservablePreference<>(Path.class, key, defaultValue, backingStore) {
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

    public <E extends Enum<E>> ObservablePreference<E> enumPreference(Class<E> type, String key, E defaultValue) {
        var p = new ObservablePreference<E>(type, key, defaultValue, backingStore) {
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

    public <T> ObservablePreference<T> objectPreference(Class<T> type,
                                                        String key,
                                                        T defaultValue,
                                                        Function<T, String> convertToString,
                                                        Function<String, T> parseFromString) {
        var p = new ObservablePreference<T>(type, key, defaultValue, backingStore) {
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

    public <U> Optional<ObservablePreference<U>> getByName(String name, Class<U> type) {
        var p = storedItems.get(name);
        if (p == null || !type.isAssignableFrom(p.getInnerType())) {
            return Optional.empty();
        }
        return Optional.of((ObservablePreference<U>) p);
    }

    @Override
    public String toString() {
        return storedItems.values().stream().map(p -> p.getKey() + "=" + p.get()).collect(Collectors.joining("\n"));
    }

}
