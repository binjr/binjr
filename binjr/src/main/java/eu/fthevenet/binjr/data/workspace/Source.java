package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import javafx.beans.property.BooleanProperty;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * A class that represents and holds the current state of a single data source
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Source")
public class Source implements Serializable, Dirtyable {
    private UUID adapterId;
    @XmlElement(name = "Name")
    @IsDirtyable
    private String name;
    @XmlElement(name = "Adapter")
    private Class adapterClass;
    @XmlElementWrapper(name = "Parameters")
    @XmlElements(@XmlElement(name = "Parameter"))
    private Map<String, String> AdapterParams;
    @XmlTransient
    private final ChangeWatcher<Source> status;

    /**
     * Creates an instance of the {@link Source} class from the provided  {@link DataAdapter}
     *
     * @param adapter the {@link DataAdapter} to create the {@link Source}  instance from
     * @return an instance of the {@link Source} class from the provided  {@link DataAdapter}
     */
    @SuppressWarnings("unchecked")
    public static Source of(DataAdapter adapter) {
        Source newSource = new Source();
        newSource.setName(adapter.getSourceName());
        newSource.setAdapterClass(adapter.getClass());
        newSource.setAdapterParams(adapter.getParams());
        newSource.setAdapterId(adapter.getId());
        return newSource;
    }

    /**
     * Initializes a new instance of the {@link Source} class
     */
    public Source() {
        this.status = new ChangeWatcher<>(this);
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
    public Class getAdapterClass() {
        return adapterClass;
    }

    /**
     * Returns a map of the parameters required to establish a connection to the data source
     *
     * @return a map of the parameters required to establish a connection to the data source
     */
    public Map<String, String> getAdapterParams() {
        return AdapterParams;
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


    private void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    private void setAdapterParams(Map<String, String> adapterParams) {
        AdapterParams = adapterParams;
    }
}
