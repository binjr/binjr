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
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.chart.XYChart;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

public class LogFileIndex extends Index {
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
    Analyzer getContentFieldAnalyzer() {
        if (UserPreferences.getInstance().useNGramTokenization.get()) {
            return new Analyzer() {
                @Override
                protected Analyzer.TokenStreamComponents createComponents(final String fieldName) {
                    var tokenizer = new NGramTokenizer(prefs.logIndexNGramSize.get().intValue(),
                            prefs.logIndexNGramSize.get().intValue());
                    var ts = new LowerCaseFilter(tokenizer);
                    return new Analyzer.TokenStreamComponents(tokenizer::setReader, ts);
                }
            };
        }
        return new Analyzer() {
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
    }

    @Override
    protected FacetsConfig initializeFacetsConfig(FacetsConfig facetsConfig) {
        facetsConfig.setRequireDimCount(SEVERITY, true);
        facetsConfig.setDrillDownTermsIndexing(SEVERITY, FacetsConfig.DrillDownTermsIndexing.ALL);
        return super.initializeFacetsConfig(facetsConfig);
    }

    private void rewriteQuery(Query query, BooleanQuery.Builder accQuery, BooleanClause.Occur occur) throws IOException {
        if (query instanceof BooleanQuery booleanQuery) {
            for (BooleanClause booleanClause : booleanQuery) {
                rewriteQuery(booleanClause.getQuery(), accQuery, booleanClause.getOccur());
            }
        } else if (query instanceof TermQuery termQuery) {
            accQuery.add(splitTermToNGrams(termQuery.getTerm()), occur);
        } else {
            int ngramSize = prefs.logIndexNGramSize.get().intValue();
            if (query instanceof PrefixQuery prefixQuery &&
                    prefixQuery.getPrefix().text().length() > ngramSize) {
                accQuery.add(splitTermToNGrams(prefixQuery.getPrefix()), occur);
            } else if (query instanceof WildcardQuery wildcardQuery &&
                    wildcardQuery.getTerm().text().replace("*", "").replace("?", "").length() > ngramSize) {
                var subBuilder = new BooleanQuery.Builder();
                for (var txt : wildcardQuery.getTerm().text().split("[?*]")) {
                    subBuilder.add(splitTermToNGrams(new Term(wildcardQuery.getTerm().field(), txt)), BooleanClause.Occur.FILTER);
                }
                accQuery.add(subBuilder.build(), occur);
            } else if (query instanceof PhraseQuery phraseQuery) {
                var text = Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining(" "));
                accQuery.add(splitTermToNGrams(new Term(phraseQuery.getField(), text)), occur);
            } else {
                accQuery.add(query, occur);
            }
        }

    }

    private Query splitTermToNGrams(Term term) throws IOException {
        final int nGramSize = prefs.logIndexNGramSize.get().intValue();
        if (prefs.logIndexAutoExpendShorterTerms.get() && term.text().length() < nGramSize) {
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
                Query userQuery;
                if (prefs.useNGramTokenization.get()) {
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
        return proc;
    }

    @Override
    public void close() throws IOException {
        hitResultCache.invalidateAll();
        facetResultCache.invalidateAll();
        super.close();
    }
}
