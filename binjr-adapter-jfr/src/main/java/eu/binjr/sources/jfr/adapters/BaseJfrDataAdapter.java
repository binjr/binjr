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


import com.google.gson.Gson;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.ReloadPolicy;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.Index;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.*;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

/**
 * A {@link DataAdapter} implementation to retrieve data from a JDK Flight Recorder file.
 *
 * @author Frederic Thevenet
 */
public abstract class BaseJfrDataAdapter<T> extends BaseDataAdapter<T> {
    private static final Logger logger = Logger.create(BaseJfrDataAdapter.class);
    protected static final Gson gson = new Gson();
    protected static final Property<ReloadStatus> INDEXING_OK = new SimpleObjectProperty<>(ReloadStatus.OK);
    protected static final String ZONE_ID = "zoneId";
    protected static final String ENCODING = "encoding";
    protected static final String PARSING_PROFILE = "parsingProfile";
    protected static final String PATH = "jfrPath";
    protected JfrEventFormat eventFormat;

    protected Path jfrFilePath;
    protected ZoneId zoneId;
    protected String encoding;

    protected Index index;
    protected FileSystemBrowser fileBrowser;


    public BaseJfrDataAdapter(Path jfrPath, ZoneId zoneId) throws DataAdapterException {
        super();
        initParams(zoneId, jfrPath, "utf-8");
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
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PATH, jfrFilePath.toString());
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

    private void initParams(ZoneId zoneId,
                            Path jfrPath,
                            String encoding) {
        this.zoneId = zoneId;
        this.jfrFilePath = jfrPath;
        this.encoding = encoding;
        this.eventFormat = new JfrEventFormat(zoneId, Charset.forName(encoding));
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(jfrFilePath.getParent());
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
        IOUtils.close(fileBrowser);
        super.close();
    }

    public synchronized void ensureIndexed(Set<String> sources, ReloadPolicy reloadPolicy) throws IOException {
        if (reloadPolicy == ReloadPolicy.ALL) {
            sources.forEach(index.getIndexedFiles()::remove);
        }
        final LongProperty charRead = new SimpleLongProperty(0);
        var isCommitNecessary = false;
        var filterMap = new HashMap<Path, HashSet<String>>();
        for (var binding : sources) {
            if (!index.getIndexedFiles().containsKey(binding)) {
                var a = binding.split("\\|");
                var filePath = Path.of(a[0].replace(BuiltInParsingProfile.NONE.getProfileId() + "/", ""));
                var eventType = a[1];
                filterMap.computeIfAbsent(filePath, p -> new HashSet<>()).add(eventType);
                isCommitNecessary = true;
                index.getIndexedFiles().put(binding, ReloadStatus.OK);
            }
        }
        for (Map.Entry<Path, HashSet<String>> entry : filterMap.entrySet()) {
            Path path = entry.getKey();
            HashSet<String> strings = entry.getValue();
            index.add(path.toString(),
                    new JfrRecordingFilter(path, strings),
                    false,
                    eventFormat,
                    (doc, event) -> {
                        // Add number fields
                        event.getNumberFields().forEach((key, value) -> doc.add(new StoredField(key, value.doubleValue())));
                        // Set HAS_NUM field
                        doc.add(new StringField(JfrEventFormat.HAS_NUM_FIELDS, event.getNumberFields().size() > 0 ? "true" : "false", Field.Store.NO));
                        // Add event categories as severity
                        String severity = event.getTextField(JfrEventFormat.CATEGORIES) == null ? "JFR" :
                                event.getTextField(JfrEventFormat.CATEGORIES);//.toLowerCase();
                        doc.add(new FacetField(SEVERITY, severity));
                        doc.add(new StoredField(SEVERITY, severity));
                        return doc;
                    },
                    charRead,
                    INDEXING_OK,
                    (rootPath, parsedEvent) -> BuiltInParsingProfile.NONE.getProfileId() + "/" + rootPath + "|" + parsedEvent.getTextField(JfrEventFormat.EVENT_TYPE_NAME),
                    (source) -> source.eventTypes().stream().map(type -> BuiltInParsingProfile.NONE.getProfileId() + "/" + source.recordingPath().toString() + "|" + type).toList());
        }
        if (isCommitNecessary) {
            index.commitIndexAndTaxonomy();
        }
    }

}
