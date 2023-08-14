/*
 * Copyright 2023 Frederic Thevenet
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

package eu.binjr.sources.jfr.adapters;

import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * This is a dummy DataAdapter. It's sole purpose is to act as an entry point for the actual adapters in the plugin,
 * alongside {@link JvmDataAdapterInfo}
 * It should not be instantiated with the intent to be used as an actual adapter.
 */
public class JvmDataAdapter extends BaseDataAdapter<Double> {

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public ZoneId getTimeZoneId() {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public String getSourceName() {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public Map<String, String> getParams() {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        throw new UnsupportedOperationException("This adapter should not be instantiated");
    }
}
