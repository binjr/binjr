package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.xml.XmlUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final Logger logger = LogManager.getLogger(Workspace.class);
    @XmlTransient
    private BooleanProperty dirty;

    @XmlElementWrapper(name = "Sources")
    @XmlElements(@XmlElement(name = "Source"))
    private final Collection<Source> sources;
    //  private String name;
    @XmlTransient
    private final Property<Path> path;

    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    private final Collection<Worksheet> worksheets;

    public Workspace() {
        this(new ArrayList<>(),
                new ArrayList<>());
    }

    public Workspace(List<Worksheet> worksheets, List<Source> sources) {
        this.dirty = new SimpleBooleanProperty(false);
        this.path = new SimpleObjectProperty<>(Paths.get("Untitled"));
        this.worksheets = worksheets;
        this.sources = sources;
    }

    @XmlTransient
    private ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
        if (newValue) {
            dirty.setValue(true);
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

    public void clearWorksheets() {
        worksheets.forEach(w -> w.dirtyProperty().removeListener(changeListener));
        worksheets.clear();
    }

    public void clearSources() {
        // sources.forEach(s -> s.dirtyProperty().removeListener(changeListener));
        sources.clear();
    }

    public void addSources(Collection<Source> sources) {
//        for (Source s : sources) {
//            dirty.setValue(dirty.getValue() | w.isDirty());
//            w.dirtyProperty().addListener(changeListener);
//        }
        this.sources.addAll(sources);
    }

    public void removeSources(Collection<Source> sources) {
        this.sources.removeAll(sources);
    }

    public Iterable<Worksheet> getWorksheets() {
        return worksheets;
    }

    public Iterable<Source> getSources() {
        return sources;
    }

    public Path getPath() {
        return path.getValue();
    }

    public Property<Path> pathProperty() {
        return path;
    }

    public boolean hasPath() {
        if (getPath() == null){
            return false;
        }
        return getPath().toFile().exists();
    }

    public void setPath(Path path) {
        this.path.setValue(path);
        if (hasPath()){
            GlobalPreferences.getInstance().setMostRecentSavedWorkspace(path);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Workspace{");
        sb.append("sources=").append(sources);
        sb.append(", path='").append(path).append('\'');
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

    public void clear() {
        clearWorksheets();
        clearSources();
        setSaved();
        this.setPath(Paths.get("Untitled"));
    }

    public void save() throws IOException, JAXBException {
        if (!hasPath()) {
            throw new UnsupportedOperationException("Cannot save workspace: a path has not been specified");
        }
        this.save(getPath().toFile());
    }

    public void save(File file) throws IOException, JAXBException {
        XmlUtils.serialize(this, file);
        setPath(file.toPath());
        setSaved();
        GlobalPreferences.getInstance().setMostRecentSaveFolder(file.getParent());
    }

    public static Workspace from(File file) throws IOException, JAXBException {
        Workspace workspace = XmlUtils.deSerialize(Workspace.class, file);
        logger.debug(() -> "Successfully deserialized workspace " + workspace.toString());
        workspace.setPath(file.toPath());
        workspace.setSaved();
        return workspace;
    }

    private void setSaved() {
        worksheets.forEach(Worksheet::setSaved);
        this.dirty.setValue(false);
    }


}
