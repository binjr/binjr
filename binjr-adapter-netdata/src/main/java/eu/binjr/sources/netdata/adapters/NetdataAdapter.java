/*
 *    Copyright 2020-2021 Frederic Thevenet
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

package eu.binjr.sources.netdata.adapters;

import com.google.gson.Gson;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.codec.Decoder;
import eu.binjr.core.data.codec.csv.CsvDecoder;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.preferences.UserPreferences;
import eu.binjr.sources.netdata.api.Chart;
import eu.binjr.sources.netdata.api.ChartSummary;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation capable of consuming data from the
 * Netdata (https://netdata.cloud) API.
 *
 * @author Frederic Thevenet
 */
public class NetdataAdapter extends HttpDataAdapter<Double> {
    private static final Logger logger = Logger.create(NetdataAdapter.class);
    private static final char DELIMITER = ',';
    private final Gson jsonParser;
    private final ZoneId zoneId;
    private final Decoder<Double> decoder;
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final NetdataAdapterPreferences adapterPrefs = (NetdataAdapterPreferences) getAdapterInfo().getPreferences();

    /**
     * Initialises a new instance of the {@link NetdataAdapter} class.
     *
     * @throws CannotInitializeDataAdapterException if an error occurs while initializing the adapter.
     */
    public NetdataAdapter() throws CannotInitializeDataAdapterException {
        this(null, ZoneId.systemDefault());
    }

    private NetdataAdapter(URL baseAddress, ZoneId zoneId) throws CannotInitializeDataAdapterException {
        super(baseAddress);
        this.zoneId = zoneId;
        this.decoder = buildDecoder(zoneId);
        jsonParser = new Gson();
    }

    /**
     * Returns a new instance of {@link NetdataAdapter} for the provided address and time zone.
     *
     * @param address the address of a Netdata server.
     * @param zoneId  the desired time zone.
     * @return a new instance of {@link NetdataAdapter} for the provided address and time zone.
     * @throws CannotInitializeDataAdapterException if an error occurs while initializing the adapter.
     */
    public static DataAdapter<Double> fromUrl(String address, ZoneId zoneId) throws CannotInitializeDataAdapterException {
        return new NetdataAdapter(urlFromString(address), zoneId);
    }

    @Override
    protected URI craftFetchUri(String path, Instant begin, Instant end) throws DataAdapterException {
        var params = new ArrayList<NameValuePair>();
        params.add(UriParameter.of("points",
                (userPrefs.downSamplingEnabled.get() && !adapterPrefs.disableServerSideDownsampling.get()
                        ? userPrefs.downSamplingThreshold.get() : adapterPrefs.maxSamplesAllowed.get())));
        params.add(UriParameter.of("group", adapterPrefs.groupingMethod.get()));
        params.add(UriParameter.of("gtime", adapterPrefs.groupingTime.get()));
        if (adapterPrefs.disableTimeFrameAlignment.get()) {
            params.add(UriParameter.of("options", "unaligned"));
        }
        params.add(UriParameter.of("format", "csv"));
        params.add(UriParameter.of("options", "seconds"));
        params.add(UriParameter.of("after", begin.getEpochSecond() - adapterPrefs.fetchReadBehindSeconds.get().intValue()));
        params.add(UriParameter.of("before", end.getEpochSecond() + adapterPrefs.fetchReadAheadSeconds.get().intValue()));

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
        var chartSummary = doHttpGet(
                craftRequestUri(ChartSummary.ENDPOINT),
                response -> jsonParser.fromJson(EntityUtils.toString(response.getEntity()), ChartSummary.class)
        );
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withAdapter(this)
                        .withLabel(getSourceName())
                        .withPath("/")
                        .build());
        Map<String, FilterableTreeItem<SourceBinding>> types = new TreeMap<>();
        chartSummary.getCharts().forEach((s, chart) -> {
            var categoryName = getCategoryName(chart);
            var categoryBranch = types.computeIfAbsent(categoryName, s1 -> new FilterableTreeItem<>(
                    new TimeSeriesBinding.Builder()
                            .withAdapter(this)
                            .withPath("")
                            .withLabel(categoryName)
                            .withParent(tree.getValue())
                            .build()));
            var branch = new FilterableTreeItem<SourceBinding>(
                    new TimeSeriesBinding.Builder()
                            .withAdapter(this)
                            .withPath(chart.getDataUrl())
                            .withLabel(chart.getName())
                            .withGraphType(ChartType.valueOrDefault(chart.getChartType().name(), ChartType.STACKED))
                            .withLegend(chart.getTitle())
                            .withUnitName(chart.getUnits())
                            .withParent(categoryBranch.getValue())
                            .build());
            chart.getDimensions().forEach((s1, chartDimensions) -> {
                branch.getInternalChildren().add(0, new FilterableTreeItem<>(
                        new TimeSeriesBinding.Builder()
                                .withLabel(chartDimensions.getName())
                                .withAdapter(this)
                                .withParent(branch.getValue())
                                .withUnitName(chart.getUnits())
                                .withGraphType(ChartType.valueOrDefault(chart.getChartType().name(), ChartType.STACKED))
                                .withPath(chart.getDataUrl())
                                .build()));
            });
            categoryBranch.getInternalChildren().add(branch);
        });
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
        Chart chart = doHttpGet(craftRequestUri(path.replace("/data?", "/chart?")), response ->
                jsonParser.fromJson(EntityUtils.toString(response.getEntity()), Chart.class)
        );
        return TimeRange.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(chart.getFirstEntry().longValue()), zoneId),
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(chart.getLastEntry().longValue()), zoneId));
    }

    @Override
    public String getSourceName() {
        return "[Netdata] " +
                (getBaseAddress() != null ? getBaseAddress().getHost() : "???") +
                ((getBaseAddress() != null && getBaseAddress().getPort() > 0) ? ":" + getBaseAddress().getPort() : "") +
                " - " +
                " (" +
                (zoneId != null ? zoneId : "???") +
                ")";
    }

    private CsvDecoder buildDecoder(ZoneId zoneId) {
        return new CsvDecoder(getEncoding(), DELIMITER,
                DoubleTimeSeriesProcessor::new,
                s -> Instant.ofEpochSecond(Long.parseLong(s)).atZone(zoneId));
    }

    private String getCategoryName(Chart chart) {
        var categoryName = chart.getType().split("_")[0];
        return categoryName.isBlank() ?
                "Unknown" : categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
    }
}
