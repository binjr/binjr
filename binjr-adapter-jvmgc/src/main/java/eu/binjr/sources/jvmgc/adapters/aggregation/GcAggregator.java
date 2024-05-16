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
import com.microsoft.gctoolkit.event.CPUSummary;
import com.microsoft.gctoolkit.event.GCEvent;
import com.microsoft.gctoolkit.event.MemoryPoolSummary;
import com.microsoft.gctoolkit.event.ReferenceGCSummary;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.shenandoah.ShenandoahCycle;
import com.microsoft.gctoolkit.event.zgc.ZGCCycle;
import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import java.time.Duration;
import java.util.List;


@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC, EventSource.SHENANDOAH})
public class GcAggregator extends Aggregator<GcAggregation> {

    public static final String REF_JNI_WEAK = "Jni Weak";
    public static final String REF_PHANTOM = "Phantom";
    public static final String REF_SOFT = "Soft";
    public static final String REF_WEAK = "Weak";
    public static final String REF_FINAL = "Final";
    public static final String ID_REF_PAUSE_TIME = "RefPauseTime";
    public static final String ID_REF_COUNT = "RefCount";
    public static final String POOL_METASPACE = "Metaspace";
    public static final String POOL_NON_CLASS_SPACE = "NonClassSpace";
    public static final String POOL_CLASS_SPACE = "ClassSpace";
    public static final String POOL_TENURED = "Tenured";
    public static final String POOL_YOUNG = "Young";
    public static final String POOL_SURVIVOR = "Survivor";
    public static final String POOL_EDEN = "Eden";
    public static final String POOL_HEAP = "Heap";
    public static final String CAT_OCCUPANCY = "Occupancy";
    public static final String CAT_SIZE = "Size";
    public static final String CAT_PAUSE_TIME = "Pause Time";
    public static final String CAT_CPU = "CPU";
    public static final String CAT_REFERENCES = "References";
    public static final String CAT_ALLOCATION_RATE = "Allocation Rate";
    public static final String ID_OCCUPANCY_BEFORE_COLLECTION = "OccupancyBeforeCollection";
    public static final String ID_OCCUPANCY_AFTER_COLLECTION = "OccupancyAfterCollection";
    public static final String ID_OCCUPANCY_MERGED = "OccupancyMerged";
    public static final String ID_ALLOCATION_RATE = "AllocationRate";
    public static final String ID_SIZE_AFTER_COLLECTION = "SizeAfterCollection";
    public static final String ID_SIZE_BEFORE_COLLECTION = "SizeBeforeCollection";
    public static final String ID_CPU_SUMMARY_KERNEL = "CpuSummaryKernel";
    public static final String ID_CPU_SUMMARY_USER = "CpuSummaryUser";
    public static final String CAT_CPU_KERNEL = "Kernel";
    public static final String CAT_CPU_USER = "User";
    public static final String ID_CPU_SUMMARY_WALL_CLOCK = "CpuSummaryWallClock";
    public static final String CAT_CPU_TIME = "CPU Time";
    public static final String CAT_CPU_WALL_CLOCK = "CPU (Wall Clock)";
    public static final String UNIT_BYTES = "bytes";
    public static final String UNIT_SECONDS = "seconds";
    public static final String UNIT_BYTES_PER_SECOND = "bytes/s";
    public static final String CAT_REFERENCES_COUNT = "References (Count)";
    public static final String CAT_REFERENCES_PAUSE_TIME = "References (Pause Time)";
    public static final String CAT_DETAILED_AFTER_GC = "Detailed (After GC)";
    public static final String CAT_DETAILED_MERGED = "Detailed (Merged)";
    public static final String CAT_DETAILED_BEFORE_GC = "Detailed (Before GC)";
    public static final String CAT_SIZE_AFTER_GC = "Size (After GC)";
    public static final String CAT_SIZE_BEFORE_GC = "Size (Before GC)";
    public static final String CAT_TOTAL_HEAP = "Total Heap";
    public static final String CAT_HEAP_MERGED = "Heap (Merged)";
    public static final String CAT_HEAP_AFTER_GC = "Heap (After GC)";
    public static final String CAT_HEAP_BEFORE_GC = "Heap (Before GC)";

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
        if (memPool != null) {
            recordMemPoolStats(poolName,
                    memPool.getSizeBeforeCollection(),
                    memPool.getSizeAfterCollection(),
                    memPool.getOccupancyBeforeCollection(),
                    memPool.getOccupancyAfterCollection(),
                    gcEvent,
                    color);
        }
    }

    private void recordTotalHeapStats(MemoryPoolSummary memPool, GCEvent gcEvent) {
        if (memPool != null) {
            var occupancyBeforeGc = memPool.getOccupancyBeforeCollection();
            if (occupancyBeforeGc >= 0) {
                aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_MERGED, CAT_HEAP_MERGED,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.STEELBLUE,
                        gcEvent.getGarbageCollectionType(),
                        shiftDateTimeSamp(gcEvent, -1 * gcEvent.getDuration()),
                        occupancyBeforeGc * 1024L);
                aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_BEFORE_COLLECTION, CAT_HEAP_BEFORE_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.SEAGREEN,
                        gcEvent.getGarbageCollectionType(),
                        gcEvent.getDateTimeStamp(),
                        occupancyBeforeGc * 1024L);
            }
            var occupancyAfterGc = memPool.getOccupancyAfterCollection();
            if (occupancyAfterGc >= 0) {
                aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_AFTER_COLLECTION, CAT_HEAP_AFTER_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.TOMATO,
                        gcEvent.getGarbageCollectionType(),
                        gcEvent.getDateTimeStamp(),
                        occupancyAfterGc * 1024L);

                aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_MERGED, CAT_HEAP_MERGED,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.STEELBLUE,
                        gcEvent.getGarbageCollectionType(),
                        gcEvent.getDateTimeStamp(),
                        occupancyAfterGc * 1024L);
            }
        }

    }

    private void recordMemPoolStats(String poolName,
                                    long sizeBeforeGc,
                                    long sizeAfterGc,
                                    long occupancyBeforeGc,
                                    long occupancyAfterGc,
                                    GCEvent gcEvent,
                                    Color color) {
        if (sizeBeforeGc >= 0) {
            aggregation().storeSample(List.of(CAT_SIZE, CAT_SIZE_BEFORE_GC),
                    poolName + ID_SIZE_BEFORE_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeBeforeGc * 1024L);
        }
        if (sizeAfterGc >= 0) {
            aggregation().storeSample(List.of(CAT_SIZE, CAT_SIZE_AFTER_GC),
                    poolName + ID_SIZE_AFTER_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeAfterGc * 1024L);
        }
        if (occupancyBeforeGc >= 0) {
            aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_DETAILED_MERGED),
                    poolName + ID_OCCUPANCY_MERGED, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    shiftDateTimeSamp(gcEvent, -1 * gcEvent.getDuration()),
                    occupancyBeforeGc * 1024L);
            aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_DETAILED_BEFORE_GC),
                    poolName + ID_OCCUPANCY_BEFORE_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyBeforeGc * 1024L);
        }
        if (occupancyAfterGc >= 0) {
            aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_DETAILED_MERGED),
                    poolName + ID_OCCUPANCY_MERGED, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY,
                    ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyAfterGc * 1024L);
            aggregation().storeSample(List.of(CAT_OCCUPANCY, CAT_DETAILED_AFTER_GC),
                    poolName + ID_OCCUPANCY_AFTER_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY,
                    ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyAfterGc * 1024L);
        }
    }

    private DateTimeStamp shiftDateTimeSamp(GCEvent gcEvent, double shift) {
        if (gcEvent.getDateTimeStamp().getDateTime() == null) {
            return new DateTimeStamp(gcEvent.getDateTimeStamp().getTimeStamp() + shift);
        }
        return new DateTimeStamp(gcEvent.getDateTimeStamp().getDateTime().plus(Duration.ofMillis(Math.round(shift * 1000))),
                gcEvent.getDateTimeStamp().getTimeStamp() + shift);
    }

    private void recordGcPauseEvent(GCEvent event) {
        if (event.getDuration() >= 0) {
            aggregation().storeSample(CAT_PAUSE_TIME,
                    event.getGarbageCollectionType().name(),
                    event.getGarbageCollectionType().getLabel(),
                    UNIT_SECONDS, UnitPrefixes.METRIC, ChartType.SCATTER,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    event.getDuration());
        }
    }

    private void recordCpuStats(GCEvent event, CPUSummary cpuSummary) {
        if (cpuSummary != null) {
            aggregation().storeSample(List.of(CAT_CPU, CAT_CPU_TIME),
                    ID_CPU_SUMMARY_KERNEL,
                    CAT_CPU_KERNEL,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.STACKED,
                    Color.RED,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    cpuSummary.getKernel());
            aggregation().storeSample(List.of(CAT_CPU, CAT_CPU_TIME),
                    ID_CPU_SUMMARY_USER,
                    CAT_CPU_USER,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.STACKED,
                    Color.GREEN,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    cpuSummary.getUser());
            aggregation().storeSample(List.of(CAT_CPU, CAT_CPU_WALL_CLOCK),
                    ID_CPU_SUMMARY_WALL_CLOCK,
                    CAT_CPU_WALL_CLOCK,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.AREA,
                    Color.STEELBLUE,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    cpuSummary.getWallClock());
        }
    }

    private void addRefCount(GCEvent event, String refType, Color color, long refCount) {
        if (refCount >= 0) {
            aggregation().storeSample(List.of(CAT_REFERENCES, CAT_REFERENCES_COUNT),
                    refType.replace(" ", "") + ID_REF_COUNT,
                    refType,
                    "#",
                    UnitPrefixes.METRIC,
                    ChartType.SCATTER,
                    color,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    refCount);
        }
    }

    private void addRefPauseTime(GCEvent event, String refType, Color color, double refPauseTime) {
        if (refPauseTime >= 0) {
            aggregation().storeSample(List.of(CAT_REFERENCES, CAT_REFERENCES_PAUSE_TIME),
                    refType.replace(" ", "") + ID_REF_PAUSE_TIME,
                    refType,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.SCATTER,
                    color,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    refPauseTime);
        }
    }

    private void recordReferenceStats(GCEvent event, ReferenceGCSummary referenceGCSummary) {
        if (referenceGCSummary != null) {
            addRefCount(event, REF_JNI_WEAK, Color.CHARTREUSE, referenceGCSummary.getJniWeakReferenceCount());
            addRefPauseTime(event, REF_JNI_WEAK, Color.CHARTREUSE, referenceGCSummary.getJniWeakReferencePauseTime());
            addRefCount(event, REF_PHANTOM, Color.TOMATO, referenceGCSummary.getPhantomReferenceCount());
            addRefPauseTime(event, REF_PHANTOM, Color.TOMATO, referenceGCSummary.getPhantomReferencePauseTime());
            addRefCount(event, REF_SOFT, Color.SEAGREEN, referenceGCSummary.getSoftReferenceCount());
            addRefPauseTime(event, REF_SOFT, Color.SEAGREEN, referenceGCSummary.getSoftReferencePauseTime());
            addRefCount(event, REF_WEAK, Color.DEEPPINK, referenceGCSummary.getWeakReferenceCount());
            addRefPauseTime(event, REF_WEAK, Color.DEEPPINK, referenceGCSummary.getWeakReferencePauseTime());
            addRefCount(event, REF_FINAL, Color.MEDIUMPURPLE, referenceGCSummary.getFinalReferenceCount());
            addRefPauseTime(event, REF_FINAL, Color.MEDIUMPURPLE, referenceGCSummary.getFinalReferencePauseTime());
        }
    }

    private void processEvent(GenerationalGCPauseEvent event) {
        recordGcPauseEvent(event);
        recordCpuStats(event, event.getCpuSummary());
        recordTotalHeapStats(event.getHeap(), event);
        recordMemPoolStats(POOL_METASPACE, event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        recordMemPoolStats(POOL_NON_CLASS_SPACE, event.getNonClassspace(), event, Color.MEDIUMPURPLE);
        recordMemPoolStats(POOL_CLASS_SPACE, event.getClassspace(), event, Color.AQUAMARINE);
        recordMemPoolStats(POOL_TENURED, event.getTenured(), event, Color.SEAGREEN);
        recordMemPoolStats(POOL_YOUNG, event.getYoung(), event, Color.GOLD);
        recordCpuStats(event, event.getCpuSummary());
        recordReferenceStats(event, event.getReferenceGCSummary());
    }


    private void processEvent(G1GCPauseEvent event) {
        recordGcPauseEvent(event);
        recordTotalHeapStats(event.getHeap(), event);
        recordMemPoolStats(POOL_METASPACE, event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        recordMemPoolStats(POOL_TENURED, event.getTenured(), event, Color.SEAGREEN);
        if (event.getSurvivor() != null) {
            recordMemPoolStats(POOL_SURVIVOR,
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getOccupancyBeforeCollection(),
                    event.getSurvivor().getOccupancyAfterCollection(),
                    event, Color.DEEPSKYBLUE);
        }
        recordMemPoolStats(POOL_EDEN, event.getEden(), event, Color.GOLD);
        recordCpuStats(event, event.getCpuSummary());
        recordReferenceStats(event, event.getReferenceGCSummary());
    }

    private void processEvent(ZGCCycle event) {
        recordGcPauseEvent(event);

    }

    private void processEvent(ShenandoahCycle event) {
        recordGcPauseEvent(event);
    }
}
