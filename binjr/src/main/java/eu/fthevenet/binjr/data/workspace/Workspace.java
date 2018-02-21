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

import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import eu.fthevenet.binjr.preferences.AppEnvironment;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.xml.XmlUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class that represents and holds the current state of the application
 * <p>It provides the means to track changes, persist and reload a state in between usage session of the application</p>
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Workspace")
public class Workspace implements Dirtyable {
    public static final String WORKSPACE_SCHEMA_VERSION = "1.2";
    @XmlTransient
    private static final Logger logger = LogManager.getLogger(Workspace.class);
    @XmlElementWrapper(name = "Sources")
    @XmlElements(@XmlElement(name = "Source"))
    @IsDirtyable
    private final ObservableList<Source> sources;
    @XmlTransient
    private final Property<Path> path;
    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    @IsDirtyable
    private final ObservableList<Worksheet> worksheets;
    @XmlTransient
    private final ChangeWatcher status;
    @XmlAttribute(name = "schemaVersion", required = false)
    private final String schemaVersion = WORKSPACE_SCHEMA_VERSION;
    @XmlAttribute(name = "producerInfo", required = false)
    private final String producerInfo;

    /**
     * Initializes a new instance of the {@link Workspace} class
     */
    public Workspace() {
        this(FXCollections.observableList(new ArrayList<>()), FXCollections.observableList(new ArrayList<>()));
    }

    /**
     * Initializes a new instance of the {@link Workspace} class with the provided list of {@link Worksheet} and {@link Source} instances
     *
     * @param worksheets the list of  {@link Worksheet} instances to initialize the workspace with
     * @param sources    the list of  {@link Source} instances to initialize the workspace with
     */
    private Workspace(ObservableList<Worksheet> worksheets, ObservableList<Source> sources) {
        this.path = new SimpleObjectProperty<>(Paths.get("Untitled"));
        this.worksheets = worksheets;
        this.sources = sources;
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
        this.producerInfo = AppEnvironment.getInstance().getAppDescription();
    }

    /**
     * Add all the elements in the provided collection to the list of {@link Worksheet} instances
     *
     * @param worksheetsToAdd the list of {@link Worksheet} instances to add
     */
    public void addWorksheets(Collection<Worksheet<?>> worksheetsToAdd) {
        this.worksheets.addAll(worksheetsToAdd);
    }

    public void addWorksheets(Worksheet<?>... worksheetsToAdd) {
        this.worksheets.addAll(worksheetsToAdd);
    }

    /**
     * Remove all the elements in the provided collection from the list of {@link Worksheet} instances
     *
     * @param worksheetsToRemove the list of {@link Worksheet} instances to remove
     */
    public void removeWorksheets(Worksheet... worksheetsToRemove) {
        this.worksheets.removeAll(worksheetsToRemove);
    }

    /**
     * Clear the {@link Worksheet} list
     */
    public void clearWorksheets() {
        worksheets.clear();
    }

    /**
     * Clear the {@link Source} list
     */
    public void clearSources() {
        this.sources.clear();
    }

    /**
     * /**
     * Add all the elements in the provided collection to the list of {@link Source} instances
     *
     * @param sourcesToAdd the list of {@link Source} instances to add
     */
    public void addSources(Collection<Source> sourcesToAdd) {
        this.sources.addAll(sourcesToAdd);
    }

    /**
     * Remove all the elements in the provided collection from the list of {@link Source} instances
     *
     * @param sourcesToRemove the list of {@link Source} instances to remove
     */
    public void removeSources(Collection<Source> sourcesToRemove) {
        this.sources.removeAll(sourcesToRemove);
    }

    /**
     * Returns all {@link Worksheet} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     *
     * @return all {@link Worksheet} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     */
    public Iterable<Worksheet> getWorksheets() {
        return worksheets;
    }

    /**
     * Returns all {@link Source} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     *
     * @return all {@link Source} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     */
    public Iterable<Source> getSources() {
        return sources;
    }

    /**
     * Returns the {@link Path} for the serialized form of the {@link Workspace}
     *
     * @return the {@link Path} for the serialized form of the {@link Workspace}
     */
    public Path getPath() {
        return path.getValue();
    }

    /**
     * Returns the property that observes the {@link Path} for the serialized form of the {@link Workspace}
     *
     * @return the property that observes the {@link Path} for the serialized form of the {@link Workspace}
     */
    public Property<Path> pathProperty() {
        return path;
    }

    /**
     * Returns true is the {@link Workspace} has a valid {@link Path}, false otherwise
     *
     * @return true is the {@link Workspace} has a valid {@link Path}, false otherwise
     */
    public boolean hasPath() {
        if (getPath() == null) {
            return false;
        }
        return getPath().toFile().exists();
    }

    /**
     * Sets the {@link Path} for the serialized form of the {@link Workspace}
     *
     * @param path the {@link Path} for the serialized form of the {@link Workspace}
     */
    public void setPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        this.path.setValue(path);
        if (hasPath()) {
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

    /**
     * Clear all the properties of the {@link Workspace} instance
     */
    public void clear() {
        clearWorksheets();
        clearSources();
        cleanUp();
        this.setPath(Paths.get("Untitled"));
    }

    /**
     * Commits the current state of the {@link Workspace} instance to its persistence layer
     *
     * @throws IOException   if an IO error occurs with the persistence layer
     * @throws JAXBException if an error occurs while serializing the current state of the {@link Workspace}
     */
    public void save() throws IOException, JAXBException {
        if (!hasPath()) {
            throw new UnsupportedOperationException("Cannot save workspace: a path has not been specified");
        }
        this.save(getPath().toFile());
    }

    /**
     * Commits the current state of the {@link Workspace} instance to the provided file
     *
     * @param file the file to save the the current state of the {@link Workspace} to
     * @throws IOException   if an IO error occurs when accessing the file
     * @throws JAXBException if an error occurs while serializing the current state of the {@link Workspace}
     */
    public void save(File file) throws IOException, JAXBException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        XmlUtils.serialize(this, file);
        setPath(file.toPath());
        cleanUp();
        GlobalPreferences.getInstance().setMostRecentSaveFolder(file.getParent());
    }

    /**
     * Deserializes the content of the provided file as a new instance of the {@link Workspace} class
     *
     * @param file the file to deserialize
     * @return a new instance of the {@link Workspace} class
     * @throws IOException   if an IO error occurs when accessing the file
     * @throws JAXBException if an error occurs while deserializing the file
     */
    public static Workspace from(File file) throws IOException, JAXBException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        Workspace workspace = XmlUtils.deSerialize(Workspace.class, file);
        logger.debug(() -> "Successfully deserialized workspace " + workspace.toString());
        workspace.setPath(file.toPath());
        workspace.cleanUp();
        return workspace;
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
}
