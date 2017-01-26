package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Created by FTT2 on 26/01/2017.
 */
public class TimeSeriesItem<T extends Number> {
    private final Property<Boolean> enabled = new SimpleBooleanProperty(true);
    private final TimeSeries<T> series;


    public TimeSeriesItem(TimeSeries<T> series) {
        this.series = series;
    }

    public Boolean getEnabled() {
        return enabled.getValue();
    }

    public Property<Boolean> enabledProperty() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled.setValue(enabled);
    }

    public TimeSeries<T> getSeries() {
        return series;
    }
}
