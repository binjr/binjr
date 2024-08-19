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


import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordingFile;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link DataAdapter} implementation to retrieve data from a JDK Flight Recorder file.
 *
 * @author Frederic Thevenet
 */
public class JfrDataAdapter extends BaseJfrDataAdapter<SearchHit> implements Reloadable<SearchHit> {
    private static final Logger logger = Logger.create(JfrDataAdapter.class);

    /**
     * Initializes a new instance of the {@link JfrDataAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public JfrDataAdapter() throws DataAdapterException {
        super(Path.of(""), ZoneId.systemDefault());
    }


    /**
     * Initializes a new instance of the {@link JfrDataAdapter} class with the provided parameters.
     *
     * @param jfrPath the path to the JFR file.
     * @param zoneId  the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JfrDataAdapter(Path jfrPath,
                          ZoneId zoneId)
            throws DataAdapterException {
        super(jfrPath, zoneId);
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<SearchHit>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(info -> BuiltInParsingProfile.NONE.getProfileId() + "/" + info.getBinding().getPath()).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(ts -> BuiltInParsingProfile.NONE.getProfileId() + "/" + ts.getBinding().getPath()).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        String rootPath = jfrFilePath.toString() + "|";
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(new LogFilesBinding.Builder()
                .withLabel(getSourceName())
                .withPath(jfrFilePath.toString() + "|")
                .withAdapter(this)
                .build());
        try (var recordingFile = new RecordingFile(jfrFilePath)) {
            for (EventType eventType : recordingFile.readEventTypes()) {
                var branch = tree;
                for (var cat : eventType.getCategoryNames()) {
                    var pos = TreeViewUtils.findFirstInTree(tree, item -> item.getValue().getLabel().equals(cat));
                    if (pos.isEmpty()) {
                        var node = new FilterableTreeItem<>((SourceBinding) new LogFilesBinding.Builder()
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
                var leaf = new FilterableTreeItem<>((SourceBinding) new LogFilesBinding.Builder()
                        .withLabel(eventType.getLabel())
                        .withLegend(eventType.getLabel())
                        .withPath(rootPath + eventType.getName())
                        .withParent(branch.getValue())
                        .withAdapter(this)
                        .build());
                branch.getInternalChildren().add(leaf);
            }
        } catch (IOException e) {
            throw new DataAdapterException("Error while attempting to read JFR recording: " + e.getMessage(), e);
        }
        return tree;
    }


    @Deprecated
    @Override
    public Map<TimeSeriesInfo<SearchHit>, TimeSeriesProcessor<SearchHit>> fetchData(String path,
                                                                                    Instant begin,
                                                                                    Instant end,
                                                                                    List<TimeSeriesInfo<SearchHit>> seriesInfo,
                                                                                    boolean bypassCache) throws DataAdapterException {
        reload(path, seriesInfo, bypassCache ? ReloadPolicy.ALL : ReloadPolicy.UNLOADED, null, INDEXING_OK);
        return new HashMap<>();
    }

    @Override
    public void reload(String path,
                       List<TimeSeriesInfo<SearchHit>> seriesInfo,
                       ReloadPolicy reloadPolicy,
                       DoubleProperty progress,
                       Property<ReloadStatus> reloadStatus) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(info -> BuiltInParsingProfile.NONE.getProfileId() + "/" + info.getBinding().getPath()).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching logs from " + path, e);
        }
    }

    @Override
    public String getSourceName() {
        return "[JFR: Events] " + (jfrFilePath != null ? jfrFilePath.getFileName() : "???");
    }
}
