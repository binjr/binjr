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
import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
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
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_CONTENT = "content";
    protected final LogsAdapterPreferences prefs = (LogsAdapterPreferences) getAdapterInfo().getPreferences();
    private final Map<String, LogFileIndex> indexes = new ConcurrentHashMap<>();
    protected Path rootPath;
    private FileSystemBrowser fileBrowser;
    private String[] folderFilters;
    private String[] fileExtensionsFilters;


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
        } catch (IOException e) {
            throw new CannotInitializeDataAdapterException("Could not create file system browser instance", e);
        }


    }


    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> configNode = new FilterableTreeItem<>(
                new LogFilesBinding.Builder()
                        .withLabel("Log Files")
                        .withAdapter(this)
                        .build());
        attachConfigFilesTree(configNode);
        return configNode;
    }

    private void attachConfigFilesTree(FilterableTreeItem<SourceBinding> configNode) throws DataAdapterException {
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
        for (var info : seriesInfos) {
            try {
                var proc = new TextProcessor();
                LogFilesBinding binding = (LogFilesBinding) info.getBinding();
                var idx = indexes.computeIfAbsent(binding.getPath(),
                        CheckedLambdas.wrap((CheckedFunction<String, LogFileIndex, IOException>)
                                s -> new LogFileIndex(s, fileBrowser.getData(s))));
                //  new QueryParser();
                //  QueryParser parser = new QueryParser(field, analyzer);
                //    proc.setData(List.of(new XYChart.Data<>(ZonedDateTime.now(), readTextFile(info.getBinding().getPath()))));
                var builder = new QueryBuilder(new StandardAnalyzer());
//              var tr = new Term("FIELD_CONTENT", "info");
//                var q = new TermQuery(tr);
                Query q  = LongPoint.newRangeQuery(FIELD_TIMESTAMP, start.toEpochMilli(), end.toEpochMilli());
                var t = idx.getSearcher().search(q, prefs.hitsPerPage.get().intValue());
                var l = new ArrayList<XYChart.Data<ZonedDateTime, String>>();
                for (var hit : t.scoreDocs) {
                    l.add(new XYChart.Data<>(ZonedDateTime.now(), idx.getSearcher().doc(hit.doc).get(FIELD_CONTENT) + "\n"));
                }
                proc.setData(l);
                data.put(info, proc);
            } catch (IOException e) {
                throw new DataAdapterException("Error fetching text from " + info.getBinding().getPath(), e);
            }
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
        IOUtils.closeAll(indexes.values());
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

        private final String path;
        private final Directory indexDirectory;
        private final DirectoryReader indexReader;
        private final IndexSearcher searcher;
        private Pattern pattern;
        private DateTimeFormatter dateTimeFormatter;

        public LogFileIndex(String path, InputStream ias) throws IOException {
            this.path = path;
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
            var n = new AtomicInteger(0);

            try (var indexWriter = new IndexWriter(indexDirectory, iwc)) {
                try (Profiler ignored = Profiler.start(e -> logger.perf("Indexed " + n.get() + " lines for file " + path + e.toMilliString()))) {
                    try (var reader = new BufferedReader(new InputStreamReader(ias, StandardCharsets.UTF_8))) {
                        reader.lines().parallel().forEach(CheckedLambdas.wrap(line -> {
                            indexWriter.addDocument(parseLine(line));
                            n.incrementAndGet();
                        }));
                    }
                }
                try (Profiler p = Profiler.start("Commit index", logger::perf)) {
                    indexWriter.commit();
                }
            }
            indexReader = DirectoryReader.open(indexDirectory);
            logger.info("Total indexed lines: " + indexReader.getDocCount(FIELD_CONTENT));
            searcher = new IndexSearcher(indexReader);

        }

        private Document parseLine(String line) throws ParsingEventException {
            Document doc = new Document();
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                try {
                    var timeStamp = m.group("time") == null ? ZonedDateTime.now() :
                            ZonedDateTime.parse(m.group("time").replaceAll("[/\\-:.,T]", " "), dateTimeFormatter);
                    doc.add(new LongPoint(FIELD_TIMESTAMP, timeStamp.toInstant().toEpochMilli()));
                    doc.add(new SortedSetDocValuesField("severity", new BytesRef(m.group("severity") == null ? "unknown" : m.group("severity"))));
                    doc.add(new SortedSetDocValuesField("thread", new BytesRef(m.group("thread") == null ? "unknown" : m.group("thread"))));
                    doc.add(new SortedSetDocValuesField("logger", new BytesRef(m.group("logger") == null ? "unknown" : m.group("logger"))));
                } catch (Exception e) {
                    throw new ParsingEventException("Error parsing line: " + e.getMessage(), e);
                }
            }
            doc.add(new TextField(FIELD_CONTENT, line, Field.Store.YES));
            return doc;
        }


        @Override
        public void close() throws IOException {
            IOUtils.close(indexReader);
            IOUtils.close(indexDirectory);
        }

        public IndexSearcher getSearcher() {
            return searcher;
        }
    }

}
