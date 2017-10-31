/*
 *    Copyright 2017 Frederic Thevenet
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

package eu.fthevenet.binjr.data.dirtyable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class that embeds the logic required to discover and track modification on object implementing {@link Dirtyable}
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ChangeWatcher implements Dirtyable {
    private static final Logger logger = LogManager.getLogger(ChangeWatcher.class);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final List<ObservableList<? extends Dirtyable>> watchedLists;

    private final ChangeListener<Boolean> dirtyableChangeListener = (observable, oldValue, newValue) -> {
        if (newValue) {
            forceDirty();
        }
    };

    /**
     * Initializes a new instance of the {@link ChangeWatcher} class for the specified source object
     *
     * @param source the object to watch for changes
     */
    public ChangeWatcher(Object source) {
        this.watchedLists = new ArrayList<>();
        List<Field> toWatch = getFieldsListWithAnnotation(source.getClass(), IsDirtyable.class);
        for (Field field : toWatch) {
            try {
                Object fieldValue = readField(field, source);
                if (fieldValue instanceof Property) {
                    ((Property<?>) fieldValue).addListener((observable, oldValue, newValue) -> forceDirty());
                }
                if (fieldValue instanceof ObservableList) {
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    Type[] types = pType.getActualTypeArguments();
                    if (types != null) {
                        for (Type type : types) {
                            if (type instanceof ParameterizedType) {
                                type = ((ParameterizedType) type).getRawType();
                            }
                            if (Dirtyable.class.isAssignableFrom((Class<?>) type)) {
                                @SuppressWarnings("unchecked")
                                ObservableList<? extends Dirtyable> ol = (ObservableList<? extends Dirtyable>) fieldValue;
                                watchedLists.add(ol);
                                ListChangeListener<Dirtyable> listChangeListener = (c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            if (c.getAddedSize() > 0) {
                                                forceDirty();
                                            }
                                            for (Dirtyable dirtyable : c.getAddedSubList()) {
                                                evaluateDirty(dirtyable.isDirty());
                                                dirtyable.dirtyProperty().addListener(dirtyableChangeListener);
                                            }
                                        }
                                        if (c.wasRemoved()) {
                                            if (c.getRemovedSize() > 0) {
                                                forceDirty();
                                            }
                                            for (Dirtyable dirtyable : c.getRemoved()) {
                                                dirtyable.dirtyProperty().removeListener(dirtyableChangeListener);
                                            }
                                        }
                                    }
                                });
                                ol.addListener(listChangeListener);
                                break;
                            }
                        }
                    }
                }
            } catch (IllegalAccessException | ClassCastException e) {
                logger.error("Error reflecting dirtyable properties", e);
            }
        }
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    @Override
    public Boolean isDirty() {
        return dirty.getValue();
    }

    @Override
    public void cleanUp() {
        dirty.setValue(false);
        watchedLists.forEach(l -> l.forEach(Dirtyable::cleanUp));
    }

    private void evaluateDirty(Boolean isDirty) {
        this.dirty.setValue(dirty.getValue() | isDirty);
    }

    private void forceDirty() {
        dirty.setValue(true);
    }

    private Object readField(final Field field, final Object target) throws IllegalAccessException {
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(target);
    }

    private List<Field> getFieldsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        if (annotationCls == null) {
            throw new IllegalArgumentException("The annotation class must not be null");
        }
        final List<Field> allFields = getAllFieldsList(cls);
        final List<Field> annotatedFields = new ArrayList<>();
        for (final Field field : allFields) {
            if (field.getAnnotation(annotationCls) != null) {
                annotatedFields.add(field);
            }
        }
        return annotatedFields;
    }

    private List<Field> getAllFieldsList(final Class<?> cls) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null");
        }
        final List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            allFields.addAll(Arrays.asList(declaredFields));
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }
}
