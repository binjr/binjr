/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.sources.prometheus.adapters;

import com.google.gson.Gson;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.HttpDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.codec.Decoder;
import eu.binjr.core.data.codec.csv.CsvDecoder;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.preferences.UserPreferences;
import org.apache.hc.core5.http.NameValuePair;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link DataAdapter} implementation capable of consuming data from the
 * Prometheus (https://prometheus.io) API.
 *
 * @author Frederic Thevenet
 */
public class PrometheusAdapter extends HttpDataAdapter<Double> {
    private static final Logger logger = Logger.create(PrometheusAdapter.class);
    private final Gson jsonParser;
    private final ZoneId zoneId;
    private final Decoder<Double> decoder;
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final PrometheusAdapterPreferences adapterPrefs = (PrometheusAdapterPreferences) getAdapterInfo().getPreferences();

    /**
     * Initialises a new instance of the {@link PrometheusAdapter} class.
     *
     * @throws CannotInitializeDataAdapterException if an error occurs while initializing the adapter.
     */
    public PrometheusAdapter() throws CannotInitializeDataAdapterException {
        this(null, ZoneId.systemDefault());
    }

    private PrometheusAdapter(URL baseAddress, ZoneId zoneId) throws CannotInitializeDataAdapterException {
        super(baseAddress);
        this.zoneId = zoneId;
        this.decoder = buildDecoder(zoneId);
        jsonParser = new Gson();
    }

    /**
     * Returns a new instance of {@link PrometheusAdapter} for the provided address and time zone.
     *
     * @param address the address of a Prometheus server.
     * @param zoneId  the desired time zone.
     * @return a new instance of {@link PrometheusAdapter} for the provided address and time zone.
     * @throws CannotInitializeDataAdapterException if an error occurs while initializing the adapter.
     */
    public static DataAdapter<Double> fromUrl(String address, ZoneId zoneId) throws CannotInitializeDataAdapterException {
        return new PrometheusAdapter(urlFromString(address), zoneId);
    }

    @Override
    protected URI craftFetchUri(String path, Instant begin, Instant end) throws DataAdapterException {
        var params = new ArrayList<NameValuePair>();
        return craftRequestUri(path, params);
    }

    @Override
    public boolean isSortingRequired() {
        return true;
    }

    @Override
    public Decoder<Double> getDecoder() {
        return this.decoder;
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withAdapter(this)
                        .withLabel(getSourceName())
                        .withPath("/")
                        .build());
        Map<String, FilterableTreeItem<SourceBinding>> types = new TreeMap<>();
        tree.getInternalChildren().addAll(types.values());
        return tree;
    }

    @Override
    public String getEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        return null;
    }

    @Override
    public String getSourceName() {
        return "[Prometheus] " +
                (getBaseAddress() != null ? getBaseAddress().getHost() : "???") +
                ((getBaseAddress() != null && getBaseAddress().getPort() > 0) ? ":" + getBaseAddress().getPort() : "") +
                " - " +
                " (" +
                (zoneId != null ? zoneId : "???") +
                ")";
    }

    private CsvDecoder buildDecoder(ZoneId zoneId) {
        return new CsvDecoder(getEncoding(), PrometheusAdapterPreferences.DELIMITER,
                DoubleTimeSeriesProcessor::new,
                s -> Instant.ofEpochSecond(Long.parseLong(s)).atZone(zoneId));
    }

}
