package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsDataAdapter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workspace
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Workspace")
public class Workspace implements Serializable {

    @XmlElementWrapper(name = "Adapters")
    @XmlElements(@XmlElement(name = "Adapter", type = JrdsDataAdapter.class))
    private List<DataAdapter> adapters;
    private String name;

    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    private List<Worksheet> worksheets = new ArrayList<>();

    public Workspace() {
        this("New Workspace",
                new ArrayList<>(),
                new ArrayList<>());
    }

    public Workspace(String name, List<Worksheet> worksheets, List<DataAdapter> adapters) {
        this.name = name;
        this.worksheets = worksheets;
        this.adapters = adapters;
    }

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public List<DataAdapter> getAdapters() {
        return adapters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Workspace{");
        sb.append("adapters=").append(adapters);
        sb.append(", name='").append(name).append('\'');
        sb.append(", worksheets=").append(worksheets);
        sb.append('}');
        return sb.toString();
    }
}
