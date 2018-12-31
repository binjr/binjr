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
 */

package eu.binjr.sources.rrd4j.adapters;

import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TreeItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class Rrd4jFileAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = LogManager.getLogger(Rrd4jFileAdapter.class);
    private List<Path> rrdPaths;
    private final Map<Path, RrdDb> rrdDbMap = new HashMap<>();
    private List<Path> tempPathToCollect = new ArrayList<>();

    public Rrd4jFileAdapter() {
        this(new ArrayList<Path>());
    }

    public Rrd4jFileAdapter(List<Path> rrdPath) {
        this.rrdPaths = rrdPath;
    }

    @Override
    public TreeItem<TimeSeriesBinding<Double>> getBindingTree() throws DataAdapterException {
        TreeItem<TimeSeriesBinding<Double>> tree = new TreeItem<>(
                new TimeSeriesBinding<>(
                        "",
                        "/",
                        null,
                        getSourceName(),
                        UnitPrefixes.METRIC,
                        ChartType.STACKED,
                        "-",
                        "/" + getSourceName(), this));
        for (Path rrdPath : rrdPaths) {
            try {
                String rrdFileName = rrdPath.getFileName().toString();
                var rrdNode = new TreeItem<>(new TimeSeriesBinding<>(
                        rrdFileName,
                        rrdFileName,
                        null,
                        rrdFileName,
                        UnitPrefixes.METRIC,
                        ChartType.STACKED,
                        "-",
                        tree.getValue().getTreeHierarchy() + "/" + rrdFileName,
                        this));
                RrdDb rrd = openRrdDb(rrdPath);
                rrdDbMap.put(rrdPath, rrd);
                for (ConsolFun consolFun : Arrays.stream(rrd.getRrdDef().getArcDefs()).map(ArcDef::getConsolFun).collect(Collectors.toSet())) {
                    var consolFunNode = new TreeItem<>(new TimeSeriesBinding<>(
                            consolFun.toString(),
                            rrdPath.resolve(consolFun.toString()).toString(),
                            null,
                            consolFun.toString(),
                            UnitPrefixes.METRIC,
                            ChartType.STACKED,
                            "-",
                            rrdNode.getValue().getTreeHierarchy() + "/" + consolFun.toString(),
                            this));
                    rrdNode.getChildren().add(consolFunNode);
                    for (String ds : rrd.getDsNames()) {
                        consolFunNode.getChildren().add(new TreeItem<>(new TimeSeriesBinding<>(
                                ds,
                                consolFunNode.getValue().getPath(),
                                null,
                                ds,
                                UnitPrefixes.METRIC,
                                ChartType.STACKED,
                                "-",
                                consolFunNode.getValue().getTreeHierarchy() + "/" + ds,
                                this)));
                    }
                }
                tree.getChildren().add(rrdNode);
            } catch (IOException e) {
                throw new DataAdapterException("Failed to open rrd db", e);
            }
        }
        return tree;
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
            FetchRequest request = rrdDbMap.get(dsPath.getParent()).createFetchRequest(ConsolFun.valueOf(dsPath.getFileName().toString()), begin.getEpochSecond(), end.getEpochSecond());
            request.setFilter(seriesInfo.stream().map(s -> s.getBinding().getLabel()).toArray(String[]::new));
            FetchData data = request.fetchData();
            Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
            for (int i = 0; i < data.getRowCount(); i++) {
                ZonedDateTime timeStamp = Instant.ofEpochSecond(data.getTimestamps()[i]).atZone(getTimeZoneId());
                for (TimeSeriesInfo<Double> info : seriesInfo) {
                    Double val = data.getValues(info.getBinding().getLabel())[i];
                    XYChart.Data<ZonedDateTime, Double> point = new XYChart.Data<>(timeStamp, val.isNaN() ? 0 : val);
                    TimeSeriesProcessor<Double> seriesProcessor = series.computeIfAbsent(info, k -> new DoubleTimeSeriesProcessor());
                    seriesProcessor.addSample(point);
                }
            }
            logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)", seriesInfo.size(), data.getRowCount(), seriesInfo.size() * data.getRowCount()));
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
        return "[RRD] " + rrdPaths.get(0).getFileName() + (rrdPaths.size() > 1 ? " + " + (rrdPaths.size() - 1) + " more RRD file(s)" : "");
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
        if ("text/xml".equalsIgnoreCase(Files.probeContentType(rrdPath))) {
            logger.debug(() -> "Attempting to import as an rrd XML dump");
            Path temp = Files.createTempFile("binjr_", "_imported.rrd");
            tempPathToCollect.add(temp);
            return new RrdDb(temp.toUri(), RrdDb.PREFIX_XML + rrdPath.toString());
        }
        //TODO Peek into the rrd file's header to determine its producer rather than wait for an exception to be thrown.
        try {
            return new RrdDb(rrdPath.toUri());
        } catch (InvalidRrdException e) {
            // Possibly a rrd db created with RrdTool.
            // Try to convert and import.
            logger.debug(() -> "Failed to open " + rrdPath + " as an Rrd4j db: attempting to import as an rrdTool db");
            Path temp = Files.createTempFile("binjr_", "_imported.rrd");
            tempPathToCollect.add(temp);
            return new RrdDb(temp.toUri(), RrdDb.PREFIX_RRDTool + rrdPath.toString());
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
