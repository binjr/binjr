/*
 * Copyright 2024 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.sources.jvmgc.adapters.aggregation;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GcDataStore extends GcAggregation {
    private final Map<String, AggregationInfo> aggregations = new LinkedHashMap<>();

    public GcDataStore() {
    }

    @Override
    public void storeSample(List<String> categories,
                            String key,
                            String label,
                            String unit,
                            UnitPrefixes prefix,
                            ChartType chartType,
                            Color color,
                            GarbageCollectionTypes gcType,
                            DateTimeStamp timeStamp,
                            double value) {
        var info = this.aggregations.computeIfAbsent(key, aggregationInfo -> new AggregationInfo(categories, key, label, unit, prefix, chartType, color));
        var ts = timeStamp.hasDateStamp() ? timeStamp.getDateTime() :
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Math.round(timeStamp.getTimeStamp() * 1000)), ZoneId.systemDefault());
        info.data().put(ts.toInstant().toEpochMilli(), new Sample(ts, value));

    }

    public Map<String, AggregationInfo> get() {
        return aggregations;
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return aggregations.isEmpty();
    }

    @Override
    public String toString() {
        return "Collected " + aggregations.size() + " different collection types";
    }

}
