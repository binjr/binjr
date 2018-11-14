/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.preferences;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

/**
 * A structure to hold system properties with observable keys and values
 *
 * @author Frederic Thevenet
 */
public class SysInfoProperty {
    private Property<String> key;
    private Property<String> value;

    public SysInfoProperty(String key, String value) {
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
    }

    /**
     * Gets the content of the key {@link Property}
     *
     * @return the key
     */
    public String getKey() {
        return key.getValue();
    }

    /**
     * Returns the {@link Property} that holds the key
     *
     * @return the {@link Property} that holds the key
     */
    public Property<String> keyProperty() {
        return key;
    }

    /**
     * Sets the content of the key {@link Property}
     *
     * @param key the key
     */
    public void setKey(String key) {
        this.key.setValue(key);
    }

    /**
     * Gets the content of the value {@link Property}
     *
     * @return the content of the value {@link Property}
     */
    public String getValue() {
        return value.getValue();
    }

    /**
     * Returns the {@link Property} that holds the value
     *
     * @return the {@link Property} that holds the value
     */
    public Property<String> valueProperty() {
        return value;
    }

    /**
     * Sets the content of the value {@link Property}
     *
     * @param value the content of the value {@link Property}
     */
    public void setValue(String value) {
        this.value.setValue(value);
    }


}
