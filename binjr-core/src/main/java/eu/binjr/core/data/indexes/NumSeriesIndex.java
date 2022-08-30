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
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import eu.binjr.core.data.timeseries.transform.NanToZeroTransform;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.preferences.UserPreferences;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NumSeriesIndex extends Index {
    private static final Logger logger = Logger.create(NumSeriesIndex.class);
    private final UserPreferences userPref;

    public NumSeriesIndex() throws IOException {
        super();
        userPref = UserPreferences.getInstance();
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
            seriesToFill.keySet().stream().map(ts -> ts.getBinding().getPath()).forEach(path -> drillDownQuery.add(PATH, path));
            var sort = new Sort(new SortedNumericSortField(TIMESTAMP, SortField.Type.LONG, false),
                    new SortedNumericSortField(LINE_NUMBER, SortField.Type.LONG, false));
            var clean = new NanToZeroTransform();
            var reduce = new LargestTriangleThreeBucketsTransform(userPref.downSamplingThreshold.get().intValue());
            long hitsCollected = 0;
            int pageSize = prefs.numIdxMaxPageSize.get().intValue();
            try (Profiler p = Profiler.start("Retrieving hits", logger::perf)) {
                for (int pageNumber = 0; pageNumber < 100000; pageNumber++) {
                    int skip = pageNumber * pageSize;
                    TopFieldCollector collector = TopFieldCollector.create(sort, skip + pageSize, Integer.MAX_VALUE);
                    logger.debug(() -> "Query: " + drillDownQuery.toString(FIELD_CONTENT));
                    try (Profiler ignored = Profiler.start("Executing query for page " + pageNumber, logger::debug)) {
                        drill.search(drillDownQuery, collector);
                    }
                    var fieldsToLoad = seriesToFill.keySet().stream().map(k -> k.getBinding().getLabel()).collect(Collectors.toSet());
                    fieldsToLoad.add(TIMESTAMP);
                    Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> pageDataBuffer = new HashMap<>();
                    var topDocs = collector.topDocs(skip, pageSize);
                    // fill the buffer with data for the current page
                    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                        var hit = topDocs.scoreDocs[i];
                        var doc = searcher.doc(hit.doc, fieldsToLoad);
                        seriesToFill.forEach((info, proc) -> {
                            var timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(doc.get(TIMESTAMP))), zoneId);
                            var value = doc.getField(info.getBinding().getLabel()).numericValue();
                            if (value != null) {
                                pageDataBuffer.computeIfAbsent(info, doubleTimeSeriesInfo -> new DoubleTimeSeriesProcessor()).addSample(timestamp, value.doubleValue());
                            }
                        });
                    }
                    // Apply downsampling to buffered data and append reduced dataset to processor
                    seriesToFill.entrySet().parallelStream().forEach(entry -> {
                        var pageProc = pageDataBuffer.get(entry.getKey());
                        var fullQueryProc = entry.getValue();
                        pageProc.applyTransforms(clean, reduce);
                        fullQueryProc.appendData(pageProc);
                    });
                    hitsCollected += topDocs.scoreDocs.length;
                    // End the loop once all hits have been collected
                    if (hitsCollected >= collector.getTotalHits()) {
                        break;
                    }
                }
            }
            return hitsCollected;
        });
    }
}
