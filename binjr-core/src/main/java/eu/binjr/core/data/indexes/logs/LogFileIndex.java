/*
 *    Copyright 2020-2021 Frederic Thevenet
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

package eu.binjr.core.data.indexes.logs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.data.indexes.SearchHitsProcessor;
import eu.binjr.core.data.indexes.Searchable;
import eu.binjr.core.data.indexes.parser.EventParser;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.LongProperty;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

public class LogFileIndex implements Searchable {
    public static final String TIMESTAMP = "timestamp";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FIELD_CONTENT = "content";
    public static final String PATH = "filePath";
    private static final Logger logger = Logger.create(LogFileIndex.class);
    public static final String DOC_URI = "docUri";
    protected final UserPreferences prefs = UserPreferences.getInstance();
    private final Directory indexDirectory;
    private final Directory taxonomyDirectory;
    private final TaxonomyWriter taxonomyWriter;
    private final IndexWriter indexWriter;
    private final FacetsConfig facetsConfig;
    private final Path indexDirectoryPath;
    private final ReadWriteLockHelper indexLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());
    private final ExecutorService parsingThreadPool;
    private final int parsingThreadsNumber;
    private TaxonomyReader taxonomyReader;
    private DirectoryReader indexReader;
    private IndexSearcher searcher;
    private final Cache<String, SearchHitsProcessor> facetResultCache;
    private final Cache<String, SearchHitsProcessor> hitResultCache;

    public LogFileIndex() throws IOException {
        this.facetResultCache = Caffeine.newBuilder()
                .maximumSize(prefs.facetResultCacheEntries.get().intValue())
                .build();
        this.hitResultCache = Caffeine.newBuilder()
                .maximumSize(prefs.hitResultCacheEntries.get().intValue())
                .build();
        this.parsingThreadsNumber = prefs.parsingThreadNumber.get().intValue() < 1 ?
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1) :
                Math.min(Runtime.getRuntime().availableProcessors(), prefs.parsingThreadNumber.get().intValue());
        AtomicInteger threadNum = new AtomicInteger(0);
        this.parsingThreadPool = Executors.newFixedThreadPool(parsingThreadsNumber, r -> {
            Thread thread = new Thread(r);
            thread.setName("parsing-thread-" + threadNum.incrementAndGet());
            return thread;
        });

        switch (prefs.indexLocation.get()) {
            case MEMORY:
                indexDirectory = new ByteBuffersDirectory();
                taxonomyDirectory = new ByteBuffersDirectory();
                logger.debug("Lucene index directory stored on the Java Heap");
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
                logger.debug("Lucene index directory stored at " + indexDirectoryPath);
                if (indexDirectory instanceof MMapDirectory mmapDir) {
                    logger.debug("Use unmap:" + mmapDir.getUseUnmap());
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
        facetsConfig.setDrillDownTermsIndexing(PATH, FacetsConfig.DrillDownTermsIndexing.ALL);
        facetsConfig.setRequireDimCount(PATH, true);
        facetsConfig.setDrillDownTermsIndexing(PATH, FacetsConfig.DrillDownTermsIndexing.ALL);
        logger.debug(() -> "New indexer initialized at " + indexDirectoryPath +
                " using " + parsingThreadsNumber + " parsing indexing threads");
        logger.debug(() -> facetsConfig.getDimConfigs().entrySet().stream()
                .map(e -> "path= " + e.getKey() +
                        " field= " + e.getValue().indexFieldName +
                        " multivalued=" + e.getValue().multiValued +
                        " hierarchical=" + e.getValue().hierarchical +
                        " requireDimCount=" + e.getValue().requireDimCount)
                .collect(Collectors.joining("\n")));
    }

    @Override
    public void add(String path, InputStream ias, EventParser parser, LongProperty progress) throws IOException {
        add(path, ias, true, parser, progress);
    }

    @Override
    public void add(String path, InputStream ias, boolean commit, EventParser parser, LongProperty progress) throws IOException {
        try (Profiler ignored = Profiler.start("Clear docs from " + path, logger::perf)) {
            indexWriter.deleteDocuments(new Term(DOC_URI, path));
        }
        try (Profiler ignored = Profiler.start("Indexing " + path, logger::perf)) {
            var n = new AtomicLong(0);
            try (Profiler p = Profiler.start(e -> logger.perf("Parsed and indexed " + n.get() + " lines: " + e.toMilliString()))) {
                final AtomicLong nbLogEvents = new AtomicLong(0);
                final AtomicBoolean taskDone = new AtomicBoolean(false);
                final AtomicBoolean taskAborted = new AtomicBoolean(false);
                final BlockingQueue<ParsedEvent> queue = new LinkedBlockingQueue<>(prefs.blockingQueueCapacity.get().intValue());
                final List<Future<Integer>> results = new ArrayList<>();

                for (int i = 0; i < parsingThreadsNumber; i++) {
                    results.add(parsingThreadPool.submit(() -> {
                        logger.trace(() -> "Starting parsing worker on thread " + Thread.currentThread().getName());
                        int nbEventProcessed = 0;
                        do {
                            List<ParsedEvent> todo = new ArrayList<>();
                            var drained = queue.drainTo(todo, prefs.parsingThreadDrainSize.get().intValue());
                            if (drained == 0 && queue.size() == 0) {
                                // Park the thread for a while before polling again
                                // as is it likely that producer is done.
                                try {
                                    Thread.sleep(10);
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
                                // Signal that worker thread was aborted
                                taskAborted.set(true);
                                // Clears the blocking queue of all remaining tasks so that other workers finish ASAP
                                queue.clear();
                                throw t;
                            }
                        } while (!taskDone.get() && !Thread.currentThread().isInterrupted());
                        return nbEventProcessed;
                    }));
                }
                var aggregator = parser.aggregator();
                try (var reader = new BufferedReader(new InputStreamReader(ias, StandardCharsets.UTF_8))) {
                    String line;
                    int charRead = 0;
                    while (!taskAborted.get() && (line = reader.readLine()) != null) {
                        charRead += line.length();
                        if (charRead >= 10240) {
                            progress.set(progress.get() + charRead);
                            charRead = 0;
                        }
                        aggregator.yield(n.incrementAndGet(), line).ifPresent(CheckedLambdas.wrap(queue::put));
                    }
                    while (!taskAborted.get() && queue.size() > 0) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Put to queue interrupted", e);
                    Thread.currentThread().interrupt();
                } finally {
                    taskDone.set(true);
                }

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
                // Don't forget the last log line buffered in the aggregator
                aggregator.tail().ifPresent(CheckedLambdas.wrap(event -> {
                    addLogEvent(path, event);
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

    @Override
    public TimeRange getTimeRangeBoundaries(List<String> files, ZoneId zoneId) throws IOException {
        return indexLock.read().lock(() -> {
            ZonedDateTime beginning = getTimeRangeBoundary(false, files, zoneId);
            ZonedDateTime end = getTimeRangeBoundary(true, files, zoneId);
            return (TimeRange.of(
                    beginning != null ? beginning : ZonedDateTime.now().minusHours(24),
                    end != null ? end : ZonedDateTime.now()));
        });
    }

    @Override
    public SearchHitsProcessor search(long start,
                                      long end,
                                      Map<String, Collection<String>> params,
                                      String query,
                                      int page,
                                      ZoneId zoneId,
                                      boolean ignoreCache) throws Exception {
        return indexLock.read().lock(() -> {
            Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
            final Query filterQuery;
            if (query != null && !query.isBlank()) {
                logger.trace("Query text=" + query);
                QueryParser parser = new QueryParser(FIELD_CONTENT, new StandardAnalyzer());
                filterQuery = new BooleanQuery.Builder()
                        .add(rangeQuery, BooleanClause.Occur.FILTER)
                        .add(parser.parse(query), BooleanClause.Occur.FILTER)
                        .build();
            } else {
                filterQuery = rangeQuery;
            }
            var logs = new ArrayList<XYChart.Data<ZonedDateTime, SearchHit>>();
            var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
            var drillDownQuery = new DrillDownQuery(facetsConfig, filterQuery);
            for (var facet : params.entrySet()) {
                for (var label : facet.getValue()) {
                    drillDownQuery.add(facet.getKey(), label);
                }
            }
            var pageSize = prefs.hitsPerPage.get().intValue();
            var skip = page * pageSize;
            var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                    new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
            TopFieldCollector collector = TopFieldCollector.create(sort, skip + pageSize, Integer.MAX_VALUE);
            logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
            String facetCacheKey = drillDownQuery.toString(FIELD_CONTENT);
            String hitCacheKey = facetCacheKey + "_" + page;
            Function<String, SearchHitsProcessor> fillHitResultCache = CheckedLambdas.wrap(k -> {
                var proc = new SearchHitsProcessor();
                DrillSideways.DrillSidewaysResult results;
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(drillDownQuery, collector);
                }
                try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                    var severityFacet = makeFacetResult(SEVERITY, results.facets, params);
                    var pathFacet = makeFacetResult(PATH, results.facets, params);
                    var topDocs = collector.topDocs(skip, pageSize);
                    logger.debug("collector.getTotalHits() = " + collector.getTotalHits());
                    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                        var hit = topDocs.scoreDocs[i];
                        var doc = searcher.doc(hit.doc);
                        var severity = severityFacet.get(doc.get(SEVERITY));
                        var path = pathFacet.get(doc.get(PATH));
                        logs.add(new XYChart.Data<>(
                                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), zoneId),
                                new SearchHit(doc.get(FIELD_CONTENT) + "\n",
                                        severity != null ? severity : new FacetEntry(SEVERITY, "Unknown", 0),
                                        path != null ? path : new FacetEntry(PATH, "Unknown", 0))));
                    }
                    proc.addFacetResults(PATH,
                            pathFacet.values()
                                    .stream()
                                    .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                    .toList());
                    proc.addFacetResults(SEVERITY,
                            severityFacet.values()
                                    .stream()
                                    .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                    .toList());
                    proc.setTotalHits(collector.getTotalHits());
                    proc.setHitsPerPage(pageSize);
                    proc.setData(logs);
                }
                logger.perf(() -> String.format("%s for entry %s", ignoreCache ? "Hit cache was explicitly bypassed" : "Hit cache miss", k));
                return proc;
            });
            SearchHitsProcessor hitProc;
            if (ignoreCache) {
                hitResultCache.invalidate(hitCacheKey);
            }
            hitProc = hitResultCache.get(hitCacheKey, fillHitResultCache);

            Function<String, SearchHitsProcessor> doFacetSearch = CheckedLambdas.wrap(k -> {
                var proc = new SearchHitsProcessor();
                try (Profiler p = Profiler.start("Retrieving facets", logger::perf)) {
                    var ranges = computeRanges(start, end);
                    var severities = params.entrySet().stream()
                            .filter(e -> e.getKey().equals(SEVERITY))
                            .flatMap(e -> e.getValue().stream())
                            .toList();
                    if (severities.isEmpty()) {
                        severities = hitProc.getFacetResults().get(SEVERITY).stream().map(FacetEntry::getLabel).toList();
                    }
                    for (var severityLabel : severities) {
                        var q = new DrillDownQuery(facetsConfig, filterQuery);
                        q.add(TIMESTAMP, rangeQuery);
                        for (var facet : params.entrySet()) {
                            if (facet.getKey().equals(SEVERITY)) {
                                q.add(facet.getKey(), severityLabel);
                            } else {
                                for (var label : facet.getValue()) {
                                    q.add(facet.getKey(), label);
                                }
                            }
                        }
                        FacetsCollector fc = new FacetsCollector();
                        FacetsCollector.search(searcher, q, 0, fc);
                        Facets facets = new LongRangeFacetCounts(TIMESTAMP, fc, ranges);
                        proc.addFacetResults(TIMESTAMP + "_" + severityLabel,
                                makeFacetResult(TIMESTAMP, facets, params).values());
                    }
                }
                logger.perf(() -> String.format("%s for entry %s", ignoreCache ? "Facet cache was explicitly bypassed" : "Facet cache miss", k));
                return proc;
            });
            SearchHitsProcessor facetProc;
            if (ignoreCache) {
                facetResultCache.invalidate(facetCacheKey);
            }
            facetProc = facetResultCache.get(facetCacheKey, doFacetSearch);

            hitProc.mergeFacetResults(facetProc);
            return hitProc;
        });
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(taxonomyReader);
        IOUtils.close(indexReader);
        IOUtils.close(taxonomyWriter);
        IOUtils.close(indexWriter);
        IOUtils.close(indexDirectory);
        hitResultCache.invalidateAll();
        facetResultCache.invalidateAll();
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

    private LongRange[] computeRanges(long start, long end) {
        long durationMs = end - start;
        int nbBuckets = (int) Math.min(prefs.logHeatmapNbBuckets.get().intValue(), durationMs);
        double intervalLength = durationMs / (double) nbBuckets;
        LongRange[] ranges = new LongRange[nbBuckets];
        for (int i = 0; i < nbBuckets; i++) {
            long bucket_start = Math.round(start + i * intervalLength);
            long bucket_end = Math.round((start + i * intervalLength) + intervalLength);
            ranges[i] = new LongRange(String.format("%d;%d", bucket_start, bucket_end), bucket_start, false, bucket_end, true);
        }
        return ranges;
    }

    private void addLogEvent(String path, ParsedEvent event) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(DOC_URI, path, Field.Store.NO));
        doc.add(new TextField(FIELD_CONTENT, event.getText(), Field.Store.YES));
        doc.add(new SortedNumericDocValuesField(LINE_NUMBER, event.getSequence()));
        var millis = event.getTimestamp().toInstant().toEpochMilli();
        doc.add(new LongPoint(TIMESTAMP, millis));
        doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
        doc.add(new StoredField(TIMESTAMP, millis));
        doc.add(new FacetField(PATH, path));
        doc.add(new StoredField(PATH, path));
        String severity = event.getSections().get(SEVERITY) == null ? "unknown" : event.getSections().get(SEVERITY).toLowerCase();
        doc.add(new FacetField(SEVERITY, severity));
        doc.add(new StoredField(SEVERITY, severity));
        // add all other sections as prefixed search fields
        event.getSections().entrySet().stream().filter(e -> !e.getKey().equals(SEVERITY)).forEach(e -> {
            doc.add(new TextField(e.getKey(), e.getValue(), Field.Store.NO));
        });
        indexWriter.addDocument(facetsConfig.build(taxonomyWriter, doc));
    }

    private ZonedDateTime getTimeRangeBoundary(boolean getMax, List<String> files, ZoneId zoneId) throws IOException {
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
                    new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, getMax)),
                    false);
            if (top.hits.scoreDocs.length > 0) {
                return ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(Long.parseLong(
                                searcher.doc(top.hits.scoreDocs[0].doc).get(TIMESTAMP))).plusMillis(getMax ? 1 : -1), zoneId);
            }
            return null;
        });
    }

    private Map<String, FacetEntry> makeFacetResult(String facetName, Facets facets, Map<String, Collection<String>> params) throws IOException {
        var facetEntryMap = new TreeMap<String, FacetEntry>();
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

}
