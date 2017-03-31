package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import javafx.beans.property.*;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * A class that represents and holds the current state of a single time series
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Timeseries")
public class TimeSeriesInfo<T extends Number> implements Serializable, Dirtyable {
    @IsDirtyable
    private final StringProperty displayName;
    @IsDirtyable
    private final BooleanProperty selected;
    @IsDirtyable
    private final Property<Color> displayColor;
    @IsDirtyable
    private final StringProperty path;
    private TimeSeriesProcessor<T> processor;
    @XmlElement(name = "Binding", required = true, type = TimeSeriesBinding.class)
    private final TimeSeriesBinding<T> binding;

    private final ChangeWatcher<TimeSeriesInfo> status;

    /**
     * Returns a new instance of the {@link TimeSeriesInfo} class built from the specified {@link TimeSeriesBinding}
     *
     * @param binding the {@link TimeSeriesBinding} to build the {@link TimeSeriesInfo} from
     * @param <T>     the type of Y data for that series
     * @return a new instance of the {@link TimeSeriesInfo} class built from the specified {@link TimeSeriesBinding}
     */
    public static <T extends Number> TimeSeriesInfo<T> fromBinding(TimeSeriesBinding<T> binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding cannot be null");
        }
        return new TimeSeriesInfo<>(binding.getLegend(),
                true,
                binding.getColor(),
                "",
                null,
                binding);
    }

    /**
     * Parameter-less constructor (needed for XMl serialization)
     */
    private TimeSeriesInfo() {
        this("",
                true,
                null,
                "",
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
                seriesInfo.getPath(),
                null,
                seriesInfo.getBinding());
    }

    /**
     * Initialises a new instance of the {@link TimeSeriesInfo} class
     *
     * @param displayName  the name for the series
     * @param selected     true if the series is selected, false otherwise
     * @param displayColor the color of the series
     * @param path         the path for the series data in the source
     * @param data         the data processor for the series
     * @param binding      the {@link TimeSeriesBinding}  for the series
     */
    private TimeSeriesInfo(String displayName,
                           Boolean selected,
                           Color displayColor,
                           String path,
                           TimeSeriesProcessor<T> data,
                           TimeSeriesBinding<T> binding) {
        this.binding = binding;
        this.displayName = new SimpleStringProperty(displayName);
        this.selected = new SimpleBooleanProperty(selected);
        this.displayColor = new SimpleObjectProperty<>(displayColor);
        this.path = new SimpleStringProperty(path);
        status = new ChangeWatcher<>(this);
    }

    /**
     * Gets the display name fo the series
     *
     * @return the display name fo the series
     */
    public String getDisplayName() {
        return displayName.get();
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
     * Sets the display name fo the series
     *
     * @param displayName the display name fo the series
     */
    public void setDisplayName(String displayName) {
        this.displayName.set(displayName);
    }

    /**
     * Returns true if the series is selected, false otherwise
     *
     * @return true if the series is selected, false otherwise
     */
    public boolean isSelected() {
        return selected.get();
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
     * Set to true if the series is selected, false otherwise
     *
     * @param selected true if the series is selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    /**
     * Returns the display color for the series
     *
     * @return the display color for the series
     */
    public Color getDisplayColor() {
        return displayColor.getValue();
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
     * Sets the display color for the series
     *
     * @param displayColor the display color for the series
     */
    public void setDisplayColor(Color displayColor) {
        this.displayColor.setValue(displayColor);
    }

    /**
     * Gets the path in the tree for the series
     *
     * @return the path in the tree for the series
     */
    public String getPath() {
        return path.get();
    }

    /**
     * The path property
     *
     * @return the path property
     */
    public StringProperty pathProperty() {
        return path;
    }

    /**
     * Sets the path in the tree for the series
     *
     * @param path the path in the tree for the series
     */
    public void setPath(String path) {
        this.path.set(path);
    }

    /**
     * Gets the {@link TimeSeriesBinding} to get the data from
     *
     * @return the {@link TimeSeriesBinding} to get the data from
     */
    public TimeSeriesBinding<T> getBinding() {
        return binding;
    }

    /**
     * Gets the data processor for the series
     *
     * @return the data processor for the series
     */
    @XmlTransient
    public TimeSeriesProcessor<T> getProcessor() {
        return processor;
    }

    /**
     * Sets the data processor for the series
     *
     * @param processor the data processor for the series
     */
    public void setProcessor(TimeSeriesProcessor<T> processor) {
        this.processor = processor;
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
}
