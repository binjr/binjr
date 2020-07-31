/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import javafx.beans.property.*;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.*;

/**
 * A class that represents and holds the current state of the representation of a single time series
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Timeseries")
public class TimeSeriesInfo<T> implements Dirtyable {
    @IsDirtyable
    private final StringProperty displayName;
    @IsDirtyable
    private final BooleanProperty selected;
    @IsDirtyable
    private final Property<Color> displayColor;
    @XmlElement(name = "Binding", required = true, type = TimeSeriesBinding.class)
    private final SourceBinding<T> binding;
    private final ChangeWatcher status;
    private final Property<TimeSeriesProcessor<T>> processor = new SimpleObjectProperty<>();

    /**
     * Parameter-less constructor (needed for XMl serialization)
     */
    private TimeSeriesInfo() {
        this("",
                true,
                null,
                null);
    }

    /**
     * Copy constructor to deep clone a {@link TimeSeriesInfo} instance.
     * <p><b>Remark:</b></p>
     * <p>All the properties of the new {@link TimeSeriesInfo} instance are new objects, assigned the same values, except for the {@code binding} property
     * which holds a copy of the reference. <br>In other words, the {@link TimeSeriesBinding} reference is shared amongst all clones produced by this constructor.
     * </p>
     *
     * @param seriesInfo the {@link TimeSeriesInfo} instance to clone.
     */
    public TimeSeriesInfo(TimeSeriesInfo<T> seriesInfo) {
        this(seriesInfo.getDisplayName(),
                seriesInfo.isSelected(),
                seriesInfo.getDisplayColor(),
                seriesInfo.getBinding());
    }

    /**
     * Initialises a new instance of the {@link TimeSeriesInfo} class
     *
     * @param displayName  the name for the series
     * @param selected     true if the series is selected, false otherwise
     * @param displayColor the color of the series
     * @param binding      the {@link TimeSeriesBinding}  for the series
     */
    public TimeSeriesInfo(String displayName,
                           Boolean selected,
                           Color displayColor,
                           SourceBinding<T> binding) {
        this.binding = binding;
        this.displayName = new SimpleStringProperty(displayName);
        this.selected = new SimpleBooleanProperty(selected);
        this.displayColor = new SimpleObjectProperty<>(displayColor);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    /**
     * Gets the display name fo the series
     *
     * @return the display name fo the series
     */
    @XmlAttribute
    public String getDisplayName() {
        return displayName.get();
    }

    /**
     * Sets the display name fo the series
     *
     * @param displayName the display name fo the series
     */
    public void setDisplayName(String displayName) {
        this.displayName.set(displayName);
    }

    /**
     * The displayName property
     *
     * @return the displayName property
     */
    public StringProperty displayNameProperty() {
        return displayName;
    }

    /**
     * Returns true if the series is selected, false otherwise
     *
     * @return true if the series is selected, false otherwise
     */
    @XmlAttribute
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Set to true if the series is selected, false otherwise
     *
     * @param selected true if the series is selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    /**
     * The selected property
     *
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * Returns the display color for the series
     *
     * @return the display color for the series
     */
    @XmlAttribute
    public Color getDisplayColor() {
        return displayColor.getValue();
    }

    /**
     * Sets the display color for the series
     *
     * @param displayColor the display color for the series
     */
    public void setDisplayColor(Color displayColor) {
        this.displayColor.setValue(displayColor);
    }

    /**
     * The displayColor property
     *
     * @return the displayColor property
     */
    public Property<Color> displayColorProperty() {
        return displayColor;
    }

    /**
     * Gets the {@link TimeSeriesBinding} to get the data from
     *
     * @return the {@link TimeSeriesBinding} to get the data from
     */
    public SourceBinding<T> getBinding() {
        return binding;
    }

    /**
     * Gets the data processor for the series
     *
     * @return the data processor for the series
     */
    @XmlTransient
    public TimeSeriesProcessor<T> getProcessor() {
        return processor.getValue();
    }

    /**
     * Sets the data processor for the series
     *
     * @param processor the data processor for the series
     */
    public void setProcessor(TimeSeriesProcessor<T> processor) {
        this.processor.setValue(processor);
    }

    /**
     * The processor property.
     *
     * @return The processor property.
     */
    public Property<TimeSeriesProcessor<T>> processorProperty() {
        return this.processor;
    }

    @XmlTransient
    @Override
    public Boolean isDirty() {
        return status.isDirty();
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return status.dirtyProperty();
    }

    @Override
    public void cleanUp() {
        status.cleanUp();
    }

    @Override
    public String toString() {
        return "TimeSeriesInfo{" +
                "displayName=" + displayName +
                ", selected=" + selected +
                ", displayColor=" + displayColor +
                ", binding=" + binding +
                '}';
    }

    @Override
    public void close() {
        this.status.close();
    }
}
