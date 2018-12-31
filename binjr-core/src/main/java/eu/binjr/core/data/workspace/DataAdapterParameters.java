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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a collection of of {@link DataAdapterParameter} instances.
 */
public class DataAdapterParameters {
    @XmlElements(@XmlElement(name = "AdapterParameter"))
    public List<DataAdapterParameter> parameters = new ArrayList<>();

    /**
     * Initializes a new instance fo the {@link DataAdapterParameters} class.
     */
    public DataAdapterParameters() {
    }

    /**
     * Initializes a new instance fo the {@link DataAdapterParameters} class.
     *
     * @param parameters A list of {@link DataAdapterParameter} instances to initializes.
     */
    public DataAdapterParameters(List<DataAdapterParameter> parameters) {
        this.parameters = parameters;
    }
}
