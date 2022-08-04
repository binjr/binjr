/*
 *    Copyright 2022 Frederic Thevenet
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

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import eu.binjr.common.concurrent.ReadWriteLockHelper;
import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.stream.Collectors;

public abstract class Index<T> implements Indexable<T> {
    public static final String TIMESTAMP = "timestamp";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FIELD_CONTENT = "content";
    public static final String PATH = "filePath";
    public static final String DOC_URI = "docUri";
    public static final float SEARCH_HIT_WEIGHT_FACTOR = 2.0f;
    private static final Logger logger = Logger.create(Index.class);
    protected final UserPreferences prefs = UserPreferences.getInstance();
    protected final Directory indexDirectory;
    protected final Directory taxonomyDirectory;
    protected final TaxonomyWriter taxonomyWriter;
    protected final IndexWriter indexWriter;
    protected final FacetsConfig facetsConfig;
    protected final Path indexDirectoryPath;
    protected final ExecutorService parsingThreadPool;
    protected final int parsingThreadsNumber;

    private final ReadWriteLockHelper indexLock = new ReadWriteLockHelper(new ReentrantReadWriteLock());
    protected DirectoryReader indexReader;
    protected IndexSearcher searcher;
    protected TaxonomyReader taxonomyReader;

    public Index() throws IOException {
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
                indexDirectoryPath = Files.createTempDirectory(prefs.temporaryFilesRoot.get(), "binjr-index_");
                indexDirectory = FSDirectory.open(indexDirectoryPath.resolve("index"));
                taxonomyDirectory = FSDirectory.open(indexDirectoryPath.resolve("taxonomy"));
                logger.debug("Lucene index directory stored at " + indexDirectoryPath);
                if (indexDirectory instanceof MMapDirectory mmapDir) {
                    logger.debug("Use unmap:" + mmapDir.getUseUnmap());
                }

        }
        logger.debug(() -> "New indexer initialized at " + indexDirectoryPath +
                " using " + parsingThreadsNumber + " parsing indexing threads");

        IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
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

    protected FacetsConfig initializeFacetsConfig(FacetsConfig facetsConfig) {
        facetsConfig.setRequireDimCount(PATH, true);
        facetsConfig.setDrillDownTermsIndexing(PATH, FacetsConfig.DrillDownTermsIndexing.ALL);
        return facetsConfig;
    }

    @Override
    public void add(String path,
                    InputStream ias,
                    EventFormat<T> parser,
                    EnrichDocumentFunction<T> enrichDocumentFunction,
                    LongProperty progress,
                    Property<IndexingStatus> indexingStatus) throws IOException {
        add(path, ias, true, parser, enrichDocumentFunction, progress, indexingStatus);
    }

    @Override
    public void add(String path,
                    InputStream ias,
                    boolean commit,
                    EventFormat<T> parser,
                    EnrichDocumentFunction<T> enrichDocumentFunction,
                    LongProperty progress,
                    Property<IndexingStatus> cancellationRequested) throws IOException {
        try (Profiler ignored = Profiler.start("Clear docs from " + path, logger::perf)) {
            indexWriter.deleteDocuments(new Term(DOC_URI, path));
        }
        try (Profiler ignored = Profiler.start("Indexing " + path, logger::perf)) {
            final AtomicLong nbLogEvents = new AtomicLong(0);
            try (Profiler p = Profiler.start(e -> logger.perf("Parsed and indexed " + nbLogEvents.get() + " events: " + e.toMilliString()))) {

                final AtomicBoolean taskDone = new AtomicBoolean(false);
                final AtomicBoolean taskAborted = new AtomicBoolean(false);
                final BlockingQueue<ParsedEvent<T>> queue = new LinkedBlockingQueue<>(prefs.blockingQueueCapacity.get().intValue());
                final List<Future<Integer>> results = new ArrayList<>();

                for (int i = 0; i < parsingThreadsNumber; i++) {
                    results.add(parsingThreadPool.submit(() -> {
                        logger.trace(() -> "Starting parsing worker on thread " + Thread.currentThread().getName());
                        int nbEventProcessed = 0;
                        do {
                            List<ParsedEvent<T>> todo = new ArrayList<>();
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
                                    var doc = new Document();
                                    doc.add(new StringField(DOC_URI, path, Field.Store.NO));
                                    doc.add(new TextField(FIELD_CONTENT, logEvent.getText(), Field.Store.YES));
                                    doc.add(new SortedNumericDocValuesField(LINE_NUMBER, logEvent.getSequence()));
                                    var millis = logEvent.getTimestamp().toInstant().toEpochMilli();
                                    doc.add(new LongPoint(TIMESTAMP, millis));
                                    doc.add(new SortedNumericDocValuesField(TIMESTAMP, millis));
                                    doc.add(new StoredField(TIMESTAMP, millis));
                                    doc.add(new FacetField(PATH, path));
                                    doc.add(new StoredField(PATH, path));
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
                try (var aggregator = parser.parse(ias)) {
                    progress.bind(aggregator.progressIndicator());
                    for (var event : aggregator) {
                        if (taskAborted.get()) {
                            cancellationRequested.setValue(IndexingStatus.ABORTED);
                            break;
                        }
                        if (cancellationRequested.getValue() == IndexingStatus.CANCELED) {
                            break;
                        }
                        if (event == null) {
                            cancellationRequested.setValue(IndexingStatus.NO_RESULTS);
                            break;
                        }
                        queue.put(event);
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
                    progress.unbind();
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
            }
            if (commit) {
               commitIndexAndTaxonomy();
            }
        }

    }

    private void commitIndexAndTaxonomy() throws IOException {
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

    public static Map<String, FacetEntry> makeFacetResult(String facetName, Facets facets, Map<String, Collection<String>> params)
            throws IOException {
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
            ranges[i] = new LongRange(String.format("%d;%d", bucket_start, bucket_end), bucket_start, false, bucket_end, true);
        }
        return ranges;
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


}
