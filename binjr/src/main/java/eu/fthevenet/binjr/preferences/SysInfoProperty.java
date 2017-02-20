package eu.fthevenet.binjr.preferences;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FTT2 on 20/02/2017.
 */
public class SysInfoProperty {
    private Property<String> key;
    private Property<String> value;

    public SysInfoProperty(String key, String value) {
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
    }

    public String getKey() {
        return key.getValue();
    }

    public Property<String> keyProperty() {
        return key;
    }

    public void setKey(String key) {
        this.key.setValue(key);
    }

    public String getValue() {
        return value.getValue();
    }

    public Property<String> valueProperty() {
        return value;
    }

    public void setValue(String value) {
        this.value.setValue(value);
    }


}
