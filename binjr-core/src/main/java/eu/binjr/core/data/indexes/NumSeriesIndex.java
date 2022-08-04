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

import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class NumSeriesIndex extends Index<String> {
    private static final Logger logger = Logger.create(NumSeriesIndex.class);

    public NumSeriesIndex() throws IOException {
        super();
    }
//
//    @Override
//    protected Document enrichDocument(Document doc, ParsedEvent<String> event) throws IOException {
//        // add all other sections as stored double fields
//        event.getFields().forEach((key, value) -> doc.add(new StoredField(key, formatToDouble(value))));
//        return super.enrichDocument(doc, event);
//    }

    public long search(long start,
                       long end,
                       Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> seriesToFill,
                       ZoneId zoneId,
                       boolean ignoreCache) throws Exception {
        return getIndexMonitor().read().lock(() -> {
            Query rangeQuery = LongPoint.newRangeQuery(TIMESTAMP, start, end);
            var drill = new DrillSideways(searcher, facetsConfig, taxonomyReader);
            var drillDownQuery = new DrillDownQuery(facetsConfig, rangeQuery);
            seriesToFill.keySet().stream().map(ts -> ts.getBinding().getPath()).forEach(path -> drillDownQuery.add(PATH, path));
            var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                    new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
            int pageNumber = 0;
            int pageSize = prefs.numIdxMaxPageSize.get().intValue();
            int skip = 0;

            logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
            DrillSideways.DrillSidewaysResult results;
            try (Profiler p = Profiler.start("Executing query", logger::perf)) {
                results = drill.search(drillDownQuery, null, null, pageSize, sort, false);
            }
            var fieldsToLoad = seriesToFill.keySet().stream().map(k-> k.getBinding().getLabel()).collect(Collectors.toSet());
            fieldsToLoad.add(TIMESTAMP);
            try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                logger.debug(() -> "Total hits = " + results.hits.totalHits.value);
                for (int i = 0; i < results.hits.scoreDocs.length; i++) {
                    var hit = results.hits.scoreDocs[i];
                    var doc = searcher.doc(hit.doc, fieldsToLoad);
                    seriesToFill.forEach((info, data) -> {
                        var timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), zoneId);
                        var value = doc.getField(info.getBinding().getLabel()).numericValue();
                        if (value != null) {
                            data.addSample(timestamp, value.doubleValue());
                        }
                    });
                }
                return results.hits.totalHits.value;
            }
        });
    }


}
