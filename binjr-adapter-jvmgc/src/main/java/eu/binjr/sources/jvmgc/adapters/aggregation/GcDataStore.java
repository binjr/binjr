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
import eu.binjr.core.data.codec.csv.DataSample;
import eu.binjr.core.data.workspace.UnitPrefixes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class GcDataStore extends GcAggregation {
    //private final ConcurrentNavigableMap<Long, DataSample> aggregations;
    private final Map<String, AggregationInfo> aggregations = new ConcurrentHashMap<>();
    // private final Set<GarbageCollectionTypes> encounteredGcEventTypes = new ConcurrentHashSet<>();

    public GcDataStore() {


    }

    @Override
    public void addHeapOccupancyDataPoint(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double... values) {
        this.storeSample(
                "OccupancyAfterCollection",
                "Heap Occupancy (After GC)",
                "bytes", UnitPrefixes.BINARY,
                gcType,
                timeStamp,
                values);

    }

    @Override
    public void recordPauseDuration(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double duration) {
        this.storeSample(
                "pauseTime",
                "Pause Time",
                "seconds",
                UnitPrefixes.METRIC,
                gcType,
                timeStamp,
                duration);
    }

    private void storeSample(String key,
                             String label,
                             String unit,
                             UnitPrefixes prefix,
                             GarbageCollectionTypes gcType,
                             DateTimeStamp timeStamp,
                             double... values) {
        DataSample sample;
        var info = this.aggregations.computeIfAbsent(key, aggregationInfo -> new AggregationInfo(key, label, unit, prefix));
        info.encounteredGcTypes().add(gcType);
        if (timeStamp.hasDateStamp()) {
            sample = new DataSample(timeStamp.getDateTime());
        } else {
            sample = new DataSample(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Math.round(timeStamp.getTimeStamp() * 1000)), ZoneId.systemDefault()));
        }
        new DataSample(timeStamp.getDateTime());
        for (var l : values) {
            sample.getCells().put(gcType.name(), l);
        }

        info.data().put(sample.getTimeStamp().toInstant().toEpochMilli(), sample);

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
