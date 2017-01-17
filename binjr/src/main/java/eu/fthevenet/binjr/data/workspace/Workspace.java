package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.providers.DataProvider;

import java.io.Serializable;
import java.util.List;

/**
 * Created by FTT2 on 17/01/2017.
 */
public class Workspace implements Serializable {
    private List<Worksheet> worksheets;
    private List<DataProvider> sources;
    private String name;

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public void setWorksheets(List<Worksheet> worksheets) {
        this.worksheets = worksheets;
    }

    public List<DataProvider> getSources() {
        return sources;
    }

    public void setSources(List<DataProvider> sources) {
        this.sources = sources;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
