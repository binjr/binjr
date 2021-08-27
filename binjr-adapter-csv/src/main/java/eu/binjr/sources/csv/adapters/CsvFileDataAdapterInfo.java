/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;

import eu.binjr.core.data.adapters.AdapterMetadata;
import eu.binjr.core.data.adapters.BaseDataAdapterInfo;
import eu.binjr.core.data.adapters.SourceLocality;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;

/**
 * Defines the metadata associated with the CsvFileDataAdapter.
 *
 * @author Frederic Thevenet
 */
@AdapterMetadata(
        name = "CSV",
        description = "CSV File Data Adapter",
        copyright = AppEnvironment.COPYRIGHT_NOTICE,
        license = AppEnvironment.LICENSE,
        siteUrl = AppEnvironment.HTTP_WWW_BINJR_EU,
        adapterClass = CsvFileAdapter.class,
        dialogClass = CsvFileAdapterDialog.class,
        sourceLocality = SourceLocality.LOCAL,
        apiLevel = AppEnvironment.PLUGIN_API_LEVEL
)
public class CsvFileDataAdapterInfo extends BaseDataAdapterInfo {

    /**
     * Initialises a new instance of the {@link CsvFileDataAdapterInfo} class.
     * @throws CannotInitializeDataAdapterException if the adapter's initialization failed
     */
    public CsvFileDataAdapterInfo() throws CannotInitializeDataAdapterException {
        super(CsvFileDataAdapterInfo.class);
    }
}
