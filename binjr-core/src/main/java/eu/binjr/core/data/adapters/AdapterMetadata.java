/*
 *    Copyright 2020-2024 Frederic Thevenet
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

import javafx.scene.control.Dialog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
/**
 * Defines the metadata used to initialise a class that implements {@link DataAdapterInfo}
 */
public @interface AdapterMetadata {
    /**
     * @return the name of the data adapter.
     */
    String name();

    /**
     * @return the category if the data adapter.
     */
    String category() default "";

    /**
     * @return the description associated to the data adapter.
     */
    String description();

    /**
     * @return the version information related to the data adapter
     */
    String version() default "0.0.0";

    /**
     * @return the copyright information related to the data adapter
     */
    String copyright();

    /**
     * @return the license information related to the data adapter
     */
    String license();

    /**
     * @return the address of the site for this adapter
     */
    String siteUrl();

    /**
     * @return the class that implements the data adapter
     */
    Class<? extends DataAdapter<?>> adapterClass();

    /**
     * @return the class that implements the dialog box used to gather the adapter's parameters from the end user.
     */
    Class<? extends Dialog<Collection<DataAdapter>>> dialogClass();

    /**
     * @return the API level implemented by the adapter
     */
    String apiLevel() default "0.0.0";

    /**
     * @return the class that implements the preferences for this adapter
     */
    Class<? extends DataAdapterPreferences> preferencesClass() default DataAdapterPreferences.class;

    /**
     * @return Indicates whether the source is local or remote
     */
    SourceLocality sourceLocality() default SourceLocality.UNKNOWN;

    /**
     * @return Indicates the visualization type for the data provided by this adapter
     */
    VisualizationType visualizationType() default VisualizationType.UNKNOWN;
}
