/*
 * Copyright 2024-2025 Frederic Thevenet
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

import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.common.logging.Logger;
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
    private static final Logger logger = Logger.create(GcLogDataStore.class);

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
                            DateTimeStamp timeStamp,
                            double value) {
        var info = this.aggregations.computeIfAbsent(key, k -> new AggregationInfo(categories, k, label, unit, prefix, chartType, color));
        var ts = timeStamp.hasDateStamp() ? timeStamp.getDateTime() :
                timeStampAnchor.plus(Math.round(timeStamp.getTimeStamp() * 1000), ChronoUnit.MILLIS);
        var collidingTs = info.data().put(ts.toInstant().toEpochMilli(), new TsSample(ts, value));
        if (collidingTs != null) {
            // attempt to disambiguate colliding timestamps for effectively different events
            info.data().put(ts.toInstant().toEpochMilli() + 1, new TsSample(ts, value));
        }
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

    public void computeAllocationStats() {
        computeAllocationStats(GcAggregator.POOL_HEAP, Color.ORANGERED);
    }

    private void computeAllocationStats(String poolName, Color color) {
        var beforeGc = this.aggregations.get(poolName + GcAggregator.ID_OCCUPANCY_BEFORE_COLLECTION);
        var afterGc = this.aggregations.get(poolName + GcAggregator.ID_OCCUPANCY_AFTER_COLLECTION);
        if (beforeGc == null) {
            logger.error("Cannot compute heap allocation rate: heap occupancy before GC not captured.");
            return;
        }
        if (afterGc == null) {
            logger.error("Cannot compute heap allocation rate: heap occupancy after GC not captured.");
            return;
        }
        if (beforeGc.data().size() != afterGc.data().size()) {
            logger.error("Cannot compute heap allocation rate: number of samples captured before ({}) and after ({}) GC do not match.",
                    beforeGc.data().size(),
                    afterGc.data().size());
            return;
        }
        var prevIterator = afterGc.data().entrySet().iterator();
        boolean firstSkipped = false;
        for (var current : beforeGc.data().entrySet()) {
            if (firstSkipped) {
                if (prevIterator.hasNext()) {
                    var previous = prevIterator.next();
                    var allocatedBytes = current.getValue().value() - previous.getValue().value();
                    double secondsSinceLastGc = (current.getKey() - previous.getKey()) / 1000d;
                    var allocRateData = this.aggregations.computeIfAbsent(poolName + GcAggregator.ID_ALLOCATION_RATE,
                            a -> new AggregationInfo(List.of(GcAggregator.CAT_HEAP, GcAggregator.CAT_ALLOCATION_RATE),
                                    poolName + GcAggregator.ID_ALLOCATION_RATE,
                                    poolName + " allocation rate",
                                    GcAggregator.UNIT_BYTES_PER_SECOND,
                                    UnitPrefixes.BINARY,
                                    ChartType.IMPULSE,
                                    color));
                    allocRateData.data().put(current.getKey(),
                            new TsSample(current.getValue().timestamp(), allocatedBytes / secondsSinceLastGc));

                }
            } else {
                firstSkipped = true;
            }
        }
    }

    public record TsSample(ZonedDateTime timestamp, double value) {

    }

    public record AggregationInfo(List<String> category,
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
            this(List.of(category), name, label, unit, prefix, chartType, color, new ConcurrentSkipListMap<>());
        }

        public AggregationInfo(List<String> categories,
                               String name,
                               String label,
                               String unit,
                               UnitPrefixes prefix,
                               ChartType chartType,
                               Color color) {
            this(categories, name, label, unit, prefix, chartType, color, new ConcurrentSkipListMap<>());
        }

    }
}
