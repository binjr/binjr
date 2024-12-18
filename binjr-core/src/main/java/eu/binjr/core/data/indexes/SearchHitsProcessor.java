/*
 *    Copyright 2020-2024 Frederic Thevenet
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

import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SearchHitsProcessor extends TimeSeriesProcessor<SearchHit> {

    private final Map<String, Collection<FacetEntry>> facetResults = new HashMap<>();
    private long totalHits = 0;
    private int hitsPerPage = 0;

    public SearchHitsProcessor() {
        super();
    }

    public SearchHitsProcessor(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected SearchHit computeMinValue() {
        return (data != null && !data.isEmpty()) ? data.getFirst().getYValue() : null;
    }

    @Override
    protected SearchHit computeAverageValue() {
        return null;
    }

    @Override
    protected SearchHit computeMaxValue() {
        return (data != null && !data.isEmpty()) ? data.getLast().getYValue() : null;
    }

    public Map<String, Collection<FacetEntry>> getFacetResults() {
        return facetResults;
    }

    public void addFacetResults(String name, Collection<FacetEntry> synthesis) {
        this.facetResults.put(name, synthesis);
    }

    public void mergeFacetResults(SearchHitsProcessor toMerge) {
        Objects.requireNonNull(toMerge, "SearchHitsProcessor to merge facets from cannot be null");
        toMerge.getFacetResults().forEach(this::addFacetResults);
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public void setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
    }

    public int getHitsPerPage() {
        return hitsPerPage;
    }

}
