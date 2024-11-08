/*
 *    Copyright 2022-2024 Frederic Thevenet
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

package eu.binjr.core.data.indexes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.parser.*;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.binjr.core.data.timeseries.transform.SortTransform;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.preferences.IndexingTokenizer;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.Reader;
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
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

public class Index implements Indexable {
    public static final String TIMESTAMP = "timestamp";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FIELD_CONTENT = "content";
    public static final String PATH = "filePath";
    public static final String DOC_URI = "docUri";
    public static final float SEARCH_HIT_WEIGHT_FACTOR = 2.0f;
    private static final Logger logger = Logger.create(Index.class);
    private static final long PARK_TIME_NANO = 1_000_000L;
    protected final UserPreferences prefs = UserPreferences.getInstance();
    protected final Directory indexDirectory;
    protected final Directory taxonomyDirectory;
    protected final TaxonomyWriter taxonomyWriter;
    protected final IndexWriter indexWriter;
    protected final FacetsConfig facetsConfig;
    protected final Path indexDirectoryPath;
    protected final ExecutorService parsingThreadPool;
    protected final int parsingThreadsNumber;
    private final UserPreferences userPref = UserPreferences.getInstance();
    private final Map<String, ReloadStatus> indexedFiles = new ConcurrentHashMap<>();

    private final ReadWriteLockHelper indexLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());
    protected DirectoryReader indexReader;
    protected IndexSearcher searcher;
    protected TaxonomyReader taxonomyReader;


    private final Cache<String, SearchHitsProcessor> facetResultCache;
    private final Cache<String, SearchHitsProcessor> hitResultCache;

    public Index() throws IOException {
        this.parsingThreadsNumber = prefs.parsingThreadNumber.get().intValue() < 1 ?
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1) :
                Math.min(Runtime.getRuntime().availableProcessors(), prefs.parsingThreadNumber.get().intValue());

        this.facetResultCache = Caffeine.newBuilder()
                .recordStats()
                .maximumSize(prefs.facetResultCacheEntries.get().intValue())
                .build();

        this.hitResultCache = Caffeine.newBuilder()
                .recordStats()
                .maximumWeight(prefs.hitResultCacheMaxSizeMiB.get().longValue() * 1082768)
                .weigher((String key, TimeSeriesProcessor<SearchHit> value) -> Math.round(value.getData().stream()
                        .map(e -> e.getYValue().getText().length())
                        .reduce(Integer::sum)
                        .orElse(100) * SEARCH_HIT_WEIGHT_FACTOR))
                .build();

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
                logger.warn("[Deprecated] Lucene index directory stored on the Java Heap");
                indexDirectoryPath = null;
                break;
            case FILES_SYSTEM:
            default:
                indexDirectoryPath = Files.createTempDirectory(prefs.temporaryFilesRoot.get(), "binjr-index_");
                indexDirectory = FSDirectory.open(indexDirectoryPath.resolve("index"));
                taxonomyDirectory = FSDirectory.open(indexDirectoryPath.resolve("taxonomy"));
                logger.debug(() -> "Lucene index directory stored at " + indexDirectoryPath);
        }
        logger.debug(() -> "New indexer initialized at " + indexDirectoryPath +
                " using " + parsingThreadsNumber + " parsing indexing threads");
        IndexWriterConfig iwc = new IndexWriterConfig(new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                Map.of(FIELD_CONTENT, getContentFieldAnalyzer())));
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        this.indexWriter = new IndexWriter(indexDirectory, iwc);
        this.taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory);
        indexReader = DirectoryReader.open(indexWriter);
        searcher = new IndexSearcher(indexReader);
        facetsConfig = initializeFacetsConfig(new FacetsConfig());
        logger.debug(() -> facetsConfig.getDimConfigs().entrySet().stream()
                .map(e -> "path= " + e.getKey() +
                        " field= " + e.getValue().indexFieldName +
                        " multivalued=" + e.getValue().multiValued +
                        " hierarchical=" + e.getValue().hierarchical +
                        " requireDimCount=" + e.getValue().requireDimCount)
                .collect(Collectors.joining("\n")));
        // initial commit
        commitIndexAndTaxonomy();
    }


    Analyzer getContentFieldAnalyzer() {
        return switch (prefs.indexingTokenizer.get()) {
            case NGRAMS -> new Analyzer() {
                @Override
                protected Analyzer.TokenStreamComponents createComponents(final String fieldName) {
                    var tokenizer = new NGramTokenizer(prefs.logIndexNGramSize.get().intValue(),
                            prefs.logIndexNGramSize.get().intValue());
                    var ts = new LowerCaseFilter(tokenizer);
                    return new Analyzer.TokenStreamComponents(tokenizer::setReader, ts);
                }
            };
            case STANDARD -> new Analyzer() {
                @Override
                protected Analyzer.TokenStreamComponents createComponents(final String fieldName) {
                    final StandardTokenizer src = new StandardTokenizer();
                    TokenStream tok = new LowerCaseFilter(src);
                    return new Analyzer.TokenStreamComponents(src::setReader, tok);
                }

                @Override
                protected Reader initReader(String fieldName, Reader reader) {
                    NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
                    builder.add(".", " ");
                    NormalizeCharMap normMap = builder.build();
                    return new MappingCharFilter(normMap, reader);
                }
            };
        };
    }

    private void rewriteQuery(Query query, BooleanQuery.Builder accQuery, BooleanClause.Occur occur) throws IOException {
        if (query instanceof BooleanQuery booleanQuery) {
            for (BooleanClause booleanClause : booleanQuery) {
                rewriteQuery(booleanClause.query(), accQuery, booleanClause.occur());
            }
        } else if (query instanceof TermQuery termQuery) {
            accQuery.add(splitTermToNGrams(termQuery.getTerm()), occur);
        } else {
            int ngramSize = prefs.logIndexNGramSize.get().intValue();
            switch (query) {
                case PrefixQuery prefixQuery when prefixQuery.getPrefix().text().length() > ngramSize ->
                        accQuery.add(splitTermToNGrams(prefixQuery.getPrefix()), occur);
                case WildcardQuery wildcardQuery when wildcardQuery.getTerm().text().replace("*", "").replace("?", "").length() > ngramSize -> {
                    var subBuilder = new BooleanQuery.Builder();
                    for (var txt : wildcardQuery.getTerm().text().split("[?*]")) {
                        subBuilder.add(splitTermToNGrams(new Term(wildcardQuery.getTerm().field(), txt), true), BooleanClause.Occur.FILTER);
                    }
                    accQuery.add(subBuilder.build(), occur);
                }
                case PhraseQuery phraseQuery -> {
                    var text = Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining(" "));
                    accQuery.add(splitTermToNGrams(new Term(phraseQuery.getField(), text)), occur);
                }
                case null, default -> accQuery.add(query, occur);
            }
        }
    }

    private Query splitTermToNGrams(Term term) throws IOException {
        return splitTermToNGrams(term, prefs.logIndexAutoExpendShorterTerms.get());
    }

    private Query splitTermToNGrams(Term term, boolean autoExpend) throws IOException {
        final int nGramSize = prefs.logIndexNGramSize.get().intValue();
        if (autoExpend && term.text().length() < nGramSize) {
            // suffix term with a wildcard if shorter than ngram size
            return new PrefixQuery(new Term(term.field(), term.text()));
        } else {
            var queryBuilder = new PhraseQuery.Builder();
            try (var ts = getContentFieldAnalyzer().tokenStream(FIELD_CONTENT, term.text())) {
                ts.reset();
                var termAttribute = ts.addAttribute(CharTermAttribute.class);
                while (ts.incrementToken()) {
                    var tokenText = termAttribute.toString();
                    if (!tokenText.isEmpty()) {
                        queryBuilder.add(new Term(term.field(), tokenText));
                    }
                }
                ts.end();
            }
            if (prefs.optimizeNGramQueries.get()) {
                return new NGramPhraseQuery(nGramSize, queryBuilder.build());
            } else {
                return queryBuilder.build();
            }
        }
    }

    public TimeSeriesProcessor<SearchHit> search(long start,
                                                 long end,
                                                 Map<String, Collection<String>> facets,
                                                 String query,
                                                 int page,
                                                 ZoneId zoneId,
                                                 boolean ignoreCache) throws Exception {
        return getIndexMonitor().read().lock(() -> {
            Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
            final Query filterQuery;
            if (query != null && !query.isBlank()) {
                logger.trace(() -> "Query text=" + query);
                Query userQuery;
                if (prefs.indexingTokenizer.get() == IndexingTokenizer.NGRAMS) {
                    var builder = new BooleanQuery.Builder();
                    var parser = new StandardQueryParser(new Analyzer() {
                        @Override
                        protected TokenStreamComponents createComponents(String fieldName) {
                            return new Analyzer.TokenStreamComponents(new CharTokenizer() {
                                @Override
                                protected boolean isTokenChar(int c) {
                                    return true;
                                }
                            });
                        }

                        @Override
                        protected TokenStream normalize(String fieldName, TokenStream in) {
                            return new LowerCaseFilter(in);
                        }
                    });
                    rewriteQuery(parser.parse(query, FIELD_CONTENT), builder, BooleanClause.Occur.FILTER);
                    userQuery = builder.build();
                } else {
                    var parser = new StandardQueryParser(getContentFieldAnalyzer());
                    userQuery = parser.parse(query, FIELD_CONTENT);
                }
                filterQuery = new BooleanQuery.Builder()
                        .add(rangeQuery, BooleanClause.Occur.FILTER)
                        .add(userQuery, BooleanClause.Occur.FILTER)
                        .build();
            } else {
                filterQuery = rangeQuery;
            }

            var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
            var drillDownQuery = new DrillDownQuery(facetsConfig, new ConstantScoreQuery(filterQuery));
            for (var facet : facets.entrySet()) {
                for (var label : facet.getValue()) {
                    logger.debug(() -> "Add facet [" + facet.getKey() + "] = " + label);
                    drillDownQuery.add(facet.getKey(), label);
                }
            }
            var pageSize = prefs.hitsPerPage.get().intValue();
            var skip = page * pageSize;
            var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                    new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
            var collectorManager = new TopFieldCollectorManager(sort, skip + pageSize, Integer.MAX_VALUE);
            logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
            String facetCacheKey = drillDownQuery.toString(FIELD_CONTENT);
            String hitCacheKey = facetCacheKey + "_" + page;
            Function<String, SearchHitsProcessor> fillHitResultCache = CheckedLambdas.wrap(k -> {
                DrillSideways.ConcurrentDrillSidewaysResult<TopFieldDocs> results;
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(drillDownQuery, collectorManager);
                }
                try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                    logger.perf(() -> String.format("%s for entry %s", ignoreCache ? "Hit cache was explicitly bypassed" : "Hit cache miss", k));
                    int procCapacity = (int) Math.min(results.collectorResult.totalHits.value(), pageSize);
                    var proc = new SearchHitsProcessor();
                    var samples = new ArrayList<XYChart.Data<ZonedDateTime, SearchHit>>(procCapacity);
                    var severityFacet = makeFacetResult(SEVERITY, results.facets, facets);
                    var pathFacet = makeFacetResult(PATH, results.facets, facets);
                    var topDocs = TopDocs.merge(skip, pageSize, new TopDocs[]{results.collectorResult});
                    for (var hit : topDocs.scoreDocs) {
                        var doc = searcher.storedFields().document(hit.doc, Set.of(TIMESTAMP, SEVERITY, PATH, FIELD_CONTENT));
                        var severity = severityFacet.get(doc.get(SEVERITY));
                        var path = pathFacet.get(doc.get(PATH));
                        samples.add(new XYChart.Data<>(
                                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), zoneId),
                                new SearchHit(doc.get(FIELD_CONTENT) + "\n",
                                        severity != null ? severity : new FacetEntry(SEVERITY, "Unknown", 0),
                                        path != null ? path : new FacetEntry(PATH, "Unknown", 0))));
                    }
                    proc.addFacetResults(PATH,
                            pathFacet.values()
                                    .stream()
                                    .sorted(Comparator.comparingInt(FacetEntry::occurrences).reversed())
                                    .toList());
                    proc.addFacetResults(SEVERITY,
                            severityFacet.values()
                                    .stream()
                                    .sorted(Comparator.comparingInt(FacetEntry::occurrences).reversed())
                                    .toList());
                    proc.setTotalHits(results.collectorResult.totalHits.value());
                    proc.setHitsPerPage(pageSize);
                    proc.setData(samples);
                    return proc;
                }
            });
            SearchHitsProcessor hitProc;
            if (ignoreCache) {
                hitResultCache.invalidate(hitCacheKey);
            }
            hitProc = hitResultCache.get(hitCacheKey, fillHitResultCache);
            logger.perf(() -> printCacheStats("Hit result cache stats", hitResultCache.stats()));
            Function<String, SearchHitsProcessor> fillFacetResultCache = CheckedLambdas.wrap(k -> {

                try (Profiler p = Profiler.start("Retrieving facets", logger::perf)) {
                    logger.perf(() -> String.format("%s for entry %s", ignoreCache ? "Facet cache was explicitly bypassed" : "Facet cache miss", k));
                    return retrieveFacets(start, end, facets, rangeQuery, filterQuery, hitProc);
                }
            });
            SearchHitsProcessor facetProc;
            if (ignoreCache) {
                facetResultCache.invalidate(facetCacheKey);
            }
            facetProc = facetResultCache.get(facetCacheKey, fillFacetResultCache);
            hitProc.mergeFacetResults(facetProc);
            return hitProc;
        });
    }


    private SearchHitsProcessor retrieveFacets(long start,
                                               long end,
                                               Map<String, Collection<String>> params,
                                               Query rangeQuery,
                                               Query filterQuery,
                                               SearchHitsProcessor hitProc) throws IOException {
        var proc = new SearchHitsProcessor();
        var ranges = computeRanges(start, end, prefs.logHeatmapNbBuckets.get().intValue());
        var severities = params.entrySet().stream()
                .filter(e -> e.getKey().equals(SEVERITY))
                .flatMap(e -> e.getValue().stream())
                .toList();
        if (severities.isEmpty()) {
            severities = hitProc.getFacetResults().get(SEVERITY).stream().map(FacetEntry::label).toList();
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
            var fcm = new FacetsCollectorManager();
            var result = FacetsCollectorManager.search(searcher, q, 0, fcm);
            Facets facets = new LongRangeFacetCounts(TIMESTAMP, result.facetsCollector(), ranges);
            proc.addFacetResults(TIMESTAMP + "_" + severityLabel,
                    makeFacetResult(TIMESTAMP, facets, params).values());
        }
        return proc;
    }


    private FacetsConfig initializeFacetsConfig(FacetsConfig facetsConfig) {
        facetsConfig.setRequireDimCount(SEVERITY, true);
        facetsConfig.setDrillDownTermsIndexing(SEVERITY, FacetsConfig.DrillDownTermsIndexing.ALL);
        facetsConfig.setRequireDimCount(PATH, true);
        facetsConfig.setDrillDownTermsIndexing(PATH, FacetsConfig.DrillDownTermsIndexing.ALL);
        return facetsConfig;
    }

    public <T> void add(String path,
                        T source,
                        boolean commit,
                        EventFormat<T> eventFormat,
                        EnrichDocumentFunction enrichDocumentFunction,
                        LongProperty progress,
                        Property<ReloadStatus> cancellationRequested) throws IOException {
        add(path, source, commit, eventFormat, enrichDocumentFunction, progress, cancellationRequested, (root, event) -> path, (ignore) -> List.of(path));
    }

    public <T> void add(String path,
                        T source,
                        boolean commit,
                        EventFormat<T> eventFormat,
                        EnrichDocumentFunction enrichDocumentFunction,
                        LongProperty progress,
                        Property<ReloadStatus> cancellationRequested,
                        BiFunction<String, ParsedEvent, String> computePathFacetValue,
                        Function<T, List<String>> computeDeletePaths) throws IOException {
        try (Profiler ignored = Profiler.start("Clear docs from " + path, logger::perf)) {
            indexWriter.deleteDocuments(computeDeletePaths.apply(source).stream().map(s -> new Term(DOC_URI, s)).toArray(Term[]::new));
        }
        try (Profiler ignored = Profiler.start("Indexing " + path, logger::perf)) {
            final AtomicLong nbLogEvents = new AtomicLong(0);
            try (Profiler p = Profiler.start(e -> logger.perf("Parsed and indexed " + nbLogEvents.get() + " events: " + e.toMilliString()))) {

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
                            if (drained == 0 && queue.isEmpty()) {
                                // Park the thread for a while before polling again
                                // as is it likely that producer is done.
                                LockSupport.parkNanos(PARK_TIME_NANO);
                            }
                            try {
                                for (var logEvent : todo) {
                                    String pathFacetValue = computePathFacetValue.apply(path, logEvent);
                                    var doc = new Document();
                                    doc.add(new StringField(DOC_URI, pathFacetValue, Field.Store.NO));
                                    doc.add(new TextField(FIELD_CONTENT, logEvent.getText(), Field.Store.YES));
                                    doc.add(new SortedNumericDocValuesField(LINE_NUMBER, logEvent.getSequence()));
                                    var millis = logEvent.getTimestamp().toInstant().toEpochMilli();
                                    doc.add(new LongPoint(TIMESTAMP, millis));
                                    doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
                                    doc.add(new StoredField(TIMESTAMP, millis));
                                    doc.add(new FacetField(PATH, pathFacetValue));
                                    doc.add(new StoredField(PATH, pathFacetValue));
                                    indexWriter.addDocument(facetsConfig.build(taxonomyWriter, enrichDocumentFunction.apply(doc, logEvent)));
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
                try (var aggregator = eventFormat.parse(source)) {
                    progress.bind(aggregator.progressIndicator());
                    for (var event : aggregator) {
                        if (taskAborted.get()) {
                            cancellationRequested.setValue(ReloadStatus.ABORTED);
                            break;
                        }
                        if (cancellationRequested.getValue() == ReloadStatus.CANCELED) {
                            break;
                        }
                        if (event == null) {
                            cancellationRequested.setValue(ReloadStatus.NO_RESULTS);
                            break;
                        }
                        queue.put(event);
                    }
                    while (!taskAborted.get() && !queue.isEmpty()) {
                        LockSupport.parkNanos(PARK_TIME_NANO);
                    }
                } catch (InterruptedException e) {
                    logger.error("Put to queue interrupted", e);
                    Thread.currentThread().interrupt();
                } finally {
                    taskDone.set(true);
                    progress.unbind();
                }
                for (Future<Integer> f : results) {
                    //signal exceptions that may have happened on thread pool
                    try {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Thread added " + f.get() + " log event to index");
                        }
                        nbLogEvents.addAndGet(f.get());
                    } catch (InterruptedException e) {
                        logger.error("Getting result from worker was interrupted", e);
                    } catch (Exception e) {
                        //rethrow execution exceptions
                        throw new IOException("Error parsing logEvent", e);
                    }
                }
            }
            if (commit) {
                commitIndexAndTaxonomy();
            }
        }
    }

    public void commitIndexAndTaxonomy() throws IOException {
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

    public ReadWriteLockHelper getIndexMonitor() {
        return indexLock;
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
    public void close() throws IOException {
        hitResultCache.invalidateAll();
        facetResultCache.invalidateAll();
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

    public static Map<String, FacetEntry> makeFacetResult(String facetName,
                                                          Facets facets,
                                                          Map<String, Collection<String>> params) throws IOException {
        return makeFacetResult(facetName, facets, params, 0);
    }

    public static Map<String, FacetEntry> makeFacetResult(String facetName,
                                                          Facets facets,
                                                          Map<String, Collection<String>> params,
                                                          int minOccurrence) throws IOException {
        var facetEntryMap = new TreeMap<String, FacetEntry>();
        var synthesis = facets.getAllChildren(facetName); //facets.getTopChildren(100, facetName);
        var labels = new ArrayList<String>();
        if (synthesis != null) {
            for (var f : synthesis.labelValues) {
                if (f.value.intValue() >= minOccurrence)
                    facetEntryMap.put(f.label, new FacetEntry(facetName, f.label, f.value.intValue()));
                labels.add(f.label);
            }
            // Add facets labels used in query if not present in the result
            params.getOrDefault(facetName, List.of()).stream()
                    .filter(l -> !labels.contains(l))
                    .map(l -> new FacetEntry(facetName, l, 0))
                    .forEach(f -> facetEntryMap.put(f.label(), f));
        }
        return facetEntryMap;
    }

    public static String printCacheStats(String title, CacheStats stats) {
        return String.format("%s: requestCount=%d hitCount=%d hitRate=%f missCount=%d evictionCount=%d evictionWeight=%d",
                title,
                stats.requestCount(),
                stats.hitCount(),
                stats.hitRate(),
                stats.missCount(),
                stats.evictionCount(),
                stats.evictionWeight()
        );
    }

    public static LongRange[] computeRanges(long start, long end, int numBuckets) {
        long durationMs = end - start;
        int nbBuckets = (int) Math.min(numBuckets, durationMs);
        double intervalLength = durationMs / (double) nbBuckets;
        LongRange[] ranges = new LongRange[nbBuckets];
        for (int i = 0; i < nbBuckets; i++) {
            long bucket_start = Math.round(start + i * intervalLength);
            long bucket_end = Math.round((start + i * intervalLength) + intervalLength);
            ranges[i] = new LongRange(String.valueOf(numBuckets * 10 + i), bucket_start, false, bucket_end, true);
        }
        return ranges;
    }

    public Map<String, FacetEntry> getAllPaths() throws IOException {
        return getPaths(0, new MatchAllDocsQuery());
    }

    public Map<String, FacetEntry> getPaths(int min, String query) throws Exception {
        return getIndexMonitor().read().lock(() -> {
            var parser = new StandardQueryParser();
            try (Profiler p = Profiler.start(() -> "Retrieved all paths", logger::perf)) {
                var fcm = new FacetsCollectorManager();
                var result = FacetsCollectorManager.search(searcher, parser.parse(query, FIELD_CONTENT), 0, fcm);
                Facets facets = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, result.facetsCollector());
                return makeFacetResult(PATH, facets, Map.of(), min);
            }
        });
    }

    public Map<String, FacetEntry> getPaths(int min, Query query) throws IOException {
        return getIndexMonitor().read().lock(() -> {
            try (Profiler p = Profiler.start(() -> "Retrieved all paths", logger::perf)) {
                var fcm = new FacetsCollectorManager();
                var result = FacetsCollectorManager.search(searcher, query, 0, fcm);
                Facets facets = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, result.facetsCollector());
                return makeFacetResult(PATH, facets, Map.of(), min);
            }
        });
    }

    public long search(long start,
                       long end,
                       Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> seriesToFill,
                       ZoneId zoneId,
                       boolean ignoreCache) throws Exception {
        return getIndexMonitor().read().lock(() -> {
            Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
            var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
            var drillDownQuery = new DrillDownQuery(facetsConfig, rangeQuery);
            seriesToFill.keySet()
                    .stream()
                    .map(ts -> ts.getBinding().getPath())
                    .distinct()
                    .forEach(path -> drillDownQuery.add(PATH, path));
            var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                    new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
            var reduceTransform = new LargestTriangleThreeBucketsTransform(userPref.downSamplingThreshold.get().intValue());
            var sortTransform = new SortTransform<Double>();
            AtomicLong hitsCollected = new AtomicLong(0);
            int pageSize = prefs.numIdxMaxPageSize.get().intValue();
            FieldDoc lastHit = null;
            try (Profiler p = Profiler.start(() -> "Retrieved " + hitsCollected.get() + " samples for " + seriesToFill.size() + " series", logger::perf)) {
                for (int pageNumber = 0; true; pageNumber++) {
                    var collectorManager = (lastHit == null) ?
                            new TopFieldCollectorManager(sort, pageSize, Integer.MAX_VALUE) :
                            new TopFieldCollectorManager(sort, pageSize, lastHit, Integer.MAX_VALUE);
                    logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
                    DrillSideways.ConcurrentDrillSidewaysResult<TopFieldDocs> result;
                    try (Profiler ignored = Profiler.start("Executing query for page " + pageNumber, logger::debug)) {
                        result = drill.search(drillDownQuery, collectorManager);
                    }
                    var fieldsToLoad = seriesToFill.keySet()
                            .stream()
                            .map(k -> k.getBinding().getLabel())
                            .collect(Collectors.toSet());
                    fieldsToLoad.add(TIMESTAMP);
                    Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> pageDataBuffer = new ConcurrentHashMap<>();
                    // fill the buffer with data for the current page
                    var nbHits = result.collectorResult.scoreDocs.length;
                    var lastHitAcc = new FieldDoc[1];
                    var timestampAcc = new ZonedDateTime[]{ZonedDateTime.of(-999999999, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())};
                    Stream<ScoreDoc> scoreDocStream = Arrays.stream(result.collectorResult.scoreDocs);
                    if (prefs.useParallelIndexFetch.get()) {
                        scoreDocStream = scoreDocStream.parallel();
                    }
                    scoreDocStream.forEach(CheckedLambdas.wrap(scoreDoc -> {
                        var doc = searcher.storedFields().document(scoreDoc.doc, fieldsToLoad);
                        var timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(doc.getField(TIMESTAMP).numericValue().longValue()), zoneId);
                        if (timestamp.isAfter(timestampAcc[0])) {
                            lastHitAcc[0] = (FieldDoc) scoreDoc;
                            timestampAcc[0] = timestamp;
                        }
                        seriesToFill.forEach((info, proc) -> {
                            var field = doc.getField(info.getBinding().getLabel());
                            if (field != null) {
                                var value = field.numericValue();
                                if (value != null) {
                                    pageDataBuffer.computeIfAbsent(info, doubleTimeSeriesInfo -> new DoubleTimeSeriesProcessor(nbHits))
                                            .addSample(timestamp, value.doubleValue());
                                }
                            }
                        });
                    }));
                    lastHit = lastHitAcc[0];
                    // Apply downsampling to buffered data and append reduced dataset to processor
                    var entryStream = seriesToFill.entrySet().stream();
                    if (prefs.useParallelIndexFetch.get()) {
                        entryStream = entryStream.parallel();
                    }
                    entryStream.forEach(entry -> {
                        var pageProc = pageDataBuffer.get(entry.getKey());
                        if (pageProc != null) {
                            var fullQueryProc = entry.getValue();
                            pageProc.applyTransforms(sortTransform, reduceTransform);
                            fullQueryProc.appendData(pageProc);
                        }
                    });
                    hitsCollected.accumulateAndGet(result.collectorResult.scoreDocs.length, Long::sum);
                    // End the loop once all hits have been collected
                    if (hitsCollected.get() >= result.collectorResult.totalHits.value()) {
                        break;
                    }
                }
            }
            return hitsCollected.get();
        });
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
                                searcher.storedFields().document(top.hits.scoreDocs[0].doc).get(TIMESTAMP))).plusMillis(getMax ? 1 : -1),
                        zoneId);
            }
            return null;
        });
    }


    @Override
    public Map<String, ReloadStatus> getIndexedFiles() {
        return indexedFiles;
    }
}
