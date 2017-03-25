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
public class TimeSeriesInfo<T extends Number>  implements Serializable, Dirtyable {
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


   public  static <T extends Number> TimeSeriesInfo<T> fromBinding(TimeSeriesBinding<T> binding){
       if (binding == null){
           throw new IllegalArgumentException("binding cannot be null");
       }
       return new TimeSeriesInfo<>(binding.getLegend(),
               true,
               binding.getColor(),
               "",
               null,
               binding);
   }

   private TimeSeriesInfo(){
       this("",
               true,
               null,
               "",
               null,
               null);
   }

    private TimeSeriesInfo(TimeSeriesBinding<T> binding) {
        this(binding.getLegend(),
                true,
                binding.getColor(),
                "",
                null,
                binding);
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

    public TimeSeriesInfo(String displayName,
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


    public TimeSeriesBinding<T> getBinding() {
        return binding;
    }

    @XmlTransient
    public TimeSeriesProcessor<T> getProcessor() {
        return processor;
    }

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
