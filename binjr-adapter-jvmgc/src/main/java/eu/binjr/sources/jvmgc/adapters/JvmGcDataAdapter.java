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

import com.microsoft.gctoolkit.GCToolKit;
import com.microsoft.gctoolkit.io.GCLogFile;
import com.microsoft.gctoolkit.io.RotatingGCLogFile;
import com.microsoft.gctoolkit.io.SingleGCLogFile;
import com.microsoft.gctoolkit.jvm.JavaVirtualMachine;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.codec.csv.DataSample;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.*;
import eu.binjr.sources.jvmgc.adapters.aggregation.GcLogDataStore;
import javafx.scene.paint.Color;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local JVM GC log file.
 *
 * @author Frederic Thevenet
 */
public class JvmGcDataAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(JvmGcDataAdapter.class);
    public static final String GC_FILE_PATH = "gcFilePath";
    public static final String ZONE_ID = "zoneId";
    public static final String ENCODING = "encoding";
    public static final String ROLLING_LOGS = "RollingLogs";
    private final JvmGcAdapterPreferences adapterPrefs = (JvmGcAdapterPreferences) getAdapterInfo().getPreferences();

    private boolean detectRollingLogs;
    private Path gcLogPath;
    private ZoneId zoneId;
    private String encoding;
    private Map<String, GcLogDataStore.AggregationInfo> sortedDataStores;


    /**
     * Initializes a new instance of the {@link JvmGcDataAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public JvmGcDataAdapter() throws DataAdapterException {
        this(Path.of(""), ZoneId.systemDefault(), true);
    }

    /**
     * Initializes a new instance of the {@link JvmGcDataAdapter} class for the provided file and time zone.
     *
     * @param gcLogPath the path to the csv file.
     * @param zoneId    the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JvmGcDataAdapter(Path gcLogPath, ZoneId zoneId, boolean detectRollingLogs) throws DataAdapterException {
        this(gcLogPath, zoneId, "utf-8", detectRollingLogs);
    }

    /**
     * Initializes a new instance of the {@link JvmGcDataAdapter} class with the provided parameters.
     *
     * @param gcLogPath the path to the csv file.
     * @param zoneId    the time zone to used.
     * @param encoding  the encoding for the csv file.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JvmGcDataAdapter(Path gcLogPath, ZoneId zoneId, String encoding, boolean detectRollingLogs) throws DataAdapterException {
        super();
        this.gcLogPath = gcLogPath;
        this.zoneId = zoneId;
        this.encoding = encoding;
        this.detectRollingLogs = detectRollingLogs;
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {

        ConcurrentNavigableMap<Long, DataSample> dataStore = new ConcurrentSkipListMap<>();
        try (Profiler ignored = Profiler.start("Building seekable datastore for GC log file", logger::perf)) {
            GCLogFile logFile = detectRollingLogs ? new RotatingGCLogFile(gcLogPath) : new SingleGCLogFile(gcLogPath);
            GCToolKit gcToolKit = new GCToolKit();
            var gcDataStore = new GcLogDataStore();
            gcToolKit.loadAggregation(gcDataStore);

            JavaVirtualMachine machine = gcToolKit.analyze(logFile);

            gcDataStore.computeAllocationStats();

            this.sortedDataStores = gcDataStore.get();

            FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                    new TimeSeriesBinding.Builder()
                            .withLabel(getSourceName())
                            .withPath("/")
                            .withAdapter(this)
                            .build());

            var poolDict = new HashMap<String, FilterableTreeItem<SourceBinding>>();

            sortedDataStores.forEach((s, m) -> {
                FilterableTreeItem<SourceBinding> node = tree;
                for (var category : m.category()) {
                    var nodecopy = node;
                    node = poolDict.computeIfAbsent(category, k -> attachNode(k, nodecopy.getValue().getLabel(), k, m.unit(), m.prefix(), m.chartType(), m.color(), nodecopy));
                }
                attachNode(m.name(), m.name(), m.label(), m.unit(), m.prefix(), m.chartType(), m.color(), node);

            });


            return tree;
        } catch (IOException e) {
            throw new DataAdapterException(e);
        }
    }

    private String getGCName(JavaVirtualMachine machine) {
        if (machine.isCMS()) {
            return "CMS";
        }
        if (machine.isG1GC()) {
            return "G1";
        }
        if (machine.isParallel()) {
            return "Parallel";
        }
        if (machine.isSerial()) {
            return "Serial";
        }
        if (machine.isShenandoah()) {
            return "Shenandoah";
        }
        if (machine.isZGC()) {
            return "ZGC";
        }
        return "Unknown";
    }

    private FilterableTreeItem<SourceBinding> attachNode(String label,
                                                         String path,
                                                         String legend,
                                                         String unit,
                                                         UnitPrefixes prefix,
                                                         ChartType chartType,
                                                         Color color,
                                                         FilterableTreeItem<SourceBinding> parent) {
        SourceBinding binding = new TimeSeriesBinding.Builder()
                .withLabel(label)
                .withPath(path)
                .withLegend(legend)
                .withPrefix(prefix)
                .withUnitName(unit)
                .withGraphType(chartType)
                .withColor(color)
                .withParent(parent.getValue())
                .withAdapter(this)
                .build();
        var node = new FilterableTreeItem<>(binding);
        parent.getInternalChildren().add(node);
        return node;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        var store = getDataStore(path);
        return TimeRange.of(store.get(store.firstKey()).timestamp(), store.get(store.lastKey()).timestamp());
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        if (this.isClosed()) {
            throw new IllegalStateException("An attempt was made to fetch data from a closed adapter");
        }
        var store = getDataStore(path);
        Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
        Map<String, List<TimeSeriesInfo<Double>>> rDict = new HashMap<>();
        for (TimeSeriesInfo<Double> info : seriesInfo) {
            rDict.computeIfAbsent(info.getBinding().getLabel(), s -> new ArrayList<>()).add(info);
            series.put(info, new DoubleTimeSeriesProcessor());
            Long fromKey = Objects.requireNonNullElse(store.floorKey(begin.toEpochMilli()), begin.toEpochMilli());
            Long toKey = Objects.requireNonNullElse(store.ceilingKey(end.toEpochMilli()), end.toEpochMilli());
            for (var sample : store.subMap(fromKey, true, toKey, true).values()) {
                series.get(info).addSample(sample.timestamp(), sample.value());
            }
        }
        return series;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return zoneId;
    }

    @Override
    public String getSourceName() {
        return "[GC Logs] " + (gcLogPath != null ? gcLogPath.getFileName() : "???" + (detectRollingLogs ? "(rolling)" : ""));
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(GC_FILE_PATH, gcLogPath.toString());
        params.put(ROLLING_LOGS, Boolean.toString(detectRollingLogs));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params, LoadingContext context) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        this.zoneId = mapParameter(params, ZONE_ID, ZoneId::of);
        this.gcLogPath = mapParameter(params, GC_FILE_PATH, Path::of);
        this.encoding = mapParameter(params, ENCODING);
        this.detectRollingLogs =  mapParameter(params,ROLLING_LOGS, Boolean::valueOf);
        Path workspaceRootPath = context.savedWorkspacePath() != null ? context.savedWorkspacePath().getParent() : this.gcLogPath.getRoot();
        if (workspaceRootPath != null) {
            this.gcLogPath = workspaceRootPath.resolve(gcLogPath);
        }
    }

    @Override
    public void close() {
        sortedDataStores.clear();
        super.close();
    }

    private ConcurrentNavigableMap<Long, GcLogDataStore.TsSample> getDataStore(String path) throws DataAdapterException {
        var storeKey = path.split("/")[0];
        return sortedDataStores.get(storeKey).data();
    }

}
