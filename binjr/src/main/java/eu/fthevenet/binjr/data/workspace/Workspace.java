package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableListWrapper;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsDataAdapter;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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
@XmlTransient
    private final Property<Boolean> dirty;

    @XmlElementWrapper(name = "Sources")
    @XmlElements(@XmlElement(name = "Source"))
    private List<Source> sources;
    private String name;

    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    private List<Worksheet> worksheets;// = new ArrayList<>();
    @XmlTransient
    private ObservableList<Worksheet> observableWorksheets;

    public Workspace() {
        this("New Workspace",
                new ArrayList<>(),
                new ArrayList<>());
    }

    public Workspace(String name, List<Worksheet> worksheets, List<Source> sources) {
        this.dirty = new SimpleBooleanProperty(false);
        this.name = name;
        this.worksheets = worksheets;
        this.observableWorksheets = new ObservableListWrapper<Worksheet>(worksheets);
        this.sources = sources;
        observableWorksheets.addListener((ListChangeListener<Worksheet>) c -> {
            c.next();
            if (c.wasAdded()) {
                for (Worksheet w : c.getAddedSubList()) {
                    w.dirtyProperty().addListener((observable, oldValue, newValue) -> dirty.setValue(dirty.getValue() | newValue));
                }
            }
        });
    }

    public List<Worksheet> getWorksheets() {
        return observableWorksheets;
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

//    public boolean isDirty() {
//        return getWorksheets().stream().map(Worksheet::isDirty).reduce(false, Boolean::logicalOr);
//    }

    public boolean isEmpty() {
        return getWorksheets().isEmpty() && getSources().isEmpty();
    }

    public Boolean isDirty() {
        return dirty.getValue();
    }

    public Property<Boolean> dirtyProperty() {
        return dirty;
    }

    public void setSaved() {
        this.dirty.setValue(false);
    }
}
