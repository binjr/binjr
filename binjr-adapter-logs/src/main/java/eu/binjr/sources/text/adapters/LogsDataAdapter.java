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
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.LogFilesBinding;
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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    protected final LogsAdapterPreferences prefs = (LogsAdapterPreferences) getAdapterInfo().getPreferences();
    private LogFileIndex index;
    protected Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;
    private final Map<String, Object> indexedFiles = new ConcurrentHashMap<String, Object>();


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
            for (Path conf : fileBrowser.listEntries(configPath ->
                    Arrays.stream(folderFilters)
                            .map(folder -> folder.equalsIgnoreCase("*") || configPath.startsWith(fileBrowser.toPath(folder)))
                            .reduce(Boolean::logicalOr).orElse(false) &&
                            Arrays.stream(fileExtensionsFilters)
                                    .map(ext -> ext.equalsIgnoreCase("*") || configPath.getFileName().toString().toLowerCase(Locale.US).endsWith(ext))
                                    .reduce(Boolean::logicalOr).orElse(false))) {
                String fileName = conf.getFileName().toString();
                var attachTo = configNode;
                if (conf.getParent() != null) {
                    attachTo = nodeDict.get(conf.getParent());
                    if (attachTo == null) {
                        attachTo = makeBranchNode(nodeDict, conf.getParent(), configNode);
                    }
                }
                FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                        new LogFilesBinding.Builder()
                                .withLabel(fileName)
                                .withPath(conf.toString())
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


//    @Override
//    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<String>> seriesInfos) throws DataAdapterException {
//        return null;
//    }

    @Override
    public Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> fetchData(String path,
                                                                              Instant start,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<String>> seriesInfos,
                                                                              boolean bypassCache) throws DataAdapterException {
        Map<TimeSeriesInfo<String>, TimeSeriesProcessor<String>> data = new HashMap<>();
        try {
            Map<String, Collection<String>> facets = new HashMap<>();
            List<String> paths = new ArrayList<>();
            for (var info : seriesInfos) {

                indexedFiles.computeIfAbsent(info.getBinding().getPath(), CheckedLambdas.wrap((p) -> {
                    index.add(p, fileBrowser.getData(p));
                    return p;
                }));
                paths.add(info.getBinding().getPath());
            }
            facets.put(PATH, paths);
            var proc = new TextProcessor();
            // LogFilesBinding binding = (LogFilesBinding) info.getBinding();

            //  new QueryParser();
            //  QueryParser parser = new QueryParser(field, analyzer);
            //    proc.setData(List.of(new XYChart.Data<>(ZonedDateTime.now(), readTextFile(info.getBinding().getPath()))));
            var builder = new QueryBuilder(new StandardAnalyzer());
            var tr = new Term(FIELD_CONTENT, "info");
            //var q = new TermQuery(tr);


            proc.setData(index.search(start.toEpochMilli(), end.toEpochMilli(), facets, null));
            data.put(null, proc);
        } catch (IOException e) {
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

    public static class TextProcessor extends TimeSeriesProcessor<String> {

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

    public class LogFileIndex implements Closeable {


        private final Directory indexDirectory;
        //   private final DirectoryReader indexReader;
        //private final IndexSearcher searcher;
        private final SearcherManager searcherManager;
        private final IndexWriter indexWriter;
        private final FacetsConfig facetsConfig;
        private Pattern pattern;
        private DateTimeFormatter dateTimeFormatter;
        private SortedSetDocValuesReaderState state;

        public LogFileIndex() throws IOException {

            indexDirectory = new ByteBuffersDirectory();
            this.pattern = Pattern.compile(String.format(prefs.linePattern.get(),
                    prefs.timestampPattern.get(),
                    prefs.severityPattern.get(),
                    prefs.threadPattern.get(),
                    prefs.loggerPattern.get(),
                    prefs.msgPattern.get()));
            this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss SSS").withZone(getTimeZoneId());
            //   indexDirectory = FSDirectory.open(Files.createTempDirectory("binjr-idx").resolve(path));

            IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            this.indexWriter = new IndexWriter(indexDirectory, iwc);

//            indexReader = DirectoryReader.open(indexDirectory, );
//            logger.info("Total indexed lines: " + indexReader.getDocCount(FIELD_CONTENT));
            this.searcherManager = new SearcherManager(indexWriter, false, false, null);
            facetsConfig = new FacetsConfig();
            facetsConfig.setIndexFieldName(SEVERITY, FACET_SEVERITY);
            facetsConfig.setIndexFieldName(PATH, FACET_PATH);

        }

        public void add(String path, InputStream ias) throws IOException {
            var n = new AtomicInteger(0);

            try (Profiler ignored = Profiler.start(e -> logger.perf("Indexed " + n.get() + " lines for file " + path + e.toMilliString()))) {
                try (var reader = new BufferedReader(new InputStreamReader(ias, StandardCharsets.UTF_8))) {
                    reader.lines()
                            .map(line -> new LogEvent(n.incrementAndGet(), line))
                            .parallel()
                            .forEach(CheckedLambdas.wrap(line -> {
                                var doc = parseLine(path, line);
                                indexWriter.addDocument(facetsConfig.build(doc));
                            }));
                }
            }
            try (Profiler p = Profiler.start("Commit index", logger::perf)) {
                indexWriter.commit();
            }
            try (Profiler p = Profiler.start("Refresh searchManager", logger::perf)) {
                searcherManager.maybeRefreshBlocking();
            }
            IndexSearcher searcher = searcherManager.acquire();
            try (Profiler p = Profiler.start("Initialize  DocValues reader state", logger::perf)) {
                this.state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader(), FACET_SEVERITY);
            } finally {
                searcherManager.release(searcher);
            }
        }

        private class LogEvent {
            private final int lineNumber;
            private final String text;

            private LogEvent(int lineNumber, String message) {
                this.lineNumber = lineNumber;
                this.text = message;
            }

            public int getLineNumber() {
                return lineNumber;
            }

            public String getText() {
                return text;
            }
        }

        private Document parseLine(String path, LogEvent event) throws ParsingEventException {
            Document doc = new Document();
            Matcher m = pattern.matcher(event.getText());
            if (m.find()) {
                try {
                    var timeStamp = m.group("time") == null ? ZonedDateTime.now() :
                            ZonedDateTime.parse(m.group("time").replaceAll("[/\\-:.,T]", " "), dateTimeFormatter);
                    var millis = timeStamp.toInstant().toEpochMilli();
                    doc.add(new LongPoint(TIMESTAMP, millis));
                    doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
                    doc.add(new StoredField(TIMESTAMP, millis));
                    doc.add(new LongPoint(LINE_NUMBER, event.getLineNumber()));
                    doc.add(new SortedSetDocValuesFacetField(PATH, path));
                    doc.add(new SortedSetDocValuesFacetField(SEVERITY, (m.group("severity") == null ? "unknown" : m.group("severity"))));
                    doc.add(new SortedSetDocValuesFacetField(THREAD, (m.group("thread") == null ? "unknown" : m.group("thread"))));
                    doc.add(new SortedSetDocValuesFacetField(LOGGER, (m.group("logger") == null ? "unknown" : m.group("logger"))));
                } catch (Exception e) {
                    throw new ParsingEventException("Error parsing line: " + e.getMessage(), e);
                }
            }
            doc.add(new TextField(FIELD_CONTENT, event.getText(), Field.Store.YES));
            return doc;
        }

        @Override
        public void close() throws IOException {
            IOUtils.close(indexWriter);
            IOUtils.close(searcherManager);
            IOUtils.close(indexDirectory);
        }

        public List<XYChart.Data<ZonedDateTime, String>> search(long start, long end, Map<String, Collection<String>> facets, String query) throws IOException {
            searcherManager.maybeRefreshBlocking();
            IndexSearcher searcher = searcherManager.acquire();
            try {
                Query q = LongPoint.newRangeQuery(TIMESTAMP, start, end);
                var l = new ArrayList<XYChart.Data<ZonedDateTime, String>>();
                var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false));
                var drill = new DrillSideways(searcher, facetsConfig, this.state);
                var dq = new DrillDownQuery(facetsConfig);
                for (var facet : facets.entrySet()) {
                    for (var label : facet.getValue()) {
                        dq.add(facet.getKey(), label);
                    }
                }
                DrillSideways.DrillSidewaysResult results;
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(dq, q, null, prefs.hitsPerPage.get().intValue(), sort, false);
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
            } finally {
                searcherManager.release(searcher);
            }
        }
    }

}
