/*
 *    Copyright 2018 Frederic Thevenet
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

package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.dialogs.DataAdapterDialog;

import java.net.URI;

/**
 * An immutable representation of a {@link DataAdapter}'s metadata
 *
 * @author Frederic Thevenet
 */
public class BaseDataAdapterInfo implements DataAdapterInfo {
    private final String name;
    private final String description;
    private final Class<? extends DataAdapter> adapterClass;
    private final URI jarUri;
    private final Class<? extends DataAdapterDialog> adapterDialog;

    /**
     * Initializes a new instance of the DataAdapterInfo class.
     *
     * @param name         the name of the data adapter.
     * @param description  the description associated to the data adapter.
     * @param adapterClass the class that implements the data adapter.
     * @param jarUri       the URI for the JAR that contains the adapter's class.
     * @param dialogClass  the class that implements the dialog box used to gather the adapter's parameters from the end user.
     */
    protected BaseDataAdapterInfo(String name, String description, Class<? extends DataAdapter> adapterClass, URI jarUri, Class<? extends DataAdapterDialog> dialogClass) {
        this.name = name;
        this.description = description;
        this.adapterClass = adapterClass;
        this.jarUri = jarUri;
        this.adapterDialog = dialogClass;
    }

    /**
     * Returns the name of the data adapter.
     *
     * @return the name of the data adapter.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description associated to the data adapter.
     *
     * @return the description associated to the data adapter.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the URI for the JAR that contains the adapter's class.
     *
     * @return the URI for the JAR that contains the adapter's class.
     */
    public URI getJarUri() {
        return jarUri;
    }

    /**
     * Returns the class that implements the data adapter.
     *
     * @return the class that implements the data adapter.
     */
    public Class<? extends DataAdapter> getAdapterClass() {
        return adapterClass;
    }

    /**
     * Returns a key to uniquely identify the adapter.
     *
     * @return a key to uniquely identify the adapter.
     */
    public String getKey() {
        return adapterClass.getName();
    }

    /**
     * Returns the class that implements the dialog box used to gather the adapter's parameters from the end user.
     *
     * @return the class that implements the dialog box used to gather the adapter's parameters from the end user.
     */
    public Class<? extends DataAdapterDialog> getAdapterDialog() {
        return adapterDialog;
    }
}
