/*
 *    Copyright 2018-2019 Frederic Thevenet
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

package eu.binjr.sources.rrd4j.adapters;

import eu.binjr.core.data.adapters.BaseDataAdapterInfo;
import eu.binjr.core.preferences.AppEnvironment;

/**
 * Defines the metadata associated with the Rrd4jFileDataAdapter
 *
 * @author Frederic Thevenet
 */
public class Rrd4jFileDataAdapterInfo extends BaseDataAdapterInfo {

    /**
     * Initialises a new instance of the {@link Rrd4jFileDataAdapterInfo} class.
     */
    public Rrd4jFileDataAdapterInfo() {
        super("RRD Files",
                "RRD Data Adapter",
                AppEnvironment.COPYRIGHT_NOTICE,
                AppEnvironment.LICENSE,
                AppEnvironment.HTTP_WWW_BINJR_EU,
                Rrd4jFileAdapter.class,
                Rrd4jFileAdapterDialog.class);
    }
}
