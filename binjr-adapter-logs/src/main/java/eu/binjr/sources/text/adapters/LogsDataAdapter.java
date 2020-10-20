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
import eu.binjr.core.data.adapters.LogQueryParameters;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.LogEvent;
import eu.binjr.core.data.timeseries.LogEventsProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.dialogs.Dialogs;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link eu.binjr.core.data.adapters.DataAdapter} implementation to retrieve data from a text file.
 *
 * @author Frederic Thevenet
 */
public class LogsDataAdapter extends BaseDataAdapter<LogEvent> {
    private static final Logger logger = Logger.create(LogsDataAdapter.class);
    private static final Gson gson = new Gson();
    public static final String TIMESTAMP = "timestamp";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FIELD_CONTENT = "content";
    public static final String PATH = "filePath";
    public static final String SEVERITY = "severity";
    public static final String THREAD = "thread";
    public static final String LOGGER = "logger";
    public static final String FACET_FIELD = "facets";
    public static final String MESSAGE = "message";
    protected final LogsAdapterPreferences prefs = (LogsAdapterPreferences) getAdapterInfo().getPreferences();
    private LogFileIndex index;
    protected Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private final Set<String> indexedFiles = new HashSet<>();
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
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<LogEvent>> seriesInfo) throws DataAdapterException {
        try {
            ensureIndexed(seriesInfo);
            return index.getTimeRangeBoundaries(
                    seriesInfo.stream()
                            .map(i -> i.getBinding().getPath())
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new DataAdapterException("Error retrieving initial time range", e);
        }
    }

    private synchronized void ensureIndexed(List<TimeSeriesInfo<LogEvent>> seriesInfo) throws IOException {
        var toDo = seriesInfo.stream()
                .map(s -> s.getBinding().getPath())
                .filter(p -> !indexedFiles.contains(p))
                .collect(Collectors.toList());
        for (int i = 0; i < toDo.size(); i++) {
            String path = toDo.get(i);
            index.add(path, fileBrowser.getData(path), (i == toDo.size() - 1));
            indexedFiles.add(path);
        }
    }

    @Override
    public Map<TimeSeriesInfo<LogEvent>, TimeSeriesProcessor<LogEvent>> fetchData(String path,
                                                                                  Instant start,
                                                                                  Instant end,
                                                                                  List<TimeSeriesInfo<LogEvent>> seriesInfo,
                                                                                  boolean bypassCache) throws DataAdapterException {
        Map<TimeSeriesInfo<LogEvent>, TimeSeriesProcessor<LogEvent>> data = new HashMap<>();
        try {
            ensureIndexed(seriesInfo);
            Map<String, Collection<String>> facets = new HashMap<>();
            facets.put(PATH, seriesInfo.stream().map(i -> i.getBinding().getPath()).collect(Collectors.toList()));
            var filter = (LogQueryParameters) gson.fromJson(path, LogQueryParameters.class);
            facets.put(SEVERITY, filter.getSeverities());
            var proc = (seriesInfo.size() == 0) ? new LogEventsProcessor() :
                    index.filter(start.toEpochMilli(), end.toEpochMilli(), facets, filter.getFilterQuery(), filter.getPage());
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

    private static class ParsedLogEvent {
        private final String timestamp;
        private final int lineNumber;
        private String text;

        private static class LogEventBuilder {
            private final Pattern timestampPattern;
            private ParsedLogEvent previous = null;
            private int lineNumber;
            private String timestamp;
            private StringBuilder textBuilder = new StringBuilder();

            public LogEventBuilder(Pattern timestampPattern) {
                this.timestampPattern = timestampPattern;
            }

            public Optional<ParsedLogEvent> build(int lineNumber, String text) {
                var m = timestampPattern.matcher(text);
                if (m.find()) {
                    var yield = previous;
                    previous = new ParsedLogEvent(lineNumber, m.group(), text);
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

            public Optional<ParsedLogEvent> getLast() {
                return previous != null ? Optional.of(previous) : Optional.empty();
            }
        }

        private ParsedLogEvent(int lineNumber, String timestamp, String text) {
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

    private class LogFileIndex implements Closeable {
        private final Directory indexDirectory;
        private final Directory taxonomyDirectory;
        private final TaxonomyWriter taxonomyWriter;
        private TaxonomyReader taxonomyReader;
        private DirectoryReader indexReader;
        private IndexSearcher searcher;
        private final IndexWriter indexWriter;
        private final FacetsConfig facetsConfig;
        private final Pattern payloadPattern;
        private final Pattern timestampPattern;
        private final DateTimeFormatter dateTimeFormatter;
        private final Path indexDirectoryPath;
        private final ReadWriteLockHelper indexLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());
        private final ExecutorService parsingThreadPool;
        private final int parsingThreadsNumber;

        public LogFileIndex() throws IOException {
            this.parsingThreadsNumber = prefs.parsingThreadNumber.get().intValue() < 1 ?
                    (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 2.0) :
                    Math.min(Runtime.getRuntime().availableProcessors(), prefs.parsingThreadNumber.get().intValue());
            AtomicInteger threadNum = new AtomicInteger(0);
            this.parsingThreadPool = Executors.newFixedThreadPool(parsingThreadsNumber, r -> {
                Thread thread = new Thread(r);
                thread.setName("parsing-thread-" + threadNum.incrementAndGet());
                return thread;
            });

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
                    taxonomyDirectory = new ByteBuffersDirectory();
                    logger.debug("Lucene lucene directory stored on the Java Heap");
                    indexDirectoryPath = null;
                    break;
                default:
                case FILES_SYSTEM:
                    if (!MMapDirectory.UNMAP_SUPPORTED) {
                        logger.debug(MMapDirectory.UNMAP_NOT_SUPPORTED_REASON);
                    }
                    indexDirectoryPath = Files.createTempDirectory("binjr-logs-index_");
                    indexDirectory = FSDirectory.open(indexDirectoryPath.resolve("index"));
                    taxonomyDirectory = FSDirectory.open(indexDirectoryPath.resolve("taxonomy"));
                    logger.debug("Lucene lucene directory stored at " + indexDirectoryPath);
                    if (indexDirectory instanceof MMapDirectory) {
                        logger.debug("Use unmap:" + ((MMapDirectory) indexDirectory).getUseUnmap());
                    }
            }
            IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            this.indexWriter = new IndexWriter(indexDirectory, iwc);
            this.taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory);
            indexReader = DirectoryReader.open(indexWriter);
            searcher = new IndexSearcher(indexReader);
            facetsConfig = new FacetsConfig();
            facetsConfig.setRequireDimCount(SEVERITY, true);
            facetsConfig.setRequireDimensionDrillDown(PATH, true);
            facetsConfig.setRequireDimCount(PATH, true);
            facetsConfig.setRequireDimensionDrillDown(PATH, true);
            logger.debug(() -> "New indexer initialized at " + indexDirectoryPath +
                    " using " + parsingThreadsNumber + " parsing indexing threads");
            logger.debug(() -> facetsConfig.getDimConfigs().entrySet().stream()
                    .map(e -> "path= " + e.getKey() +
                            " field= " + e.getValue().indexFieldName +
                            " multivalued=" + e.getValue().multiValued +
                            " hierarchical=" + e.getValue().hierarchical +
                            " requireDimCount=" + e.getValue().requireDimCount +
                            " requireDimensionDrillDown=" + e.getValue().requireDimensionDrillDown)
                    .collect(Collectors.joining("\n")));
        }

        public void add(String path, InputStream ias) throws IOException {
            add(path, ias, true);
        }

        public void add(String path, InputStream ias, boolean commit) throws IOException {
            try (Profiler ignored = Profiler.start("Indexing " + path, logger::perf)) {
                var n = new AtomicInteger(0);
                var builder = new ParsedLogEvent.LogEventBuilder(timestampPattern);
                try (Profiler p = Profiler.start(e -> logger.perf("Parsed and indexed " + n.get() + " lines: " + e.toMilliString()))) {
                    final AtomicLong nbLogEvents = new AtomicLong(0);
                    final AtomicBoolean taskDone = new AtomicBoolean(false);
                    final AtomicBoolean taskAborted = new AtomicBoolean(false);
                    final ArrayBlockingQueue<ParsedLogEvent> queue = new ArrayBlockingQueue<>(prefs.blockingQueueCapacity.get().intValue());
                    final List<Future<Integer>> results = new ArrayList<>();
                    for (int i = 0; i < parsingThreadsNumber; i++) {
                        results.add(parsingThreadPool.submit(() -> {
                            logger.trace(() -> "Starting parsing worker on thread " + Thread.currentThread().getName());
                            int nbEventProcessed = 0;
                            do {
                                List<ParsedLogEvent> todo = new ArrayList<>();
                                var drained = queue.drainTo(todo, prefs.parsingThreadDrainSize.get().intValue());
                                if (drained == 0 && queue.size() == 0) {
                                    // Park the thread for a while before polling again
                                    // as is it likely that producer is done.
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                                try {
                                    for (var logEvent : todo) {
                                        addLogEvent(path, logEvent);
                                        nbEventProcessed++;
                                    }
                                } catch (Throwable t) {
                                    // Signal that worker thread was aborted and rethrow
                                    taskAborted.set(true);
                                    throw t;
                                }
                            } while (!taskDone.get() && !taskAborted.get() && !Thread.currentThread().isInterrupted());
                            return nbEventProcessed;
                        }));
                    }
                    try (var reader = new BufferedReader(new InputStreamReader(ias, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null && !taskAborted.get()) {
                            var optLog = builder.build(n.incrementAndGet(), line);
                            if (optLog.isPresent()) {
                                queue.put(optLog.get());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    while (!taskAborted.get() && queue.size() > 0) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    taskDone.set(true);
                    for (Future<Integer> f : results) {
                        //signal exceptions that may have happened on thread pool
                        try {
                            logger.trace("Thread added " + f.get() + " log event to index");
                            nbLogEvents.addAndGet(f.get());
                        } catch (InterruptedException e) {
                            logger.error("Getting result from worker was interrupted", e);
                        } catch (Exception e) {
                            //rethrow execution exceptions
                            throw new IOException("Error parsing logEvent", e);
                        }
                    }
                    // Don't forget the last log line buffered in the builder
                    builder.getLast().ifPresent(CheckedLambdas.wrap(line -> {
                        addLogEvent(path, line);
                    }));
                }
                if (commit) {
                    indexLock.write().lock(() -> {
                        try (Profiler p = Profiler.start("Commit index", logger::perf)) {
                            taxonomyWriter.commit();
                            indexWriter.commit();
                        }
                        try (Profiler p = Profiler.start("Refresh index reader and searcher", logger::perf)) {
                            var updatedReader = DirectoryReader.openIfChanged(indexReader);
                            if (updatedReader != null) {
                                this.indexReader.close();
                                this.indexReader = updatedReader;
                                this.searcher = new IndexSearcher(indexReader);
                            }
                            if (taxonomyReader == null) {
                                taxonomyReader = new DirectoryTaxonomyReader(taxonomyDirectory);
                            } else {
                                var updatedTaxoReader = DirectoryTaxonomyReader.openIfChanged(taxonomyReader);
                                if (updatedTaxoReader != null) {
                                    this.taxonomyReader.close();
                                    this.taxonomyReader = updatedTaxoReader;
                                }
                            }
                        }
                    });
                }
            }
        }

        private void addLogEvent(String path, ParsedLogEvent event) throws IOException {
            Document doc = new Document();
            Matcher m = payloadPattern.matcher(event.getText());
            doc.add(new TextField(FIELD_CONTENT, event.getText(), Field.Store.YES));
            doc.add(new SortedNumericDocValuesField(LINE_NUMBER, event.getLineNumber()));
            if (m.find()) {
                try {
                    var timeStamp = ZonedDateTime.parse(event.getTimestamp().replaceAll("[/\\-:.,T]", " "), dateTimeFormatter);
                    var millis = timeStamp.toInstant().toEpochMilli();
                    doc.add(new LongPoint(TIMESTAMP, millis));
                    doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
                    doc.add(new StoredField(TIMESTAMP, millis));
                    doc.add(new FacetField(PATH, path));
                    doc.add(new StoredField(PATH, path));
                    String severity = (m.group("severity") == null ? "unknown" : m.group("severity")).toLowerCase();
                    doc.add(new FacetField(SEVERITY, severity));
                    doc.add(new StoredField(SEVERITY, severity));
//                    doc.add(new TextField(THREAD, (m.group("thread") == null ? "unknown" : m.group("thread")), Field.Store.NO));
//                    doc.add(new TextField(LOGGER, (m.group("logger") == null ? "unknown" : m.group("logger")), Field.Store.NO));
                } catch (Exception e) {
                    throw new ParsingEventException("Error parsing line: " + e.getMessage(), e);
                }
            }
            indexWriter.addDocument(facetsConfig.build(taxonomyWriter, doc));
        }

        @Override
        public void close() throws IOException {
            IOUtils.close(taxonomyReader);
            IOUtils.close(indexReader);
            IOUtils.close(taxonomyWriter);
            IOUtils.close(indexWriter);
            IOUtils.close(indexDirectory);
            if (parsingThreadPool != null) {
                try {
                    parsingThreadPool.shutdown();
                    parsingThreadPool.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("Termination interrupted", e);
                }
            }
            if (indexDirectoryPath != null) {
                IOUtils.attemptDeleteTempPath(indexDirectoryPath);
            }
        }

        public TimeRange getTimeRangeBoundaries(List<String> files) throws IOException {
            ZonedDateTime beginning = getTimeRangeBoundary(false, files);
            ZonedDateTime end = getTimeRangeBoundary(true, files);
            return (TimeRange.of(
                    beginning != null ? beginning : ZonedDateTime.now().minusHours(24),
                    end != null ? end : ZonedDateTime.now()));
        }

        private ZonedDateTime getTimeRangeBoundary(boolean getMin, List<String> files) throws IOException {
            return indexLock.read().lock(() -> {
                var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
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

        private HashMap<String, FacetEntry> makeFacetResult(String facetName, Facets facets, Map<String, Collection<String>> params) throws IOException {
            var facetEntryMap = new HashMap<String, FacetEntry>();
            var synthesis = facets.getTopChildren(100, facetName);
            var labels = new ArrayList<String>();
            if (synthesis != null) {
                for (var f : synthesis.labelValues) {
                    facetEntryMap.put(f.label, new FacetEntry(facetName, f.label, f.value.intValue()));
                    labels.add(f.label);
                }
                // Add facets labels used in query if not present in the result
                params.getOrDefault(facetName, List.of()).stream()
                        .filter(l -> !labels.contains(l))
                        .map(l -> new FacetEntry(facetName, l, 0))
                        .forEach(f -> facetEntryMap.put(f.getLabel(), f));
            }
            return facetEntryMap;
        }

        public LogEventsProcessor filter(long start, long end, Map<String, Collection<String>> params, String query, int page) throws Exception {
            return indexLock.read().lock(() -> {
                Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
                Query filterQuery = rangeQuery;
                if (query != null && !query.isBlank()) {
                    logger.trace("Query text=" + query);
                    QueryParser parser = new QueryParser(FIELD_CONTENT, new StandardAnalyzer());
                    filterQuery = new BooleanQuery.Builder()
                            .add(rangeQuery, BooleanClause.Occur.FILTER)
                            .add(parser.parse(query), BooleanClause.Occur.FILTER)
                            .build();
                }
                var logs = new ArrayList<XYChart.Data<ZonedDateTime, LogEvent>>();
                var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
                var drillDownQuery = new DrillDownQuery(facetsConfig, filterQuery);
                for (var facet : params.entrySet()) {
                    for (var label : facet.getValue()) {
                        drillDownQuery.add(facet.getKey(), label);
                    }
                }
                var pageSize = prefs.hitsPerPage.get().intValue();
                var skip = page * pageSize;
                DrillSideways.DrillSidewaysResult results;
                var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                        new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
                TopFieldCollector collector = TopFieldCollector.create(sort, skip + pageSize, Integer.MAX_VALUE);
                logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(drillDownQuery, collector);
                }
                var topDocs = collector.topDocs();
                logger.debug("collector.getTotalHits() = " + collector.getTotalHits());
                var severityFacet = new HashMap<String, FacetEntry>();
                var pathFacet = new HashMap<String, FacetEntry>();
                try (Profiler p = Profiler.start("Retrieving hits & facets", logger::perf)) {
                    pathFacet = makeFacetResult(PATH, results.facets, params);
                    severityFacet = makeFacetResult(SEVERITY, results.facets, params);
                    for (int i = skip; i < topDocs.scoreDocs.length; i++) {
                        var hit = topDocs.scoreDocs[i];
                        var doc = searcher.doc(hit.doc);
                        var severity = severityFacet.get(doc.get(SEVERITY));
                        var path = pathFacet.get(doc.get(PATH));
                        logs.add(new XYChart.Data<>(
                                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), getTimeZoneId()),
                                new LogEvent(doc.get(FIELD_CONTENT) + "\n",
                                        severity != null ? severity : new FacetEntry(SEVERITY, "Unknown", 0),
                                        path != null ? path : new FacetEntry(PATH, "Unknown", 0))));
                    }
                }
                var proc = new LogEventsProcessor();
                proc.setData(logs);
                proc.addFacetResults(PATH,
                        pathFacet.values()
                                .stream()
                                .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                .collect(Collectors.toList()));
                proc.addFacetResults(SEVERITY,
                        severityFacet.values()
                                .stream()
                                .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                .collect(Collectors.toList()));
                proc.setTotalHits(collector.getTotalHits());
                proc.setHitsPerPage(pageSize);
                return proc;
            });
        }
    }

}
