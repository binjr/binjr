/*
 *    Copyright 2017-2022 Frederic Thevenet
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
 */

package eu.binjr.core.data.workspace;

import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.version.Version;
import eu.binjr.common.xml.XmlUtils;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.CannotLoadWorkspaceException;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A class that represents and holds the current state of the application
 * <p>It provides the means to track changes, persist and reload a state in between usage session of the application</p>
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Workspace")
public class Workspace implements Dirtyable {
    public static final String WORKSPACE_SCHEMA_VERSION = "3.1";
    public static final Version MINIMUM_SUPPORTED_SCHEMA_VERSION = new Version("3.0");
    public static final Version SUPPORTED_SCHEMA_VERSION = new Version(WORKSPACE_SCHEMA_VERSION);

    @XmlTransient
    private static final Logger logger = Logger.create(Workspace.class);
    @XmlElementWrapper(name = "Sources")
    @XmlElements(@XmlElement(name = "Source"))
    @IsDirtyable
    private final ObservableList<Source> sources;
    @XmlTransient
    private final Property<Path> path;
    @XmlElementWrapper(name = "Worksheets")
    @XmlElements(@XmlElement(name = "Worksheet"))
    @IsDirtyable
    private final ObservableList<Worksheet<?>> worksheets;
    @XmlAttribute
    private final SimpleDoubleProperty dividerPosition;
    @XmlTransient
    private final ChangeWatcher status;
    @XmlAttribute(name = "schemaVersion", required = false)
    private final Version schemaVersion = new Version(WORKSPACE_SCHEMA_VERSION);
    @XmlAttribute(name = "producerInfo", required = false)
    private final String producerInfo;
    private transient final BindingManager bindingManager = new BindingManager();
    @XmlTransient
    private final BooleanProperty presentationMode;

    private transient final BooleanProperty sourcePaneVisible;
    private transient final Deque<Worksheet<?>> closedWorksheetQueue = new ArrayDeque<>();
    private transient final BooleanProperty closedWorksheetQueueEmpty = new SimpleBooleanProperty(true);

    /**
     * Initializes a new instance of the {@link Workspace} class
     */
    public Workspace() {
        this(FXCollections.observableList(new ArrayList<>()), FXCollections.observableList(new ArrayList<>()));
    }

    /**
     * Initializes a new instance of the {@link Workspace} class with the provided list of {@link XYChartsWorksheet} and {@link Source} instances
     *
     * @param worksheets the list of  {@link XYChartsWorksheet} instances to initialize the workspace with
     * @param sources    the list of  {@link Source} instances to initialize the workspace with
     */
    private Workspace(ObservableList<Worksheet<?>> worksheets, ObservableList<Source> sources) {
        this.path = new SimpleObjectProperty<>(Paths.get("Untitled"));
        this.worksheets = worksheets;
        this.sources = sources;
        this.dividerPosition = new SimpleDoubleProperty(0.3);

        this.producerInfo = AppEnvironment.getInstance().getAppDescription();
        this.sourcePaneVisible = new SimpleBooleanProperty(true);
        this.presentationMode = new SimpleBooleanProperty(false);

        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    /**
     * Deserializes the content of the provided file as a new instance of the {@link Workspace} class
     *
     * @param file the file to deserialize
     * @return a new instance of the {@link Workspace} class
     * @throws IOException                  if an IO error occurs when accessing the file
     * @throws JAXBException                if an error occurs while deserializing the file
     * @throws CannotLoadWorkspaceException if an error occurs while loading the workspace.
     */
    public static Workspace from(File file) throws IOException, JAXBException, CannotLoadWorkspaceException {
        Workspace workspace;
        if (sanityCheck(file)) {
            try {
                var migrated = XmlUtils.processXslt(Workspace.class.getResourceAsStream("/eu/binjr/xsl/migrate_workspace_schema.xsl"),
                        Files.newInputStream(file.toPath()));
                workspace = XmlUtils.deSerialize(migrated, ReflectionHelper.INSTANCE.getClassesToBeBound().toArray(Class<?>[]::new));
                Dialogs.notifyInfo("This workspace has been migrated from a previous version of binjr",
                        "If you overwrite it, you might not be able to re-open it with a previous version anymore");
            } catch (TransformerException e) {
                throw new CannotLoadWorkspaceException("Error while migrating workspace to current schema", e);
            }
        } else {
            workspace = XmlUtils.deSerialize(file, ReflectionHelper.INSTANCE.getClassesToBeBound().toArray(Class<?>[]::new));
        }
        logger.debug(() -> "Successfully deserialized workspace " + workspace.toString());
        workspace.setPath(file.toPath());
        workspace.cleanUp();
        return workspace;
    }


    private static boolean sanityCheck(File file) throws IOException, CannotLoadWorkspaceException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("Could not find specified workspace file " + file.getPath());
        }
        try {
            String verStr = XmlUtils.getFirstAttributeValue(file, "schemaVersion");
            if (verStr == null) {
                throw new CannotLoadWorkspaceException(
                        "Could not determine the workspace's schema version: it was probably produced with an older, incompatible version of binjr." +
                                "\n (Minimum supported schema version=" + MINIMUM_SUPPORTED_SCHEMA_VERSION + ")");
            }
            Version foundVersion = new Version(verStr);
            if (foundVersion.compareTo(SUPPORTED_SCHEMA_VERSION) > 0) {
                if (foundVersion.getMajor() != SUPPORTED_SCHEMA_VERSION.getMajor()) {
                    // Only throw if major version is different, only warn otherwise.
                    throw new CannotLoadWorkspaceException(
                            "This workspace is not compatible with the current version of binjr. (Supported schema version="
                                    + SUPPORTED_SCHEMA_VERSION
                                    + ", found="
                                    + foundVersion + ")");
                }
                logger.warn("This workspace version is higher that the supported version; there may be incompatibilities (Supported schema version="
                        + SUPPORTED_SCHEMA_VERSION
                        + ", found="
                        + foundVersion + ")");
            }
            if (foundVersion.compareTo(MINIMUM_SUPPORTED_SCHEMA_VERSION) < 0) {
                // Returns true to signal workspace requires schema migration
                return true;
            }
        } catch (XMLStreamException e) {
            throw new CannotLoadWorkspaceException("Error retrieving bjr schema version", e);
        }
        return false;
    }


    /**
     * Add all the elements in the provided collection to the list of {@link XYChartsWorksheet} instances
     *
     * @param worksheetsToAdd the list of {@link XYChartsWorksheet} instances to add
     */
    public void addWorksheets(Collection<Worksheet<?>> worksheetsToAdd) {
        this.worksheets.addAll(worksheetsToAdd);
    }

    /**
     * Add all the elements in the provided collection to the list of {@link XYChartsWorksheet} instances
     *
     * @param worksheetsToAdd the {@link XYChartsWorksheet} instances to add
     */
    public void addWorksheets(Worksheet<?>... worksheetsToAdd) {
        this.worksheets.addAll(worksheetsToAdd);
    }

    /**
     * Remove all the elements in the provided collection from the list of {@link XYChartsWorksheet} instances
     *
     * @param worksheetsToRemove the list of {@link XYChartsWorksheet} instances to remove
     */
    public void removeWorksheets(Worksheet<?>... worksheetsToRemove) {
        for (var w : worksheetsToRemove) {
            closedWorksheetQueue.push(w.duplicate());
            closedWorksheetQueueEmpty.setValue(false);
            w.close();
            this.worksheets.remove(w);
        }
    }

    public Optional<Worksheet<?>> pollClosedWorksheet() {
        var head = this.closedWorksheetQueue.poll();
        closedWorksheetQueueEmpty.setValue(closedWorksheetQueue.isEmpty());
        return (head != null ? Optional.of(head) : Optional.empty());
    }

    public boolean isClosedWorksheetQueueEmpty() {
        return closedWorksheetQueueEmpty.get();
    }

    public ReadOnlyBooleanProperty closedWorksheetQueueEmptyProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(closedWorksheetQueueEmpty);
    }


    /**
     * Clear the {@link XYChartsWorksheet} list
     */
    public void clearWorksheets() {
        IOUtils.closeAll(closedWorksheetQueue);
        closedWorksheetQueue.clear();
        closedWorksheetQueueEmpty.setValue(true);
        IOUtils.closeAll(worksheets);
        worksheets.clear();
    }

    /**
     * Clear the {@link Source} list
     */
    public void clearSources() {
        IOUtils.closeAll(sources);
        sources.clear();
    }

    /**
     * /**
     * Add all the elements in the provided collection to the list of {@link Source} instances
     *
     * @param sourcesToAdd the list of {@link Source} instances to add
     */
    public void addSources(Collection<Source> sourcesToAdd) {
        sourcesToAdd.forEach(this::addSource);
    }

    /**
     * Add a single {@link Source} instance to the worksheet.
     *
     * @param sourceToAdd the {@link Source} instance to add.
     */
    public void addSource(Source sourceToAdd) {
        this.sources.add(sourceToAdd);
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
     * Remove a single {@link Source} instance from the worksheet.
     *
     * @param sourceToRemove the {@link Source} to remove.
     */
    public void removeSource(Source sourceToRemove) {
        this.sources.remove(sourceToRemove);
    }

    /**
     * Returns all {@link XYChartsWorksheet} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     *
     * @return all {@link XYChartsWorksheet} instances currently held by the {@link Workspace} as an {@link Iterable} structure.
     */
    public ObservableList<Worksheet<?>> getWorksheets() {
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
            UserHistory.getInstance().mostRecentWorkspaces.push(path);
        }
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

    @Override
    public String toString() {
        return "Workspace{" + "sources=" + sources +
                ", path='" + path + '\'' +
                ", worksheets=" + worksheets +
                '}';
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

    public void close() {
        logger.debug("Closing Workspace " + this);
        try {
            clear();
            this.status.close();
            bindingManager.close();
        } catch (Exception e) {
            logger.warn("An error occurred while closing the workspace " + (this.getPath() != null ? getPath() : "null"), e);
        }
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

        XmlUtils.serialize(this, file, ReflectionHelper.INSTANCE.getClassesToBeBound().toArray(Class<?>[]::new));
        setPath(file.toPath());
        cleanUp();
        UserHistory.getInstance().mostRecentSaveFolders.push(file.toPath());
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

    public BindingManager getBindingManager() {
        return bindingManager;
    }

    public Boolean isSourcePaneVisible() {
        return sourcePaneVisible.getValue();
    }

    public Property<Boolean> sourcePaneVisibleProperty() {
        return sourcePaneVisible;
    }

    public void setSourcePaneVisible(boolean value) {
        sourcePaneVisible.setValue(value);
    }

    public boolean isPresentationMode() {
        return presentationMode.get();
    }

    public BooleanProperty presentationModeProperty() {
        return presentationMode;
    }

    public void setPresentationMode(boolean presentationMode) {
        this.presentationMode.set(presentationMode);
    }

    public DoubleProperty dividerPositionProperty() {
        return dividerPosition;
    }

    // @XmlAttribute
    public Double getDividerPosition() {
        return dividerPosition.getValue();
    }

    public void setDividerPosition(Double dividerPosition) {
        this.dividerPosition.setValue(dividerPosition);
    }
}
