/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.sources.text.adapters;


import com.google.gson.Gson;
import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.text.BinaryPrefixFormatter;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.LogFilesBinding;
import eu.binjr.core.data.adapters.LogFilter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.dialogs.Dialogs;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation to retrieve data from a text file.
 *
 * @author Frederic Thevenet
 */
public class LogsDataAdapter extends BaseDataAdapter<String> {
    private static final Logger logger = Logger.create(LogsDataAdapter.class);
    private static final Gson gson = new Gson();
    public static final String TIMESTAMP = "timestamp";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FIELD_CONTENT = "content";
    private static final String TIMESTAMP_VALUES = "timestampValues";
    public static final String PATH = "path";
    public static final String SEVERITY = "severity";
    public static final String THREAD = "thread";
    public static final String LOGGER = "logger";
    public static final String SORT_TIMESTAMP = "sortedTimestamp";
    public static final String FACET_SEVERITY = "facetSeverity";
    public static final String FACET_PATH = "facetPath";
    public static final String MESSAGE = "message";
    protected final LogsAdapterPreferences prefs = (LogsAdapterPreferences) getAdapterInfo().getPreferences();
    private LogFileIndex index;
    protected Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private final Map<String, Object> indexedFiles = new ConcurrentHashMap<String, Object>();
    private final BinaryPrefixFormatter binaryPrefixFormatter = new BinaryPrefixFormatter("###,###.## ");

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while initializing the adapter.
     */
    public LogsDataAdapter() throws DataAdapterException {
        super();
    }

    /**
     * Initializes a new instance of the {@link LogsDataAdapter} class from the provided {@link Path}
     *
     * @param rootPath the {@link Path} from which to load content.
     * @throws DataAdapterException if an error occurs initializing the adapter.
     */
    public LogsDataAdapter(Path rootPath, String[] folderFilters, String[] fileExtensionsFilters) throws DataAdapterException {
        super();
        this.rootPath = rootPath;
        Map<String, String> params = new HashMap<>();
        initParams(rootPath, folderFilters, fileExtensionsFilters);
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("rootPath", rootPath.toString());
        params.put("folderFilters", gson.toJson(folderFilters));
        params.put("fileExtensionsFilters", gson.toJson(fileExtensionsFilters));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (logger.isDebugEnabled()) {
            logger.debug(() -> "LogsDataAdapter params:");
            params.forEach((s, s2) -> logger.debug(() -> "key=" + s + ", value=" + s2));
        }
        initParams(Paths.get(validateParameterNullity(params, "rootPath")),
                gson.fromJson(validateParameterNullity(params, "folderFilters"), String[].class),
                gson.fromJson(validateParameterNullity(params, "fileExtensionsFilters"), String[].class));


    }

    private void initParams(Path rootPath, String[] folderFilters, String[] fileExtensionsFilters) throws DataAdapterException {
        this.rootPath = rootPath;
        this.folderFilters = folderFilters;
        this.fileExtensionsFilters = fileExtensionsFilters;
        try {
            this.fileBrowser = FileSystemBrowser.of(rootPath);
            this.index = new LogFileIndex();
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("An error occurred during the data adapter initialization", e);
        }
    }


    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> configNode = new FilterableTreeItem<>(
                new LogFilesBinding.Builder()
                        .withLabel("Log Files")
                        .withAdapter(this)
                        .build());
        attachNodes(configNode);
        return configNode;
    }

    private void attachNodes(FilterableTreeItem<SourceBinding> configNode) throws DataAdapterException {
        try (var p = Profiler.start("Building log files binding tree", logger::perf)) {
            Map<Path, FilterableTreeItem<SourceBinding>> nodeDict = new HashMap<>();
            nodeDict.put(fileBrowser.toPath("/"), configNode);
            for (var fsEntry : fileBrowser.listEntries(configPath ->
                    Arrays.stream(folderFilters)
                            .map(folder -> folder.equalsIgnoreCase("*") || configPath.startsWith(fileBrowser.toPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                            Arrays.stream(fileExtensionsFilters)
                                    .map(ext -> ext.equalsIgnoreCase("*") || configPath.getFileName().toString().toLowerCase(Locale.US).endsWith(ext))
                                    .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = fsEntry.getPath().getFileName().toString();
                var attachTo = configNode;
                if (fsEntry.getPath().getParent() != null) {
                    attachTo = nodeDict.get(fsEntry.getPath().getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, fsEntry.getPath().getParent(), configNode);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(fileName + " (" + binaryPrefixFormatter.format(fsEntry.getSize()) + "B)")
                                .withPath(fsEntry.getPath().toString())
                                .withParent(attachTo.getValue())
                                .withAdapter(this)
                                .build());
                attachTo.getInternalChildren().add(filenode);
            }
            TreeViewUtils.sortFromBranch(configNode);
        } catch (Exception e) {
            Dialogs.notifyException("Error while enumerating files: " + e.getMessage(), e);
        }
    }

    private FilterableTreeItem<SourceBinding> makeBranchNode(Map<Path, FilterableTreeItem<SourceBinding>> nodeDict,
                                                             Path path,
                                                             FilterableTreeItem<SourceBinding> root) {
        var parent = root;
        var rootPath = path.isAbsolute() ? path.getRoot() : path.getName(0);
        for (int i = 0; i < path.getNameCount(); i++) {
            Path current = rootPath.resolve(path.getName(i));
            FilterableTreeItem<SourceBinding> filenode = nodeDict.get(current);
            if (filenode == null) {
                filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(current.getFileName().toString())
                                .withPath(path.toString())
                                .withParent(parent.getValue())
                                .withAdapter(this)
                                .build());
                nodeDict.put(current, filenode);
                parent.getInternalChildren().add(filenode);
            }
            parent = filenode;
            rootPath = current;

        }
        return parent;
    }


    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<String>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo);
            return index.getTimeRangeBounries(seriesInfo.stream().map(i -> i.getBinding().getPath()).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }

    private void ensureIndexed(List<TimeSeriesInfo<String>> seriesInfo) throws IOException {
        for (var info : seriesInfo) {
            indexedFiles.computeIfAbsent(info.getBinding().getPath(), CheckedLambdas.wrap((p) -> {
                index.add(p, fileBrowser.getData(p));
                return p;
            }));
        }
    }

    @Override
    public Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> fetchData(String path,
                                                                              Instant start,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<String>> seriesInfo,
                                                                              boolean bypassCache) throws DataAdapterException {
        Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> data = new HashMap<>();
        try {
            ensureIndexed(seriesInfo);
            Map<String, Collection<String>> facets = new HashMap<>();
            facets.put(PATH, seriesInfo.stream().map(i -> i.getBinding().getPath()).collect(Collectors.toList()));
            var proc = new StringProcessor();
            var filter = (LogFilter) gson.fromJson(path, LogFilter.class);
            facets.put(SEVERITY, filter.severities);
            proc.setData(index.search(start.toEpochMilli(), end.toEpochMilli(), facets, filter.getFilterQuery()));
            data.put(null, proc);
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching logs from " + path, e);
        }

        return data;
    }

    private String readTextFile(String path) throws IOException {
        try (Profiler ignored = Profiler.start("Extracting text from file " + path, logger::perf)) {
            try (var reader = new BufferedReader(new InputStreamReader(fileBrowser.getData(path), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @Override
    public String getEncoding() {
        return "utf-8";
    }

    @Override
    public ZoneId getTimeZoneId() {
        return ZoneId.systemDefault();
    }

    @Override
    public String getSourceName() {
        return "[Logs] " + rootPath.getFileName();
    }

    @Override
    public void onStart() throws DataAdapterException {
        super.onStart();
    }

    @Override
    public void close() {
        IOUtils.close(index);
        IOUtils.close(fileBrowser);
        super.close();
    }

    public static class StringProcessor extends TimeSeriesProcessor<String> {

        @Override
        protected String computeMinValue() {
            return null;
        }

        @Override
        protected String computeAverageValue() {
            return null;
        }

        @Override
        protected String computeMaxValue() {
            return null;
        }
    }


    private static class LogEvent {
        private final String timestamp;
        private final int lineNumber;
        private String text;

        private static class LogEventBuilder {
            private final Pattern timestampPattern;
            private LogEvent previous = null;
            private int lineNumber;
            private String timestamp;
            private StringBuilder textBuilder = new StringBuilder();

            public LogEventBuilder(Pattern timestampPattern) {
                this.timestampPattern = timestampPattern;
            }

            public Optional<LogEvent> build(int lineNumber, String text) {
                var m = timestampPattern.matcher(text);
                if (m.find()) {
                    var yield = previous;
                    previous = new LogEvent(lineNumber, m.group(), text);
                    if (yield != null) {
                        return Optional.of(yield);
                    } else {
                        return Optional.empty();
                    }
                } else {
                    if (previous != null) {
                        previous.text = previous.text + "\n" + text;
                    }
                    return Optional.empty();
                }
            }

            public Optional<LogEvent> getLast() {
                return previous != null ? Optional.of(previous) : Optional.empty();
            }
        }

        private LogEvent(int lineNumber, String timestamp, String text) {
            this.lineNumber = lineNumber;
            this.text = text;
            this.timestamp = timestamp;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getText() {
            return text;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    public class LogFileIndex implements Closeable {
        private final Directory indexDirectory;
        private DirectoryReader indexReader;
        private IndexSearcher searcher;
        private final IndexWriter indexWriter;
        private final FacetsConfig facetsConfig;
        private final Pattern payloadPattern;
        private final Pattern timestampPattern;
        private final DateTimeFormatter dateTimeFormatter;
        private SortedSetDocValuesReaderState state;
        private final Path indexDirectoryPath;
        private final ReadWriteLockHelper indexLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());
        private final QueryParser parser = new QueryParser(FIELD_CONTENT, new StandardAnalyzer());

        public LogFileIndex() throws IOException {
            this.payloadPattern = Pattern.compile(
                    String.format("\\[\\s?(?<severity>%s)\\s?\\]\\s+\\[(?<thread>%s)\\]\\s+\\[(?<logger>%s)\\]",
                            prefs.severityPattern.get(),
                            prefs.threadPattern.get(),
                            prefs.loggerPattern.get()
                    ));
            this.timestampPattern = Pattern.compile(prefs.timestampPattern.get());
            logger.debug(() -> "Log parsing regexp: " + payloadPattern);
            this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss SSS").withZone(getTimeZoneId());
            switch (prefs.indexDirectoryLocation.get()) {
                case MEMORY:
                    indexDirectory = new ByteBuffersDirectory();
                    logger.debug("Lucene lucene directory stored on the Java Heap");
                    indexDirectoryPath = null;
                    break;
                default:
                case FILES_SYSTEM:
                    if (!MMapDirectory.UNMAP_SUPPORTED) {
                        logger.debug(MMapDirectory.UNMAP_NOT_SUPPORTED_REASON);
                    }
                    indexDirectoryPath = Files.createTempDirectory("binjr-logs-index_");
                    logger.debug("Lucene lucene directory stored at " + indexDirectoryPath);
                    indexDirectory = FSDirectory.open(indexDirectoryPath);
                    if (indexDirectory instanceof MMapDirectory) {
                        logger.info("Use unmap:" + ((MMapDirectory) indexDirectory).getUseUnmap());
                    }
            }
            IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            this.indexWriter = new IndexWriter(indexDirectory, iwc);
            indexReader = DirectoryReader.open(indexWriter);
            searcher = new IndexSearcher(indexReader);
            facetsConfig = new FacetsConfig();
            facetsConfig.setIndexFieldName(SEVERITY, FACET_SEVERITY);
            facetsConfig.setIndexFieldName(PATH, FACET_PATH);
        }

        public void add(String path, InputStream ias) throws IOException {
            try (Profiler ignored = Profiler.start("Indexing " + path, logger::perf)) {
                var n = new AtomicInteger(0);
                var builder = new LogEvent.LogEventBuilder(timestampPattern);
                try (Profiler p = Profiler.start(e -> logger.perf("Parsed and indexed " + n.get() + " lines: " + e.toMilliString()))) {
                    try (var reader = new BufferedReader(new InputStreamReader(ias, StandardCharsets.UTF_8))) {
                        reader.lines()
                                .map(line -> builder.build(n.incrementAndGet(), line))
                                .parallel()
                                .forEach(CheckedLambdas.wrap(line -> {
                                    if (line.isPresent()) {
                                        addLogEvent(path, line.get());
                                    }
                                }));
                    }
                    // Don't forget the last log line buffered in the builder
                    builder.getLast().ifPresent(CheckedLambdas.wrap(line -> {
                        addLogEvent(path, line);
                    }));
                }
                indexLock.write().lock(() -> {
                    try (Profiler p = Profiler.start("Commit index", logger::perf)) {
                        indexWriter.commit();
                    }
                    try (Profiler p = Profiler.start("Refresh index reader and searcher", logger::perf)) {
                        var updatedReader = DirectoryReader.openIfChanged(indexReader);
                        if (updatedReader != null) {
                            this.indexReader.close();
                            this.indexReader = updatedReader;
                            this.searcher = new IndexSearcher(indexReader);
                        }
                        this.state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader(), FACET_SEVERITY);
                    }
                });
            }
        }

        private void addLogEvent(String path, LogsDataAdapter.LogEvent event) throws IOException {
            Document doc = new Document();
            Matcher m = payloadPattern.matcher(event.getText());
            doc.add(new TextField(FIELD_CONTENT, event.getText(), Field.Store.YES));
            doc.add(new LongPoint(LINE_NUMBER, event.getLineNumber()));
            if (m.find()) {
                try {
                    var timeStamp = ZonedDateTime.parse(event.getTimestamp().replaceAll("[/\\-:.,T]", " "), dateTimeFormatter);
                    var millis = timeStamp.toInstant().toEpochMilli();
                    doc.add(new LongPoint(TIMESTAMP, millis));
                    doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
                    doc.add(new StoredField(TIMESTAMP, millis));
                    doc.add(new SortedSetDocValuesFacetField(PATH, path));
                    doc.add(new SortedSetDocValuesFacetField(SEVERITY, (m.group("severity") == null ? "unknown" : m.group("severity")).toLowerCase()));
//                    doc.add(new TextField(THREAD, (m.group("thread") == null ? "unknown" : m.group("thread")), Field.Store.NO));
//                    doc.add(new TextField(LOGGER, (m.group("logger") == null ? "unknown" : m.group("logger")), Field.Store.NO));
                } catch (Exception e) {
                    throw new ParsingEventException("Error parsing line: " + e.getMessage(), e);
                }
            }
            indexWriter.addDocument(facetsConfig.build(doc));
        }

        @Override
        public void close() throws IOException {
            IOUtils.close(indexReader);
            IOUtils.close(indexWriter);
            IOUtils.close(indexDirectory);

            if (indexDirectoryPath != null) {
                IOUtils.attemptDeleteTempPath(indexDirectoryPath);
            }
        }

        public TimeRange getTimeRangeBounries(List<String> files) throws IOException {
            ZonedDateTime beginning = getTimeRangeBoundary(false, files);
            ZonedDateTime end = getTimeRangeBoundary(true, files);
            return (TimeRange.of(
                    beginning != null ? beginning : ZonedDateTime.now().minusHours(24),
                    end != null ? end : ZonedDateTime.now()));

        }

        private ZonedDateTime getTimeRangeBoundary(boolean getMin, List<String> files) throws IOException {
            return indexLock.read().lock(() -> {
                var drill = new DrillSideways(searcher, facetsConfig, this.state);
                var dq = new DrillDownQuery(facetsConfig);
                for (var label : files) {
                    dq.add(PATH, label);
                }
                var top = drill.search(dq,
                        null,
                        null,
                        1,
                        new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, getMin)),
                        false);

                if (top.hits.scoreDocs.length > 0) {
                    return ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(Long.parseLong(
                                    searcher.doc(top.hits.scoreDocs[0].doc).get(TIMESTAMP))), getTimeZoneId());
                }
                return null;
            });
        }

        public List<XYChart.Data<ZonedDateTime, String>> search(long start, long end, Map<String, Collection<String>> facets, String query) throws Exception {
            return indexLock.read().lock(() -> {
                Query filterQuery = null;
                if (query != null && !query.isBlank()) {
                    filterQuery = parser.parse(query);
                    logger.debug("Parsed filter query: " + filterQuery.toString(FIELD_CONTENT));
                }
                Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
                var l = new ArrayList<XYChart.Data<ZonedDateTime, String>>();
                var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false));
                var drill = new DrillSideways(searcher, facetsConfig, this.state);
                var drillDownQuery = new DrillDownQuery(facetsConfig, filterQuery);
                for (var facet : facets.entrySet()) {
                    for (var label : facet.getValue()) {
                        drillDownQuery.add(facet.getKey(), label);
                    }
                }
                DrillSideways.DrillSidewaysResult results;
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(drillDownQuery, rangeQuery, null, prefs.hitsPerPage.get().intValue(), sort, false);
                }
                try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                    for (var hit : results.hits.scoreDocs) {
                        var doc = searcher.doc(hit.doc);
                        l.add(new XYChart.Data<>(
                                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), getTimeZoneId()),
                                doc.get(FIELD_CONTENT) + "\n"));
                    }
                }
                return l;
            });
        }
    }
}
