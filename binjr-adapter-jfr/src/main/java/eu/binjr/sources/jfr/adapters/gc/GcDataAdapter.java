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

package eu.binjr.sources.jfr.adapters.gc;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.Index;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.indexes.IndexingStatus;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GcDataAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(GcDataAdapter.class);
    private static final Property<IndexingStatus> INDEXING_OK = new SimpleObjectProperty<>(IndexingStatus.OK);
    private static final String ZONE_ID = "zoneId";
    private static final String ENCODING = "encoding";
    private static final String PATH = "jvmGcPath";
    private ZoneId zoneId;
    private Path gcFilePath;
    private String encoding;
    private GcEventFormat eventFormat;

    protected Index index;

    public GcDataAdapter() throws DataAdapterException {
        this(Path.of(""), ZoneId.systemDefault());
    }

    public GcDataAdapter(Path path, ZoneId of) {
        initParams(of, path, StandardCharsets.UTF_8.name());
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        String rootPath = BuiltInParsingProfile.NONE.getProfileId() + "/" + gcFilePath.toString() + "|";
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(new TimeSeriesBinding.Builder()
                .withLabel(getSourceName())
                .withPath(rootPath)
                .withAdapter(this)
                .build());

        return tree;
    }

    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path, Instant begin, Instant end, List<TimeSeriesInfo<Double>> seriesInfo, boolean bypassCache) throws DataAdapterException {
        return null;
    }

    @Override
    public String getEncoding() {
        return this.encoding;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return this.zoneId;
    }

    @Override
    public String getSourceName() {
        return "[JVM GC] " + (gcFilePath != null ? gcFilePath.getFileName() : "???");
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PATH, gcFilePath.toString());
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        initParams(validateParameter(params, ZONE_ID,
                        s -> {
                            if (s == null) {
                                throw new InvalidAdapterParameterException("Parameter '" + ZONE_ID + "'  is missing in adapter " + getSourceName());
                            }
                            return ZoneId.of(s);
                        }),
                Paths.get(validateParameterNullity(params, PATH)),
                validateParameterNullity(params, ENCODING));
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {

            this.index = Indexes.LOG_FILES.acquire();
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

    @Override
    public void close() {
        try {
            Indexes.LOG_FILES.release();
        } catch (Exception e) {
            logger.error("An error occurred while releasing index " + Indexes.LOG_FILES.name() + ": " + e.getMessage());
            logger.debug("Stack Trace:", e);
        }
        super.close();
    }


    private void initParams(ZoneId zoneId, Path path, String encoding) {
        this.zoneId = zoneId;
        this.gcFilePath = path;
        this.encoding = encoding;
        this.eventFormat = new GcEventFormat(zoneId, Charset.forName(encoding));
    }
}
