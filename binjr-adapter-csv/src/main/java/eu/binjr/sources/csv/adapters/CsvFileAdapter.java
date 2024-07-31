/*
 *    Copyright 2017-2022 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;

import com.google.gson.Gson;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.Index;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.indexes.IndexingStatus;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.sources.csv.data.parsers.BuiltInCsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CsvEventFormat;
import eu.binjr.sources.csv.data.parsers.CsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CustomCsvParsingProfile;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;
import org.apache.lucene.document.StoredField;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local CSV formatted file.
 *
 * @author Frederic Thevenet
 */
public class CsvFileAdapter extends BaseDataAdapter<Double> {
    private static final Logger logger = Logger.create(CsvFileAdapter.class);
    private static final Gson gson = new Gson();
    private static final Property<IndexingStatus> INDEXING_OK = new SimpleObjectProperty<>(IndexingStatus.OK);
    private static final String ZONE_ID = "zoneId";
    private static final String ENCODING = "encoding";
    private static final String DELIMITER = "delimiter";
    private static final String PARSING_PROFILE = "parsingProfile";
    private static final String PATH = "csvPath";
    private static final String TIMESTAMP_POSITION = "timestampPosition";
    private CsvEventFormat parser;
    private CsvParsingProfile csvParsingProfile;
    private Path csvPath;
    private ZoneId zoneId;
    private String encoding;
    private final Map<String, IndexingStatus> indexedFiles = new HashMap<>();
    private Index index;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private List<String> headers;


    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public CsvFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", BuiltInCsvParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class for the provided file and time zone.
     *
     * @param csvPath the path to the csv file.
     * @param zoneId  the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String csvPath, ZoneId zoneId) throws DataAdapterException {
        this(csvPath, zoneId, "utf-8", BuiltInCsvParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with the provided parameters.
     *
     * @param csvPath           the path to the csv file.
     * @param zoneId            the time zone to used.
     * @param encoding          the encoding for the csv file.
     * @param csvParsingProfile a pattern to decode time stamps.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String csvPath,
                          ZoneId zoneId,
                          String encoding,
                          CsvParsingProfile csvParsingProfile)
            throws DataAdapterException {
        super();
        initParams(zoneId, csvPath, encoding, csvParsingProfile);
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        try (InputStream in = Files.newInputStream(csvPath)) {
            this.headers = parser.getDataColumnHeaders(in);
            for (int i = 0; i < headers.size(); i++) {
                if (i != csvParsingProfile.getTimestampColumn()) {
                    String header = headers.get(i).isBlank() ? "Column #" + i : headers.get(i);
                    var b = new TimeSeriesBinding.Builder()
                            .withLabel(Integer.toString(i))
                            .withPath(getId() + "/" + csvPath.toString())
                            .withLegend(header)
                            .withParent(tree.getValue())
                            .withAdapter(this)
                            .build();
                    tree.getInternalChildren().add(new TreeItem<>(b));
                }
            }
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
        return tree;
    }

    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            return index.getTimeRangeBoundaries(seriesInfo.stream().map(ts -> ts.getBinding().getPath()).toList(), getTimeZoneId());
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }


    @Override
    public Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> fetchData(String path,
                                                                              Instant begin,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<Double>> seriesInfo,
                                                                              boolean bypassCache) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo.stream().map(TimeSeriesInfo::getBinding).collect(Collectors.toSet()), ReloadPolicy.UNLOADED);
            Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> series = new HashMap<>();
            for (TimeSeriesInfo<Double> info : seriesInfo) {
                series.put(info, new DoubleTimeSeriesProcessor());
            }
            var nbHits = index.search(
                    begin.toEpochMilli(),
                    end.toEpochMilli(),
                    series,
                    zoneId,
                    bypassCache);
            logger.debug(() -> "Retrieved " + nbHits + " hits");
            return series;
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching data from " + path, e);
        }
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
        return "[CSV] " +
                (csvPath != null ? csvPath.getFileName() : "???") +
                " (" +
                (zoneId != null ? zoneId : "???") +
                ")";
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PARSING_PROFILE, gson.toJson(CustomCsvParsingProfile.of(csvParsingProfile)));
        params.put(PATH, csvPath.toString());
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
                validateParameterNullity(params, PATH),
                validateParameterNullity(params, ENCODING),
                gson.fromJson(validateParameterNullity(params, PARSING_PROFILE), CustomCsvParsingProfile.class));
    }

    private void initParams(ZoneId zoneId,
                            String csvPath,
                            String encoding,
                            CsvParsingProfile parsingProfile) {
        this.zoneId = zoneId;
        this.csvPath = Path.of(csvPath);
        this.encoding = encoding;
        this.csvParsingProfile = parsingProfile;
        this.parser = new CsvEventFormat(parsingProfile, zoneId, Charset.forName(encoding));
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
        try {
            this.fileBrowser = FileSystemBrowser.of(csvPath.getParent());
            this.index = Indexes.NUM_SERIES.acquire();
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }

    @Override
    public void close() {
        try {
            Indexes.NUM_SERIES.release();
        } catch (Exception e) {
            logger.error("An error occurred while releasing index " + Indexes.NUM_SERIES.name() + ": " + e.getMessage());
            logger.debug("Stack Trace:", e);
        }
        IOUtils.close(fileBrowser);
        super.close();
    }

    private double formatToDouble(String value, NumberFormat numberFormat) {
        if (value != null) {
            try {
                return numberFormat.parse(value).doubleValue();
            } catch (Exception e) {
                logger.trace(() -> "Failed to convert '" + value + "' to double");
            }
        }
        return Double.NaN;
    }

    private synchronized void ensureIndexed(Set<SourceBinding<Double>> bindings, ReloadPolicy reloadPolicy) throws IOException {
        if (reloadPolicy == ReloadPolicy.ALL) {
            bindings.stream().map(SourceBinding::getPath).forEach(indexedFiles::remove);
        }
        final LongProperty charRead = new SimpleLongProperty(0);
        for (var binding : bindings) {
            String path = binding.getPath();
            indexedFiles.computeIfAbsent(path, CheckedLambdas.wrap(p -> {
                ThreadLocal<NumberFormat> formatters =
                        ThreadLocal.withInitial(() -> NumberFormat.getNumberInstance(csvParsingProfile.getNumberFormattingLocale()));
                try {
                    index.add(p,
                            fileBrowser.getData(path.replace(getId() + "/", "")),
                            true,
                            parser,
                            (doc, event) -> {
                                event.getTextFields().forEach((key, value) -> doc.add(new StoredField(key, formatToDouble(value, formatters.get()))));
                                return doc;
                            },
                            charRead,
                            INDEXING_OK);
                    return IndexingStatus.OK;
                } finally {
                    formatters.remove();
                }
            }));
        }

    }
}
