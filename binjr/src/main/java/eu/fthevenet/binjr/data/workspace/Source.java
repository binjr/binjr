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

package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import javafx.beans.property.BooleanProperty;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;
import java.util.UUID;

/**
 * A class that represents and holds the current state of a single data source
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Source")
public class Source implements Dirtyable {
    @XmlAttribute(name = "id")
    private UUID adapterId;
    @XmlAttribute(name = "name")
    @IsDirtyable
    private String name;
    @XmlAttribute(name = "adapter")
    private String adapterClassName;
    @XmlJavaTypeAdapter(ParameterMapAdapter.class)
    @XmlElement(name = "AdapterParameters")
    private Map<String, String> adapterParams;
    @XmlTransient
    private final ChangeWatcher status;

    /**
     * Creates an instance of the {@link Source} class from the provided  {@link DataAdapter}
     *
     * @param adapter the {@link DataAdapter} to create the {@link Source}  instance from
     * @return an instance of the {@link Source} class from the provided  {@link DataAdapter}
     */
    @SuppressWarnings("unchecked")
    public static Source of(DataAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter cannot be null");
        }
        Source newSource = new Source();
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
    }

    /**
     * The name of the {@link Source}
     *
     * @return the name of the {@link Source}
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the {@link Source}
     *
     * @param name the name of the {@link Source}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The name of the class that implements the {@link DataAdapter} for the {@link Source}
     *
     * @return the name of the class that implements the {@link DataAdapter} for the {@link Source}
     */
    public String getAdapterClassName() {
        return adapterClassName;
    }

    /**
     * Returns a map of the parameters required to establish a connection to the data source
     *
     * @return a map of the parameters required to establish a connection to the data source
     */
    public Map<String, String> getAdapterParams() {
        return adapterParams;
    }

    /**
     * Gets the id of the {@link DataAdapter} attached to the source
     *
     * @return the id of the {@link DataAdapter} attached to the source
     */
    public UUID getAdapterId() {
        return adapterId;
    }

    /**
     * Sets the id of the {@link DataAdapter} attached to the source
     *
     * @param adapterId the id of the {@link DataAdapter} attached to the source
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


    private void setAdapterClassName(String adapterClassName) {
        this.adapterClassName = adapterClassName;
    }

    private void setAdapterParams(Map<String, String> adapterParams) {
        this.adapterParams = adapterParams;
    }
}
