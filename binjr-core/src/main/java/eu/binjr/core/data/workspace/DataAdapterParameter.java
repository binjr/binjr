/*
 *    Copyright 2017-2018 Frederic Thevenet
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

import eu.binjr.core.data.adapters.DataAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single parameter used to confidure a {@link DataAdapter} instance.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "parameter")
public class DataAdapterParameter {
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String value;

    /**
     * Initializes a new instance of the {@link DataAdapterParameter} class.
     */
    public DataAdapterParameter() {
        this.name = "";
        this.value = "";
    }

    /**
     * Initializes a new instance of the {@link DataAdapterParameter} class.
     *
     * @param name  the name of the parameter.
     * @param value the value of the parameter.
     */
    public DataAdapterParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Sets the name of the parameter.
     *
     * @param name the name of the parameter.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the parameter.
     *
     * @return the name of the parameter.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the parameter.
     *
     * @param value the value of the parameter.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets  the value of the parameter.
     *
     * @return the value of the parameter.
     */
    public String getName() {
        return name;
    }
}
