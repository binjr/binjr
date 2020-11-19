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

package eu.binjr.core.data.indexes;

import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SearchHitsProcessor extends TimeSeriesProcessor<SearchHit> {

    private final Map<String, Collection<FacetEntry>> facetResults = new HashMap<>();
    private int totalHits = 0;
    private int hitsPerPage = 0;

    @Override
    protected SearchHit computeMinValue() {
        return (data != null && data.size() > 0) ? data.get(0).getYValue() : null;
    }

    @Override
    protected SearchHit computeAverageValue() {
        return null;
    }

    @Override
    protected SearchHit computeMaxValue() {
        return (data != null && data.size() > 0) ? data.get(data.size() - 1).getYValue() : null;
    }

    public Map<String, Collection<FacetEntry>> getFacetResults() {
        return facetResults;
    }

    public void addFacetResults(String name, Collection<FacetEntry> synthesis) {
        this.facetResults.put(name, synthesis);
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    public void setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
    }

    public int getHitsPerPage() {
        return hitsPerPage;
    }

}
