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

import com.microsoft.gctoolkit.aggregator.Aggregates;
import com.microsoft.gctoolkit.aggregator.Aggregator;
import com.microsoft.gctoolkit.aggregator.EventSource;
import com.microsoft.gctoolkit.event.GCEvent;
import com.microsoft.gctoolkit.event.MemoryPoolSummary;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.shenandoah.ShenandoahCycle;
import com.microsoft.gctoolkit.event.zgc.ZGCCycle;
import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import java.time.temporal.ChronoUnit;


@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC, EventSource.SHENANDOAH})
public class GcAggregator extends Aggregator<GcAggregation> {

    public GcAggregator(GcAggregation results) {
        super(results);
        register(GenerationalGCPauseEvent.class, this::processEvent);
        register(G1GCPauseEvent.class, this::processEvent);
        register(ZGCCycle.class, this::processEvent);
        register(ShenandoahCycle.class, this::processEvent);
    }

    private void recordMemPoolStats(String poolName,
                                    MemoryPoolSummary memPool,
                                    GCEvent gcEvent,
                                    Color color) {
        recordMemPoolStats(poolName,
                memPool.getSizeBeforeCollection(),
                memPool.getSizeAfterCollection(),
                memPool.getOccupancyBeforeCollection(),
                memPool.getOccupancyAfterCollection(),
                gcEvent,
                color);
    }

    private void recordMemPoolStats(String poolName,
                                    long sizeBeforeGc,
                                    long sizeAfterGc,
                                    long occupancyBeforeGc,
                                    long occupancyAfterGc,
                                    GCEvent gcEvent,
                                    Color color) {
        if (sizeBeforeGc >= 0) {
            aggregation().storeSample("Size (Before GC)",
                    poolName + "SizeBeforeCollection", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeBeforeGc * 1024L);
        }
        if (sizeAfterGc >= 0) {
            aggregation().storeSample("Size (After GC)",
                    poolName + "SizeAfterCollection", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeAfterGc * 1024L);
        }
        if (occupancyBeforeGc >= 0) {
            aggregation().storeSample("Occupancy (Compounded)",
                    poolName + "OccupancyCompounded", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyBeforeGc * 1024L);
            aggregation().storeSample("Occupancy (Before GC)",
                    poolName + "OccupancyBeforeCollection", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyBeforeGc * 1024L);
        }
        if (occupancyAfterGc >= 0) {
            aggregation().storeSample("Occupancy (Compounded)",
                    poolName + "OccupancyCompounded", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    new DateTimeStamp(gcEvent.getDateTimeStamp().getDateTime().plus(1, ChronoUnit.MILLIS),
                            gcEvent.getDateTimeStamp().toEpochInMillis() + 1),
                    occupancyAfterGc * 1024L);
            aggregation().storeSample("Occupancy (After GC)",
                    poolName + "OccupancyAfterCollection", poolName,
                    "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyAfterGc * 1024L);
        }

    }

    private void recordGcPauseEvent(GCEvent event) {
        if (event.getDuration() >= 0) {
            aggregation().storeSample("Pause Time",
                    event.getGarbageCollectionType().name(),
                    event.getGarbageCollectionType().getLabel(),
                    "seconds", UnitPrefixes.METRIC, ChartType.SCATTER,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(), event.getDuration());
        }
    }

    private void processEvent(GenerationalGCPauseEvent event) {
        recordGcPauseEvent(event);
//        if (event.getHeap() != null) {
//            recordMemPoolStats("Heap", event.getHeap(), event, Color.DARKKHAKI);
//        }
        if (event.getPermOrMetaspace() != null) {
            recordMemPoolStats("Metaspace", event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        }
        if (event.getTenured() != null) {
            recordMemPoolStats("Tenured", event.getTenured(), event, Color.SEAGREEN);
        }

        if (event.getYoung() != null) {
            recordMemPoolStats("Young", event.getYoung(), event, Color.GOLD);
        }

        if (event.getNonClassspace() != null) {
            recordMemPoolStats("NonClassSpace", event.getNonClassspace(), event, Color.MEDIUMPURPLE);
        }

        if (event.getClassspace() != null) {
            recordMemPoolStats("ClassSpace", event.getClassspace(), event, Color.AQUAMARINE);
        }
    }


    private void processEvent(G1GCPauseEvent event) {
        recordGcPauseEvent(event);
//        if (event.getHeap() != null) {
//            recordMemPoolStats("Heap", event.getHeap(), event, Color.DARKKHAKI);
//        }
        if (event.getPermOrMetaspace() != null) {
            recordMemPoolStats("Metaspace", event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        }
        if (event.getTenured() != null) {
            recordMemPoolStats("Tenured", event.getTenured(), event, Color.SEAGREEN);
        }
        if (event.getEden() != null) {
            recordMemPoolStats("Eden", event.getEden(), event, Color.GOLD);
        }
        if (event.getSurvivor() != null) {
            recordMemPoolStats("Survivor",
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getOccupancyBeforeCollection(),
                    event.getSurvivor().getOccupancyAfterCollection(),
                    event, Color.DEEPSKYBLUE);
        }
    }

    private void processEvent(ZGCCycle event) {
        recordGcPauseEvent(event);
    }

    private void processEvent(ShenandoahCycle event) {
        recordGcPauseEvent(event);
    }
}
