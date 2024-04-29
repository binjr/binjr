/*
 * Copyright 2024 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.sources.jvmgc.adapters;


import eu.binjr.core.data.adapters.AdapterMetadata;
import eu.binjr.core.data.adapters.BaseDataAdapterInfo;
import eu.binjr.core.data.adapters.SourceLocality;
import eu.binjr.core.data.adapters.VisualizationType;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;


/**
 * Defines the metadata associated with JfrDataAdapter.
 *
 * @author Frederic Thevenet
 */
@AdapterMetadata(
        name = "GC Logs",
        category = "JVM",
        description = "JVM Garbage Collection Logs Data Adapter",
        copyright = AppEnvironment.COPYRIGHT_NOTICE,
        license = AppEnvironment.LICENSE,
        siteUrl = AppEnvironment.HTTP_WWW_BINJR_EU,
        adapterClass = JvmGcDataAdapter.class,
        dialogClass = JvmGcDataAdapterDialog.class,
        sourceLocality = SourceLocality.LOCAL,
        apiLevel = AppEnvironment.PLUGIN_API_LEVEL,
        preferencesClass = JvmGcAdapterPreferences.class,
        visualizationType = VisualizationType.CHARTS
)
public class JvmGcDataAdapterInfo extends BaseDataAdapterInfo {

    /**
     * Initialises a new instance of the {@link JvmGcDataAdapterInfo} class.
     *
     * @throws CannotInitializeDataAdapterException if the adapter's initialization failed
     */
    public JvmGcDataAdapterInfo() throws CannotInitializeDataAdapterException {
        super(JvmGcDataAdapterInfo.class);
    }

}
