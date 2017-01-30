package eu.fthevenet.binjr.controllers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by ftt2 selected 15/11/2016.
 */
public class SelectableListItem {
    private  StringProperty name = new SimpleStringProperty();
    private  BooleanProperty selected = new SimpleBooleanProperty();
    private Double minimum = 0d;
    private Double maximum = 0d;
    private Double average = 0d;
    private Double current = Double.NaN;

    public SelectableListItem(String name, boolean selected) {
        setName(name);
        setSelected(selected);
    }

    public final StringProperty nameProperty() {
        return this.name;
    }

    public final String getName() {
        return this.nameProperty().get();
    }

    public final void setName(final String name) {
        this.nameProperty().set(name);
    }

    public final BooleanProperty selectedProperty() {
        return this.selected;
    }

    public final boolean getSelected() {
        return this.selectedProperty().get();
    }

    public final void setSelected(final boolean selected) {
        this.selectedProperty().set(selected);
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }

    public Double getMaximum() {
        return maximum;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }

    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }

    public Double getCurrent() {
        return current;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    @Override
    public String toString() {
        return getName();
    }

}
