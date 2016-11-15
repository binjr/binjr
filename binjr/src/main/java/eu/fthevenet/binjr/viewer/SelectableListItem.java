package eu.fthevenet.binjr.viewer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by ftt2 selected 15/11/2016.
 */
public class SelectableListItem {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();

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

    @Override
    public String toString() {
        return getName();
    }

}
