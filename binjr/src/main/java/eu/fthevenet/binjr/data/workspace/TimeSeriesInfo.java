package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "TimeSeries")
public class TimeSeriesInfo<T extends Number>  extends Dirtyable implements Comparable<TimeSeriesInfo<T>>, Serializable {
    private final StringProperty displayName;
    private final BooleanProperty selected;
    private final Property<Color> displayColor;
    private final StringProperty path;
    private TimeSeries<T> data;
    private final TimeSeriesBinding<T> binding;
    private final IntegerProperty order;
   // private final BooleanProperty dirty;


    public TimeSeriesInfo(TimeSeriesBinding<T> binding) {
        this(binding.getLegend(),
                true,
                binding.getColor(),
                "",
                null,
                binding,
                0);
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
                seriesInfo.getBinding(),
                seriesInfo.getOrder());
    }

    public TimeSeriesInfo(String displayName,
                          Boolean selected,
                          Color displayColor,
                          String path,
                          TimeSeries<T> data,
                          TimeSeriesBinding<T> binding,
                          Integer order) {
        this.binding = binding;
        this.displayName = new SimpleStringProperty(displayName);
        this.selected = new SimpleBooleanProperty(selected);
        this.displayColor = new SimpleObjectProperty<>(displayColor);
        this.path = new SimpleStringProperty(path);
        this.order = new SimpleIntegerProperty(order);

//
//        ChangeListener<Object> setDirty = (observable, oldValue, newValue) -> dirty.setValue(true);
//        this.displayNameProperty().addListener(setDirty);
//        this.selectedProperty().addListener(setDirty);
//        this.displayColorProperty().addListener(setDirty);
//        this.pathProperty().addListener(setDirty);
//        this.orderProperty().addListener(setDirty);
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName.set(displayName);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public Color getDisplayColor() {
        return displayColor.getValue();
    }

    public Property<Color> displayColorProperty() {
        return displayColor;
    }

    public void setDisplayColor(Color displayColor) {
        this.displayColor.setValue(displayColor);
    }

    public String getPath() {
        return path.get();
    }

    public StringProperty pathProperty() {
        return path;
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public int getOrder() {
        return order.get();
    }

    public IntegerProperty orderProperty() {
        return order;
    }

    public void setOrder(int order) {
        this.order.set(order);
    }

    @XmlTransient
    public TimeSeriesBinding<T> getBinding() {
        return binding;
    }

    @XmlTransient
    public TimeSeries<T> getData() {
        return data;
    }

    public void setData(TimeSeries<T> data) {
        this.data = data;
    }

    @Override
    public int compareTo(TimeSeriesInfo<T> o) {
        return this.order.getValue().compareTo(o.getOrder());
    }
}
