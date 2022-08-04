/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

public class LogFileIndex extends Index<String> {
    private static final Logger logger = Logger.create(LogFileIndex.class);

    private final Cache<String, SearchHitsProcessor> facetResultCache;
    private final Cache<String, SearchHitsProcessor> hitResultCache;

    public LogFileIndex() throws IOException {
        super();
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
    }

    @Override
    protected FacetsConfig initializeFacetsConfig(FacetsConfig facetsConfig) {
        facetsConfig.setRequireDimCount(SEVERITY, true);
        facetsConfig.setDrillDownTermsIndexing(SEVERITY, FacetsConfig.DrillDownTermsIndexing.ALL);
        return super.initializeFacetsConfig(facetsConfig);
    }


    public TimeSeriesProcessor<SearchHit> search(long start,
                                                 long end,
                                                 Map<String, Collection<String>> facets,
                                                 List<String> filters,
                                                 String query,
                                                 int page,
                                                 ZoneId zoneId,
                                                 boolean ignoreCache) throws Exception {
        return getIndexMonitor().read().lock(() -> {
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

            var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
            var drillDownQuery = new DrillDownQuery(facetsConfig, filterQuery);
            for (var facet : facets.entrySet()) {
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
                DrillSideways.DrillSidewaysResult results;
                try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                    results = drill.search(drillDownQuery, collector);
                }
                try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                    logger.perf(() -> String.format("%s for entry %s", ignoreCache ? "Hit cache was explicitly bypassed" : "Hit cache miss", k));
                    var proc = new SearchHitsProcessor();
                    var samples = new ArrayList<XYChart.Data<ZonedDateTime, SearchHit>>();
                    var severityFacet = makeFacetResult(SEVERITY, results.facets, facets);
                    var pathFacet = makeFacetResult(PATH, results.facets, facets);
                    var topDocs = collector.topDocs(skip, pageSize);
                    logger.debug("collector.getTotalHits() = " + collector.getTotalHits());
                    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                        var hit = topDocs.scoreDocs[i];
                        var doc = searcher.doc(hit.doc, Set.of(TIMESTAMP, SEVERITY, PATH, FIELD_CONTENT));
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
                                    .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                    .toList());
                    proc.addFacetResults(SEVERITY,
                            severityFacet.values()
                                    .stream()
                                    .sorted(Comparator.comparingInt(FacetEntry::getNbOccurrences).reversed())
                                    .toList());
                    proc.setTotalHits(collector.getTotalHits());
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
            //   if (hitProc instanceof SearchHitsProcessor searchHitProc && facetProc instanceof SearchHitsProcessor facetHitProc) {
            hitProc.mergeFacetResults(facetProc);
            //  }
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
            severities =hitProc.getFacetResults().get(SEVERITY).stream().map(FacetEntry::getLabel).toList();
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
        return proc;
    }

    @Override
    public void close() throws IOException {
        hitResultCache.invalidateAll();
        facetResultCache.invalidateAll();
        super.close();
    }
}
