package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a workspace
 *
 * @author Frederic Thevenet
 */
public class Workspace implements Serializable {
    private List<Worksheet> worksheets;
    private List<DataAdapter> adapters;
    private String name;

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public void setWorksheets(List<Worksheet> worksheets) {
        this.worksheets = worksheets;
    }

    public List<DataAdapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<DataAdapter> adapters) {
        this.adapters = adapters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
