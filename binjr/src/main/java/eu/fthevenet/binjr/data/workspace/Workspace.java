package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableListWrapper;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsDataAdapter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a workspace
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Workspace")
public class Workspace implements Serializable {
    @XmlTransient
    private BooleanProperty dirty;

    @XmlElementWrapper(name = "Sources")
    @XmlElements(@XmlElement(name = "Source"))
    private List<Source> sources;
    private String name;

    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    private List<Worksheet> worksheets;

    public Workspace() {
        this("New Workspace",
                new ArrayList<>(),
                new ArrayList<>());
    }

    public Workspace(String name, List<Worksheet> worksheets, List<Source> sources) {
        this.dirty = new SimpleBooleanProperty(false);
        this.name = name;
        this.worksheets = worksheets;
        this.sources = sources;
    }

    @XmlTransient
    private ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
        if (newValue) {
            this.dirty.setValue(true);
        }
    };

    public void addWorksheets(Collection<Worksheet> worksheets) {
        for (Worksheet w : worksheets) {
            dirty.setValue(dirty.getValue() | w.isDirty());
            w.dirtyProperty().addListener(changeListener);
        }
        this.worksheets.addAll(worksheets);
    }

    public void removeWorksheets(Collection<Worksheet> worksheets) {
        for (Worksheet w : worksheets) {
            w.dirtyProperty().removeListener(changeListener);
        }
        this.worksheets.removeAll(worksheets);
    }

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public List<Source> getSources() {
        return sources;
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
        sb.append("sources=").append(sources);
        sb.append(", name='").append(name).append('\'');
        sb.append(", worksheets=").append(worksheets);
        sb.append('}');
        return sb.toString();
    }

    public Boolean isDirty() {
        return dirty.getValue();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setSaved() {
        worksheets.forEach(Worksheet::setSaved);
        this.dirty.setValue(false);
    }

    public void clear(){
        this.dirty.setValue(false);
        this.worksheets.clear();
        this.sources.clear();
        this.name= "New Workspace";
    }
}
