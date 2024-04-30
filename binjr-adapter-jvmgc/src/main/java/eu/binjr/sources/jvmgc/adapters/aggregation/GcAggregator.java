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


@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC, EventSource.SHENANDOAH})
public class GcAggregator extends Aggregator<GcAggregation> {

    public GcAggregator(GcAggregation results) {
        super(results);
        register(GenerationalGCPauseEvent.class, this::processEvent);
        register(G1GCPauseEvent.class, this::processEvent);
        register(ZGCCycle.class, this::processEvent);
        register(ShenandoahCycle.class, this::processEvent);
    }

    private void processMemPool(String poolName, MemoryPoolSummary memPool, GarbageCollectionTypes gcType, DateTimeStamp timeStamp) {
        aggregation().storeSample(poolName, poolName + "OccupancyAfterCollection", "Occupancy (After GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                gcType, timeStamp, memPool.getOccupancyAfterCollection() * 1024L);
        aggregation().storeSample(poolName, poolName + "OccupancyBeforeCollection", "Occupancy (Before GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                gcType, timeStamp, memPool.getOccupancyBeforeCollection() * 1024L);
        aggregation().storeSample(poolName, poolName + "SizeAfterCollection", "Size (After GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                gcType, timeStamp, memPool.getSizeAfterCollection() * 1024L);
        aggregation().storeSample(poolName, poolName + "SizeBeforeCollection", "Size (Before GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                gcType, timeStamp, memPool.getSizeBeforeCollection() * 1024L);
    }


    private void processEvent(GenerationalGCPauseEvent event) {
        aggregation().storeSample("", "pauseTime", "Pause Time", "seconds", UnitPrefixes.METRIC, ChartType.SCATTER,
                event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
        if (event.getHeap() != null) {
            processMemPool("Heap", event.getHeap(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }
        if (event.getPermOrMetaspace() != null) {
            processMemPool("Metaspace", event.getPermOrMetaspace(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }
        if (event.getTenured() != null) {
            processMemPool("Tenured", event.getTenured(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }

        if (event.getYoung() != null) {
            processMemPool("Young", event.getYoung(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }

        if (event.getNonClassspace() != null) {
            processMemPool("NonClassSpace", event.getNonClassspace(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }

        if (event.getClassspace() != null) {
            processMemPool("ClassSpace", event.getClassspace(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }
    }


    private void processEvent(G1GCPauseEvent event) {
        aggregation().storeSample("", "pauseTime", "Pause Time", "seconds", UnitPrefixes.METRIC, ChartType.SCATTER,
                event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
        if (event.getHeap() != null) {
            processMemPool("Heap", event.getHeap(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }
        if (event.getPermOrMetaspace() != null) {
            processMemPool("Metaspace", event.getPermOrMetaspace(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }
        if (event.getTenured() != null) {
            processMemPool("Tenured", event.getTenured(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }

        if (event.getEden() != null) {
            processMemPool("Eden", event.getEden(), event.getGarbageCollectionType(), event.getDateTimeStamp());
        }

        if (event.getSurvivor() != null) {
            aggregation().storeSample("Survivor", "SurvivorOccupancyAfterCollection", "Occupancy (After GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getOccupancyAfterCollection() * 1024L);
            aggregation().storeSample("Survivor", "SurvivorOccupancyBeforeCollection", "Occupancy (Before GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getOccupancyBeforeCollection() * 1024L);
            aggregation().storeSample("Survivor", "SurvivorSize", "Size", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getSurvivor().getSize() * 1024L);
        }


//        if (event.getHeap() != null) {
//            aggregation().storeSample("OccupancyAfterCollection", "Heap Occupancy (After GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
//                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyAfterCollection() * 1024L);
//            aggregation().storeSample("OccupancyBeforeCollection", "Heap Occupancy (Before GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
//                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyBeforeCollection() * 1024L);
//            aggregation().storeSample("SizeAfterCollection", "Heap Size (After GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
//                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getSizeAfterCollection() * 1024L);
//            aggregation().storeSample("SizeBeforeCollection", "Heap Size (Before GC)", "bytes", UnitPrefixes.BINARY, ChartType.LINE,
//                    event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getSizeBeforeCollection() * 1024L);
//        }
//        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
//
//        if (event.getHeap() != null) {
//            aggregation().addHeapOccupancyAfterGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyAfterCollection() * 1024L);
//            aggregation().addHeapOccupancyBeforeGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyBeforeCollection() * 1024L);
//            aggregation().addHeapSizeAfterGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getSizeAfterCollection() * 1024L);
//            aggregation().addHeapSizeBeforeGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getSizeBeforeCollection() * 1024L);
//        }
//
//        if (event.getPermOrMetaspace() != null) {
//            aggregation().addHeapOccupancyAfterGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getPermOrMetaspace().getOccupancyAfterCollection() * 1024L);
//        }
    }

    private void processEvent(ZGCCycle event) {
//        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
//
//        if (event.getLive() != null) {
//            aggregation().addHeapOccupancyAfterGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getLive().getReclaimEnd() * 1024L);
//            aggregation().addHeapOccupancyBeforeGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getLive().getReclaimStart() * 1024L);
//            aggregation().addHeapSizeAfterGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getAllocated().getReclaimEnd() * 1024L);
//            aggregation().addHeapSizeBeforeGc(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getAllocated().getReclaimStart() * 1024L);
//        }
    }

    private void processEvent(ShenandoahCycle event) {
//        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());

        //  aggregation().addDataPoint(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.get ());
    }
}
