package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableListWrapper;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a single worksheet
 *
 * @author Frederic Thevenet
 */
public class Worksheet implements Serializable {
    private ObservableList<TimeSeries<Number>> series;
    private Property<String> name;

    public Worksheet(String name) {
        this.name = new SimpleStringProperty(name);
        this.series = new ObservableListWrapper<>(new ArrayList<>());
    }

    public String getName() {
        return name.getValue();
    }

    public Property<String> nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public ObservableList<TimeSeries<Number>> getSeries() {
        return series;
    }

    public void setSeries(ObservableList<TimeSeries<Number>> series) {
        this.series = series;
    }
}
