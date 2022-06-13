/*
 *    Copyright 2020-2022 Frederic Thevenet
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

package eu.binjr.core.data.workspace;

import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that represents and holds the current state of a single worksheet
 *
 * @author Frederic Thevenet
 */
public abstract class Worksheet<T> implements Dirtyable {
    protected static final AtomicInteger globalCounter = new AtomicInteger(0);

    @IsDirtyable
    private final Property<String> name;

    private final transient BooleanProperty editModeEnabled;

    protected Worksheet(String name, boolean editModeEnabled) {
        this.name = new SimpleStringProperty(name);
        this.editModeEnabled = new SimpleBooleanProperty(editModeEnabled);
    }

    /**
     * The name of the {@link Worksheet}
     *
     * @return the name of the {@link Worksheet}
     */
    @XmlAttribute()
    public String getName() {
        return name.getValue();
    }

    /**
     * The name of the {@link Worksheet}
     *
     * @return An instance of {@link Property} for the name of the {@link Worksheet}
     */
    public Property<String> nameProperty() {
        return name;
    }

    /**
     * The name of the {@link Worksheet}
     *
     * @param name the name of the {@link Worksheet}
     */
    public void setName(String name) {
        this.name.setValue(name);
    }

    public abstract Class<? extends WorksheetController> getControllerClass();

    public abstract Worksheet<T> duplicate();

    @XmlTransient
    public Boolean isEditModeEnabled() {
        return editModeEnabled.getValue();
    }

    public BooleanProperty editModeEnabledProperty() {
        return editModeEnabled;
    }

    public void setEditModeEnabled(Boolean editModeEnabled) {
        this.editModeEnabled.setValue(editModeEnabled);
    }

    @Override
    public abstract void close();

    public abstract void initWithBindings(String title, BindingsHierarchy... rootItems) throws DataAdapterException;

    protected abstract List<? extends TimeSeriesInfo<T>> listAllSeriesInfo();

    public void attachAdaptersToSeriesInfo(Collection<DataAdapter<T>> adapters) throws NoAdapterFoundException {
        for (TimeSeriesInfo<T> s : listAllSeriesInfo()) {
            UUID id = s.getBinding().getAdapterId();
            DataAdapter<T> da = adapters
                    .stream()
                    .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                    .findAny()
                    .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " + (id != null ? id.toString() : "null")));
            s.getBinding().setAdapter(da);
        }
    }

}
