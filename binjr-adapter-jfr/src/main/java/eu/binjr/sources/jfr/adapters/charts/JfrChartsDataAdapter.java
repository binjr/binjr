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

package eu.binjr.sources.jfr.adapters.charts;


import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.ReloadPolicy;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.UnitPrefixes;
import eu.binjr.sources.jfr.adapters.BaseJfrDataAdapter;
import eu.binjr.sources.jfr.adapters.JfrEventFormat;
import jdk.jfr.*;
import jdk.jfr.consumer.RecordingFile;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link DataAdapter} implementation to retrieve data from a JDK Flight Recorder file.
 *
 * @author Frederic Thevenet
 */
public class JfrChartsDataAdapter extends BaseJfrDataAdapter<Double> {
    private static final Logger logger = Logger.create(JfrChartsDataAdapter.class);
    public static final int RECURSE_MAX_DEPTH = 10;
    public static final String GCREF_WEAK_REFERENCE = "Weak reference";


    /**
     * Initializes a new instance of the {@link JfrChartsDataAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public JfrChartsDataAdapter() throws DataAdapterException {
        super(Path.of(""), ZoneId.systemDefault());
    }


    /**
     * Initializes a new instance of the {@link JfrChartsDataAdapter} class with the provided parameters.
     *
     * @param jfrPath the path to the JFR file.
     * @param zoneId  the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JfrChartsDataAdapter(Path jfrPath,
                                ZoneId zoneId)
            throws DataAdapterException {
        super(jfrPath, zoneId);
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(info -> info.getBinding().getPath()).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(ts -> ts.getBinding().getPath()).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(new TimeSeriesBinding.Builder()
                .withLabel(getSourceName())
                .withPath(BuiltInParsingProfile.NONE.getProfileId() + "/" + jfrFilePath.toString() + "|")
                .withAdapter(this)
                .build());
        try (var recordingFile = new RecordingFile(jfrFilePath)) {
            ensureIndexed(Set.of(tree.getValue().getPath()), ReloadPolicy.UNLOADED);
            var indexedEventTypes = index.getPaths(2, JfrEventFormat.HAS_NUM_FIELDS + ":true");
            for (EventType eventType : recordingFile.readEventTypes()) {
                String eventKey = BuiltInParsingProfile.NONE.getProfileId() + "/" +
                        jfrFilePath.toString() + "|/" +
                        String.join("/", eventType.getCategoryNames()) + "/" +
                        eventType.getLabel();
                var facetEntry = indexedEventTypes.get(eventKey);
                if (facetEntry != null) {
                    var branch = tree;
                    for (var cat : eventType.getCategoryNames()) {
                        var pos = TreeViewUtils.findFirstInTree(tree, item -> item.getValue().getLabel().equals(cat));
                        if (pos.isEmpty()) {
                            var node = new FilterableTreeItem<>((SourceBinding) new TimeSeriesBinding.Builder()
                                    .withLabel(cat)
                                    .withParent(branch.getValue())
                                    .withPath(branch.getValue().getPath() + "/" + cat)
                                    .withAdapter(this)
                                    .build());
                            branch.getInternalChildren().add(node);
                            branch = node;
                        } else {
                            branch = (FilterableTreeItem<SourceBinding>) pos.get();
                        }
                    }
                    var leaf = new FilterableTreeItem<>((SourceBinding) new TimeSeriesBinding.Builder()
                            .withLabel(eventType.getLabel())
                            .withLegend(eventType.getLabel())
                            .withPath(branch.getValue().getPath() + "/" + eventType.getLabel())
                            .withParent(branch.getValue())
                            .withGraphType(ChartType.LINE)
                            .withAdapter(this)
                            .build());
                    branch.getInternalChildren().add(leaf);
                    switch (eventType.getName()) {
                        case JfrEventFormat.JDK_GCREFERENCE_STATISTICS -> {
                            addField(JfrEventFormat.GCREF_FINAL_REFERENCE, eventType.getField(JfrEventFormat.GCREF_COUNT_FIELD), leaf, 1);
                            addField(JfrEventFormat.GCREF_SOFT_REFERENCE, eventType.getField(JfrEventFormat.GCREF_COUNT_FIELD), leaf, 1);
                            addField(GCREF_WEAK_REFERENCE, eventType.getField(JfrEventFormat.GCREF_COUNT_FIELD), leaf, 1);
                            addField(JfrEventFormat.GCREF_PHANTOM_REFERENCE, eventType.getField(JfrEventFormat.GCREF_COUNT_FIELD), leaf, 1);
                        }
                        default -> {
                            for (var field : eventType.getFields()) {
                                addField("", field, leaf, 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DataAdapterException("Error while attempting to read JFR recording: " + e.getMessage(), e);
        }
        return tree;
    }

    private void addField(String parentName, ValueDescriptor field, FilterableTreeItem<SourceBinding> leaf, int depth) {
        if (JfrEventFormat.includeField(field)) {
            var unit = extractKnownUnit(field);
            leaf.getInternalChildren().add(new FilterableTreeItem<>(new TimeSeriesBinding.Builder()
                    .withLabel(String.join(" ", parentName, field.getLabel()).trim())
                    .withPath(leaf.getValue().getPath())
                    .withParent(leaf.getValue())
                    .withUnitName(unit.name())
                    .withPrefix(unit.prefix())
                    .withGraphType(ChartType.LINE)
                    .withAdapter(this)
                    .build()));
        }
        for (var nestedField : field.getFields()) {
            if (!field.getTypeName().equals(JfrEventFormat.JDK_TYPES_THREAD_GROUP) &&
                    !field.getTypeName().equals(JfrEventFormat.JDK_TYPES_STACK_TRACE) &&
                    depth <= RECURSE_MAX_DEPTH) {
                addField(field.getLabel(), nestedField, leaf, depth + 1);
            }
        }
    }


    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path,
                                                                              Instant begin,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<Double>> seriesInfo,
                                                                              boolean bypassCache) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(info -> info.getBinding().getPath()).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
            for (TimeSeriesInfo<Double> info : seriesInfo) {
                series.put(info, new DoubleTimeSeriesProcessor());
            }
            var nbHits = index.search(
                    begin.toEpochMilli(),
                    end.toEpochMilli(),
                    series,
                    getTimeZoneId(),
                    bypassCache);
            logger.debug(() -> "Retrieved " + nbHits + " hits");
            return series;
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching data from " + path, e);
        }
    }


    @Override
    public String getSourceName() {
        return new StringBuilder("[JFR: Charts] ")
                .append(jfrFilePath != null ? jfrFilePath.getFileName() : "???")
                .toString();
    }

    private Unit extractKnownUnit(ValueDescriptor field) {
        var timespan = field.getAnnotation(Timespan.class);
        if (timespan != null) {
            return new Unit(timespan.value(), UnitPrefixes.METRIC);
        }

        var frequency = field.getAnnotation(Frequency.class);
        if (frequency != null) {
            return new Unit("Hertz", UnitPrefixes.METRIC);
        }

        var dataAmount = field.getAnnotation(DataAmount.class);
        if (dataAmount != null) {
            return new Unit(dataAmount.value(), UnitPrefixes.BINARY);
        }

        var percentage = field.getAnnotation(Percentage.class);
        if (percentage != null) {
            return new Unit("%", UnitPrefixes.NONE);
        }

        return new Unit("-", UnitPrefixes.NONE);
    }

    private record Unit(String name, UnitPrefixes prefix) {
    }

}
