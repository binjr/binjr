/*
 *    Copyright 2017-2018 Frederic Thevenet
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

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SerializedDataAdapter;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.common.javafx.bindings.BindingManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Closeable;
import java.util.Map;
import java.util.UUID;

/**
 * A class that represents and holds the current state of a single data source.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Source")
public class Source implements Dirtyable, Closeable {
    private UUID adapterId;
    @IsDirtyable
    private Property<String> name = new SimpleStringProperty();
    private String adapterClassName;
    private Map<String, String> adapterParams;
    private final ChangeWatcher status;
    private DataAdapter adapter;
    private final BindingManager bindingManager = new BindingManager();
    private Property<Boolean> editable = new SimpleBooleanProperty();

    /**
     * Creates an instance of the {@link Source} class from the provided  {@link SerializedDataAdapter}
     *
     * @param adapter the {@link SerializedDataAdapter} to create the {@link Source}  instance from
     * @return an instance of the {@link Source} class from the provided  {@link SerializedDataAdapter}
     */
    @SuppressWarnings("unchecked")
    public static Source of(DataAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter cannot be null");
        }
        Source newSource = new Source();
        newSource.setAdapter(adapter);
        newSource.setName(adapter.getSourceName());
        newSource.setAdapterClassName(adapter.getClass().getName());
        newSource.setAdapterParams(adapter.getParams());
        newSource.setAdapterId(adapter.getId());
        return newSource;
    }

    /**
     * Initializes a new instance of the {@link Source} class
     */
    public Source() {
        this.status = new ChangeWatcher(this);
        if (adapterId == null) {
            this.adapterId = UUID.randomUUID();
        }
    }

    /**
     * The name of the {@link Source}
     *
     * @return the name of the {@link Source}
     */
    @XmlAttribute(name = "name")
    public String getName() {
        return name.getValue();
    }

    /**
     * The name of the {@link Source}
     *
     * @param name the name of the {@link Source}
     */
    public void setName(String name) {
        this.name.setValue(name);
    }

    public Property<String> nameProperty() {
        return this.name;
    }

    /**
     * The name of the class that implements the {@link SerializedDataAdapter} for the {@link Source}
     *
     * @return the name of the class that implements the {@link SerializedDataAdapter} for the {@link Source}
     */
    @XmlAttribute(name = "adapter")
    public String getAdapterClassName() {
        return adapterClassName;
    }

    /**
     * Returns a map of the parameters required to establish a connection to the data source
     *
     * @return a map of the parameters required to establish a connection to the data source
     */
    @XmlJavaTypeAdapter(ParameterMapAdapter.class)
    @XmlElement(name = "AdapterParameters")
    public Map<String, String> getAdapterParams() {
        return adapterParams;
    }

    /**
     * Gets the id of the {@link SerializedDataAdapter} attached to the source
     *
     * @return the id of the {@link SerializedDataAdapter} attached to the source
     */
    @XmlAttribute(name = "id")
    public UUID getAdapterId() {
        return adapterId;
    }

    /**
     * Sets the id of the {@link SerializedDataAdapter} attached to the source
     *
     * @param adapterId the id of the {@link SerializedDataAdapter} attached to the source
     */
    public void setAdapterId(UUID adapterId) {
        this.adapterId = adapterId;
    }

    @Override
    public Boolean isDirty() {
        return this.status.isDirty();
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return this.status.dirtyProperty();
    }

    @Override
    public void cleanUp() {
        this.status.cleanUp();
    }

    /**
     * Returns the {@link BindingManager} instance for the {@link Source}
     *
     * @return the {@link BindingManager} instance for the {@link Source}
     */
    @XmlTransient
    public BindingManager getBindingManager() {
        return bindingManager;
    }

    @Override
    public void close() {
        bindingManager.close();
        adapter.close();
    }

    /**
     * Returns the {@link SerializedDataAdapter} attached to the {@link Source}.
     *
     * @return the {@link SerializedDataAdapter} attached to the {@link Source}.
     */
    @XmlTransient
    public DataAdapter getAdapter() {
        return adapter;
    }

    /**
     * Sets the {@link SerializedDataAdapter} attached to the {@link Source}.
     *
     * @param adapter the {@link SerializedDataAdapter} attached to the {@link Source}.
     */
    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns true is the source is editable, false otherwise.
     *
     * @return true is the source is editable, false otherwise
     */
    @XmlTransient
    public Boolean isEditable() {
        return editable.getValue();
    }

    /**
     * The editable property.
     *
     * @return the editable property.
     */
    public Property<Boolean> editableProperty() {
        return editable;
    }

    /**
     * Set to true if the source is editable, false otherwise.
     *
     * @param editable true if the source is editable, false otherwise.
     */
    public void setEditable(Boolean editable) {
        this.editable.setValue(editable);
    }

    //region Private members.
    private void setAdapterClassName(String adapterClassName) {
        this.adapterClassName = adapterClassName;
    }

    private void setAdapterParams(Map<String, String> adapterParams) {
        this.adapterParams = adapterParams;
    }
    //endregion
}
