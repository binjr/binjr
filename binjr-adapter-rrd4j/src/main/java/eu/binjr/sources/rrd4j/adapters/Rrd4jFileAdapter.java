/*
 *    Copyright 2018-2022 Frederic Thevenet
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

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TreeItem;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation capable of consuming data
 * from Round Robin Database files.
 *
 * @author Frederic Thevenet
 */
public class Rrd4jFileAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(Rrd4jFileAdapter.class);
    private final Rrd4jFileAdapterPreferences prefs = (Rrd4jFileAdapterPreferences) this.getAdapterInfo().getPreferences();
    private final Map<Path, RrdDb> rrdDbMap = new HashMap<>();
    private List<Path> rrdPaths;
    private final List<Path> tempPathToCollect = new ArrayList<>();

    /**
     * Initialises a new instance of the {@link Rrd4jFileAdapter} class.
     */
    public Rrd4jFileAdapter() {
        this(new ArrayList<Path>());
    }

    /**
     * Initialises a new instance of the {@link Rrd4jFileAdapter} class from the provided list of {@link Path}
     *
     * @param rrdPath a list of {@link Path} to be mounted by the adapter.
     */
    public Rrd4jFileAdapter(List<Path> rrdPath) {
        this.rrdPaths = rrdPath;
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        for (Path rrdPath : rrdPaths) {
            try {
                String rrdFileName = rrdPath.getFileName().toString();
                FilterableTreeItem<SourceBinding> rrdNode = new FilterableTreeItem<>(
                        new TimeSeriesBinding.Builder()
                                .withLabel(rrdFileName)
                                .withPath(rrdFileName)
                                .withParent(tree.getValue())
                                .withAdapter(this)
                                .build());
                RrdDb rrd = openRrdDb(rrdPath);
                rrdDbMap.put(rrdPath, rrd);
                for (ConsolFun consolFun : Arrays.stream(rrd.getRrdDef().getArcDefs())
                        .map(ArcDef::getConsolFun)
                        .collect(Collectors.toSet())) {
                    FilterableTreeItem<SourceBinding> consolFunNode = new FilterableTreeItem<>(new TimeSeriesBinding.Builder()
                            .withLabel(consolFun.toString())
                            .withPath(rrdPath.resolve(consolFun.toString()).toString())
                            .withParent(rrdNode.getValue())
                            .withAdapter(this)
                            .build());
                    rrdNode.getInternalChildren().add(consolFunNode);
                    for (String ds : rrd.getDsNames()) {
                        consolFunNode.getInternalChildren().add(new TreeItem<>(new TimeSeriesBinding.Builder()
                                .withLabel(ds)
                                .withPath(consolFunNode.getValue().getPath())
                                .withParent(consolFunNode.getValue())
                                .withAdapter(this)
                                .build()));
                    }
                }
                tree.getInternalChildren().add(rrdNode);
            } catch (IOException e) {
                throw new DataAdapterException("Failed to open rrd db", e);
            }
        }
        return tree;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        Path dsPath = Path.of(path);
        try {
            var end = Instant.ofEpochSecond(rrdDbMap.get(dsPath.getParent()).getLastArchiveUpdateTime()).atZone(getTimeZoneId());
            return TimeRange.of(end.minusHours(24), end);
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException("IO Error while retrieving last update from rrd db", e);
        }
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path, Instant begin, Instant
            end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache)
            throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        Path dsPath = Path.of(path);
        try {
            FetchRequest request = rrdDbMap.get(dsPath.getParent()).createFetchRequest(
                    ConsolFun.valueOf(dsPath.getFileName().toString()),
                    begin.getEpochSecond(),
                    end.getEpochSecond());
            request.setFilter(seriesInfo.stream().map(s -> s.getBinding().getLabel()).toArray(String[]::new));
            FetchData data = request.fetchData();
            Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
            for (int i = 0; i < data.getRowCount(); i++) {
                ZonedDateTime timeStamp = Instant.ofEpochSecond(data.getTimestamps()[i]).atZone(getTimeZoneId());
                for (TimeSeriesInfo<Double> info : seriesInfo) {
                    Double val = data.getValues(info.getBinding().getLabel())[i];
                    XYChart.Data<ZonedDateTime, Double> point = new XYChart.Data<>(timeStamp, val);
                    TimeSeriesProcessor<Double> seriesProcessor =
                            series.computeIfAbsent(info, k -> new DoubleTimeSeriesProcessor());
                    seriesProcessor.addSample(point);
                }
            }
            logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)",
                    seriesInfo.size(),
                    data.getRowCount(),
                    seriesInfo.size() * data.getRowCount()));
            return series;
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException("IO Error while retrieving data from rrd db", e);
        }
    }

    @Override
    public String getEncoding() {
        return "UTF-8";
    }

    @Override
    public ZoneId getTimeZoneId() {
        return ZoneId.systemDefault();
    }

    @Override
    public String getSourceName() {
        return "[RRD] " + rrdPaths.get(0).getFileName() + (rrdPaths.size() > 1 ? " + " + (rrdPaths.size() - 1) +
                " more RRD file(s)" : "");
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        int i = 0;
        for (Path rrdPath : rrdPaths) {
            params.put("rrdPaths_" + i++, rrdPath.toString());
        }
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        this.rrdPaths = params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("rrdPaths_"))
                .map(e -> Paths.get(e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        closeRrdDb();
        cleanTempFiles();
        super.close();
    }

    private RrdDb openRrdDb(Path rrdPath) throws IOException {
        var factory  = RrdBackendFactory.getFactory(prefs.rrd4jBackend.get().toString());
        logger.debug(()-> "Opening rrd file using backend factory= " + factory.getName());
        if ("text/xml".equalsIgnoreCase(Files.probeContentType(rrdPath))) {
            logger.debug(() -> "Attempting to import as an rrd XML dump");
            Path temp = Files.createTempFile(UserPreferences.getInstance().temporaryFilesRoot.get(), "binjr_", "_imported.rrd");
            tempPathToCollect.add(temp);
            return RrdDb.getBuilder()
                    .setBackendFactory(factory)
                    .setPath(temp.toUri())
                    .setReadOnly(true)
                    .setExternalPath(RrdDb.PREFIX_XML + rrdPath)
                    .build();
        }
        try {
            return RrdDb.getBuilder()
                    .setBackendFactory(factory)
                    .setPath(rrdPath.toUri())
                    .setReadOnly(true)
                    .build();
        } catch (InvalidRrdException e) {
            // Possibly a rrd db created with RrdTool.
            // Try to convert and import.
            logger.debug(() -> "Failed to open " + rrdPath + " as an Rrd4j db: attempting to import as an rrdTool db");
            Path temp = Files.createTempFile(UserPreferences.getInstance().temporaryFilesRoot.get(), "binjr_", "_imported.rrd");
            tempPathToCollect.add(temp);
            return RrdDb.getBuilder()
                    .setBackendFactory(factory)
                    .setPath(temp.toUri())
                    .setReadOnly(true)
                    .setExternalPath(RrdDb.PREFIX_RRDTool + rrdPath)
                    .build();
        }
    }

    private void closeRrdDb() {
        rrdDbMap.forEach((s, rrdDb) -> {
            logger.debug(() -> "Closing RRD db " + s);
            try {
                rrdDb.close();
            } catch (IOException e) {
                logger.error("Error attempting to close RRD db " + s, e);
            }
        });
        rrdDbMap.clear();
    }

    private void cleanTempFiles() {
        //Cleaning up temp files used to import rrdtool files
        tempPathToCollect.forEach(p -> {
            logger.debug(() -> "Deleting temp file " + p);
            try {
                Files.delete(p);
            } catch (IOException e) {
                logger.error("Failed to delete temp file", e);
            }
        });
        tempPathToCollect.clear();
    }
}
