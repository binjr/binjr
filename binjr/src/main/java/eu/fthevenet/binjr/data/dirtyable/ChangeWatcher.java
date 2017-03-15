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
import java.util.List;

/**
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ChangeWatcher<T> {
    private static final Logger logger = LogManager.getLogger(ChangeWatcher.class);
    private final T source;
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final List<Property<?>> watchedProperties;
    private final List<ObservableList<? extends Dirtyable>> watchedLists;
    private final ChangeListener<Object> propertyChangeListener = (observable, oldValue, newValue) -> forceDirty();

    private final ChangeListener<Boolean> dirtyableChangeListener = (observable, oldValue, newValue) -> {
        if (newValue) {
            forceDirty();
        }
    };

    private ListChangeListener<Dirtyable> listChangeListener = (c -> {
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

    public ChangeWatcher(T source) {
        this.source = source;
        this.watchedProperties = new ArrayList<>();
        this.watchedLists = new ArrayList<>();

        List<Field> toWatch = getFieldsListWithAnnotation(source.getClass(), IsDirtyable.class);
        for (Field field : toWatch) {
            try {
                Object fieldValue = readField(field, source);
                if (fieldValue instanceof Property) {
                    watchedProperties.add((Property<?>) fieldValue);
                    ((Property<?>) fieldValue).addListener(propertyChangeListener);
                    logger.debug(() -> "Watched property: " + field.getName());
                }
                if (fieldValue instanceof ObservableList) {
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    Type[] types = pType.getActualTypeArguments();
                    if (types != null) {
                        for (Type type : types) {
                            if (Dirtyable.class.isAssignableFrom((Class<?>) type)) {
                                @SuppressWarnings("unchecked")
                                ObservableList<? extends Dirtyable> ol = (ObservableList<? extends Dirtyable>) fieldValue;
                                watchedLists.add(ol);
                                ol.addListener(listChangeListener);
                                logger.debug(() -> "Watched list: " + field.getName());
                                break;
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error("Error reflecting dirtyable properties", e);
            }
        }
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.getValue();
    }

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

    public T getSource() {
        return source;
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
        final List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            for (final Field field : declaredFields) {
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }
}
