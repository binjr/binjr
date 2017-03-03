package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a  data source
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

    public Source() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getAdapterClass() {
        return adapterClass;
    }

    public void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    public Map<String, String>  getAdapterParams() {
        return AdapterParams;
    }

    public void setAdapterParams(Map<String, String>  adapterParams) {
        AdapterParams = adapterParams;
    }

    public static Source of(DataAdapter adapter){
        Source newSource = new Source();
        newSource.setName(adapter.getSourceName());
        newSource.setAdapterClass(adapter.getClass());
        newSource.setAdapterParams(adapter.getParams());
        return newSource;
    }
}
