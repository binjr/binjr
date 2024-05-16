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
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.paint.Color;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class GcLogDataStore extends GcAggregation {
    private final Map<String, AggregationInfo> aggregations = new LinkedHashMap<>();
    private final ZonedDateTime timeStampAnchor;

    public GcLogDataStore() {
        timeStampAnchor = UserPreferences.getInstance().defaultDateTimeAnchor.get().resolve().atZone(ZoneId.systemDefault());
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
                timeStampAnchor.plus(Math.round(timeStamp.getTimeStamp() * 1000), ChronoUnit.MILLIS);
        info.data().put(ts.toInstant().toEpochMilli(), new TsSample(ts, value));

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

    public void computeAllocationRate(){
        computeAllocationRate(GcAggregator.POOL_HEAP, Color.ORANGERED);
        computeAllocationRate(GcAggregator.POOL_TENURED, Color.SEAGREEN);
        computeAllocationRate(GcAggregator.POOL_METASPACE, Color.CHOCOLATE);
        computeAllocationRate(GcAggregator.POOL_SURVIVOR, Color.STEELBLUE);

        computeAllocationRate(GcAggregator.POOL_EDEN, Color.GOLD);
        computeAllocationRate(GcAggregator.POOL_YOUNG, Color.GOLD);
    }

    private void computeAllocationRate(String poolName, Color color) {
        var beforeGc = this.aggregations.get(poolName + GcAggregator.ID_OCCUPANCY_BEFORE_COLLECTION);
        var afterGc = this.aggregations.get(poolName + GcAggregator.ID_OCCUPANCY_AFTER_COLLECTION);
        if (beforeGc == null || afterGc == null){
            return;
        }
        if (beforeGc.data().size() != afterGc.data().size()) {
            throw new IllegalStateException("After collection and Before collection series do no not match for memory pool " + poolName);
        }
        var allocRateData = this.aggregations.computeIfAbsent(poolName + GcAggregator.ID_ALLOCATION_RATE,
                a -> new AggregationInfo(List.of(GcAggregator.CAT_ALLOCATION_RATE),
                        poolName + GcAggregator.ID_ALLOCATION_RATE,
                        poolName,
                        GcAggregator.UNIT_BYTES_PER_SECOND,
                        UnitPrefixes.BINARY,
                        ChartType.LINE,
                        color));
        var prevIterator =  afterGc.data().entrySet().iterator();
        boolean firstSkipped = false;
        for (var current : beforeGc.data().entrySet()) {
            if (firstSkipped) {
                if (prevIterator.hasNext()) {
                    var previous = prevIterator.next();
                    var allocatedBytes = current.getValue().value() - previous.getValue().value();
                    double secondsSinceLastGc = (current.getKey() - previous.getKey()) / 1000d;
                    allocRateData.data().put(current.getKey(),
                            new TsSample(current.getValue().timestamp(), allocatedBytes / secondsSinceLastGc));
                }
            }
            firstSkipped = true;
        }

    }

    public static record TsSample(ZonedDateTime timestamp, double value){

    }

    public static record AggregationInfo(List<String> category,
                                         String name,
                                         String label,
                                         String unit,
                                         UnitPrefixes prefix,
                                         ChartType chartType,
                                         Color color,
                                         ConcurrentNavigableMap<Long, TsSample> data) {
        public AggregationInfo(String category,
                               String name,
                               String label,
                               String unit,
                               UnitPrefixes prefix,
                               ChartType chartType,
                               Color color) {
            this(List.of(category), name, label, unit, prefix, chartType,color, new ConcurrentSkipListMap<>());
        }
        public AggregationInfo(List<String> categories,
                               String name,
                               String label,
                               String unit,
                               UnitPrefixes prefix,
                               ChartType chartType,
                               Color color) {
            this(categories, name, label, unit, prefix, chartType,color, new ConcurrentSkipListMap<>());
        }

    }
}
