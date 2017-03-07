package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.Map;

/**
 * A class that represents and holds the current state of a single data source
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Source")
public class Source implements Serializable {
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Adapter")
    private Class adapterClass;
    @XmlElementWrapper(name = "Parameters")
    @XmlElements(@XmlElement(name = "Parameter"))
    private Map<String, String> AdapterParams;

    /**
     * Creates an instance of the {@link Source} class from the provided  {@link DataAdapter}
     *
     * @param adapter the {@link DataAdapter} to create the {@link Source}  instance from
     * @return an instance of the {@link Source} class from the provided  {@link DataAdapter}
     */
    public static Source of(DataAdapter adapter) {
        Source newSource = new Source();
        newSource.setName(adapter.getSourceName());
        newSource.setAdapterClass(adapter.getClass());
        newSource.setAdapterParams(adapter.getParams());
        return newSource;
    }

    /**
     * Initializes a new instance of the {@link Source} class
     */
    public Source() {
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
     * The name of the class that implements the {@link DataAdapter} for the {@link Source}
     *
     * @param adapterClass the name of the class that implements the {@link DataAdapter} for the {@link Source}
     */
    public void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
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
     * Sets the parameters required to establish a connection to the data source
     *
     * @param adapterParams a map of the parameters required to establish a connection to the data source
     */
    public void setAdapterParams(Map<String, String> adapterParams) {
        AdapterParams = adapterParams;
    }
}
