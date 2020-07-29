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

package eu.binjr.core.data.adapters;

import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.common.preferences.ObservablePreferenceFactory;
import eu.binjr.common.version.Version;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Dialog;

/**
 * An immutable representation of a {@link SerializedDataAdapter}'s metadata
 *
 * @author Frederic Thevenet
 */
public interface DataAdapterInfo {

    /**
     * Returns the name of the data adapter.
     *
     * @return the name of the data adapter.
     */
    public String getName();

    /**
     * Returns the description associated to the data adapter.
     *
     * @return the description associated to the data adapter.
     */
    public String getDescription();

    /**
     * Returns the version of the adapter.
     *
     * @return the version of the adapter.
     */
    public Version getVersion();

    /**
     * Returns the url of the website associated with the adapter.
     *
     * @return the url of the website associated with the adapter.
     */
    public String getSiteUrl();

    /**
     * Returns the name of license under which the adapter is distributed.
     *
     * @return the name of license under which the adapter is distributed.
     */
    public String getLicense();

    /**
     * Returns the copyright notice associated with the adapter.
     *
     * @return the copyright notice associated with the adapter.
     */
    public String getCopyright();

    /**
     * Returns the location of the JAR in which the adapter is packaged.
     *
     * @return the location of the JAR in which the adapter is packaged.
     */
    public String getJarLocation();

    /**
     * Returns the class that implements the data adapter.
     *
     * @return the class that implements the data adapter.
     */
    public Class<? extends DataAdapter> getAdapterClass();

    /**
     * Returns a key to uniquely identify the adapter.
     *
     * @return a key to uniquely identify the adapter.
     */
    public String getKey();

    /**
     * Returns the class that implements the dialog box used to gather the adapter's parameters from the end user.
     *
     * @return the class that implements the dialog box used to gather the adapter's parameters from the end user.
     */
    public Class<? extends Dialog<DataAdapter>> getAdapterDialog();

    /**
     * The enabled property.
     *
     * @return The enabled property.
     */
    public BooleanProperty enabledProperty();

    /**
     * Returns true if the adapter is enabled, false otherwise.
     *
     * @return true if the adapter is enabled, false otherwise.
     */
    public boolean isEnabled();

    /**
     * Set to true to enable the adapter, false otherwise.
     *
     * @param enabled true to enable the adapter, false otherwise.
     */
    public void setEnabled(boolean enabled);

    /**
     * Returns the {@link ObservablePreference} instance associated with the adapter
     *
     * @return the {@link ObservablePreference} instance associated with the adapter
     */
    public ObservablePreferenceFactory getPreferences();

    default Version getApiLevel() {
        return Version.emptyVersion;
    }

    /**
     * Returns the source locality for the adapter.
     *
     * @return the source locality for the adapter.
     */
    default SourceLocality getSourceLocality() {
        return SourceLocality.UNKNOWN;
    }

}
