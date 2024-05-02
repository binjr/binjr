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
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.microsoft.gctoolkit.event.MemoryPoolSummary;
import com.microsoft.gctoolkit.event.RegionSummary;
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

    private void processMemPool(String poolName, MemoryPoolSummary memPool, GarbageCollectionTypes gcType, DateTimeStamp timeStamp, Color color) {
        if (memPool.getSizeBeforeCollection() >= 0) {
            aggregation().storeSample("Size (Before GC)", poolName + "SizeBeforeCollection", poolName, "bytes", UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcType, timeStamp, memPool.getSizeBeforeCollection() * 1024L);
        }
        if (memPool.getSizeAfterCollection() >= 0) {
            aggregation().storeSample("Size (After GC)", poolName + "SizeAfterCollection", poolName, "bytes", UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcType, timeStamp, memPool.getSizeAfterCollection() * 1024L);
        }
        if (memPool.getOccupancyBeforeCollection() >= 0) {
            aggregation().storeSample("Occupancy (Compounded)", poolName + "OccupancyCompounded", poolName, "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcType, timeStamp, memPool.getOccupancyBeforeCollection() * 1024L);
            aggregation().storeSample("Occupancy (Before GC)", poolName + "OccupancyBeforeCollection", poolName, "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcType, timeStamp, memPool.getOccupancyBeforeCollection() * 1024L);
        }
        if (memPool.getOccupancyAfterCollection() >= 0) {
            aggregation().storeSample("Occupancy (Compounded)", poolName + "OccupancyCompounded", poolName, "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcType, new DateTimeStamp(timeStamp.getDateTime().plus(1, ChronoUnit.MILLIS), timeStamp.toEpochInMillis() + 1), memPool.getOccupancyAfterCollection() * 1024L);
            aggregation().storeSample("Occupancy (After GC)", poolName + "OccupancyAfterCollection", poolName, "bytes", UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcType, timeStamp, memPool.getOccupancyAfterCollection() * 1024L);
        }

    }

    private void recordGcPauseEvent(GCEvent event) {
        if (event.getDuration() >= 0) {
            aggregation().storeSample("Pause Time", event.getGarbageCollectionType().name(), event.getGarbageCollectionType().getLabel(), "seconds", UnitPrefixes.METRIC, ChartType.SCATTER,
                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
        }
    }

    private void processEvent(GenerationalGCPauseEvent event) {
        recordGcPauseEvent(event);
//        if (event.getHeap() != null) {
//            processMemPool("Heap", event.getHeap(), event.getGarbageCollectionType(), event.getDateTimeStamp());
//        }
        if (event.getPermOrMetaspace() != null) {
            processMemPool("Metaspace", event.getPermOrMetaspace(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.CHOCOLATE);
        }
        if (event.getTenured() != null) {
            processMemPool("Tenured", event.getTenured(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.SEAGREEN);
        }

        if (event.getYoung() != null) {
            processMemPool("Young", event.getYoung(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.GOLD);
        }

        if (event.getNonClassspace() != null) {
            processMemPool("NonClassSpace", event.getNonClassspace(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.MEDIUMPURPLE);
        }

        if (event.getClassspace() != null) {
            processMemPool("ClassSpace", event.getClassspace(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.AQUAMARINE);
        }
    }


    private void processEvent(G1GCPauseEvent event) {
        recordGcPauseEvent(event);
        if (event.getHeap() != null) {
            processMemPool("Heap", event.getHeap(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.SEAGREEN);
        }
        if (event.getPermOrMetaspace() != null) {
            processMemPool("Metaspace", event.getPermOrMetaspace(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.CHOCOLATE);
        }
        if (event.getTenured() != null) {
            processMemPool("Tenured", event.getTenured(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.BISQUE);
        }
        if (event.getEden() != null) {
            processMemPool("Eden", event.getEden(), event.getGarbageCollectionType(), event.getDateTimeStamp(), Color.GOLD);
        }
        if (event.getSurvivor() != null) {
            if (event.getSurvivor().getOccupancyBeforeCollection() >= 0) {
                aggregation().storeSample("Occupancy (Before GC)", "SurvivorOccupancyBeforeCollection", "Survivor", "bytes", UnitPrefixes.BINARY, ChartType.STACKED, Color.DEEPSKYBLUE,
                        event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getOccupancyBeforeCollection() * 1024L);
            }
            if (event.getSurvivor().getOccupancyAfterCollection() >= 0) {
                aggregation().storeSample("Occupancy (After GC)", "SurvivorOccupancyAfterCollection", "Survivor", "bytes", UnitPrefixes.BINARY, ChartType.STACKED, Color.DEEPSKYBLUE,
                        event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getOccupancyAfterCollection() * 1024L);
            }
            if (event.getSurvivor().getSize() >= 0) {
                aggregation().storeSample("Size (After GC)", "SurvivorSize", "Survivor", "bytes", UnitPrefixes.BINARY, ChartType.LINE, Color.DEEPSKYBLUE,
                        event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getSize() * 1024L);
            }
        }
    }

    private void processEvent(ZGCCycle event) {
        recordGcPauseEvent(event);
    }

    private void processEvent(ShenandoahCycle event) {
        recordGcPauseEvent(event);
    }
}
