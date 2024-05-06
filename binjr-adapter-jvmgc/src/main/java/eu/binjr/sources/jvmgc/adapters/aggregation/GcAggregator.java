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

    public static final String UNIT_SECONDS = "seconds";
    public static final String REF_JNI_WEAK = "Jni Weak";
    public static final String REF_PHANTOM = "Phantom";
    public static final String REF_SOFT = "Soft";
    public static final String REF_WEAK = "Weak";
    public static final String REF_FINAL = "Final";
    public static final String UNIT_BYTES = "bytes";
    public static final String METASPACE = "Metaspace";
    public static final String NON_CLASS_SPACE = "NonClassSpace";
    public static final String CLASS_SPACE = "ClassSpace";
    public static final String TENURED = "Tenured";
    public static final String YOUNG = "Young";
    public static final String SURVIVOR = "Survivor";
    public static final String EDEN = "Eden";
    public static final String OCCUPANCY = "Occupancy";
    public static final String SIZE = "Size";
    public static final String PAUSE_TIME = "Pause Time";
    public static final String CPU = "CPU";
    public static final String REFERENCES = "References";

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
                aggregation().storeSample(List.of(OCCUPANCY, "Total Heap"),
                        "HeapOccupancyMerged", "Heap (Merged)",
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.STEELBLUE,
                        gcEvent.getGarbageCollectionType(),
                        shiftDateTimeSamp(gcEvent, -1 * gcEvent.getDuration()),
                        occupancyBeforeGc * 1024L);
                aggregation().storeSample(List.of(OCCUPANCY, "Total Heap"),
                        "HeapOccupancyBeforeCollection", "Heap (Before GC)",
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.SEAGREEN,
                        gcEvent.getGarbageCollectionType(),
                        gcEvent.getDateTimeStamp(),
                        occupancyBeforeGc * 1024L);
            }
            var occupancyAfterGc = memPool.getOccupancyAfterCollection();
            if (occupancyAfterGc >= 0) {
                aggregation().storeSample(List.of(OCCUPANCY, "Total Heap"),
                        "HeapOccupancyAfterCollection", "Heap (After GC)",
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.TOMATO,
                        gcEvent.getGarbageCollectionType(),
                        gcEvent.getDateTimeStamp(),
                        occupancyAfterGc * 1024L);

                aggregation().storeSample(List.of(OCCUPANCY, "Total Heap"),
                        "HeapOccupancyMerged", "Heap (Merged)",
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
            aggregation().storeSample(List.of(SIZE, "Size (Before GC)"),
                    poolName + "SizeBeforeCollection", poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeBeforeGc * 1024L);
        }
        if (sizeAfterGc >= 0) {
            aggregation().storeSample(List.of(SIZE, "Size (After GC)"),
                    poolName + "SizeAfterCollection", poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    sizeAfterGc * 1024L);
        }
        if (occupancyBeforeGc >= 0) {
            aggregation().storeSample(List.of(OCCUPANCY, "Detailed (Merged)"),
                    poolName + "OccupancyCompounded", poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    shiftDateTimeSamp(gcEvent, -1 * gcEvent.getDuration()),
                    occupancyBeforeGc * 1024L);
            aggregation().storeSample(List.of(OCCUPANCY, "Detailed (Before GC)"),
                    poolName + "OccupancyBeforeCollection", poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyBeforeGc * 1024L);
        }
        if (occupancyAfterGc >= 0) {
            aggregation().storeSample(List.of(OCCUPANCY, "Detailed (Merged)"),
                    poolName + "OccupancyCompounded", poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY,
                    ChartType.STACKED, color,
                    gcEvent.getGarbageCollectionType(),
                    gcEvent.getDateTimeStamp(),
                    occupancyAfterGc * 1024L);
            aggregation().storeSample(List.of(OCCUPANCY, "Detailed (After GC)"),
                    poolName + "OccupancyAfterCollection", poolName,
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
            aggregation().storeSample(PAUSE_TIME,
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
            aggregation().storeSample(List.of(CPU, "CPU Time"),
                    "CpuSummaryKernel",
                    "Kernel",
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.STACKED,
                    Color.RED,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    cpuSummary.getKernel());
            aggregation().storeSample(List.of(CPU, "CPU Time"),
                    "CpuSummaryUser",
                    "User",
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.STACKED,
                    Color.GREEN,
                    event.getGarbageCollectionType(),
                    event.getDateTimeStamp(),
                    cpuSummary.getUser());
            aggregation().storeSample(List.of(CPU, "CPU (Wall clock)"),
                    "CpuSummaryWallClock",
                    "CPU (Wall Clock)",
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
            aggregation().storeSample(List.of(REFERENCES, "References (Count)"),
                    refType.replace(" ", "") + "RefCount",
                    refType,
                    "",
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
            aggregation().storeSample(List.of(REFERENCES, "References (Pause Time)"),
                    refType.replace(" ", "") + "RefPauseTime",
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
            addRefCount(event, REF_JNI_WEAK,Color.CHARTREUSE, referenceGCSummary.getJniWeakReferenceCount());
            addRefPauseTime(event, REF_JNI_WEAK,Color.CHARTREUSE, referenceGCSummary.getJniWeakReferencePauseTime());
            addRefCount(event, REF_PHANTOM,Color.TOMATO, referenceGCSummary.getPhantomReferenceCount());
            addRefPauseTime(event, REF_PHANTOM,Color.TOMATO, referenceGCSummary.getPhantomReferencePauseTime());
            addRefCount(event, REF_SOFT,Color.SEAGREEN, referenceGCSummary.getSoftReferenceCount());
            addRefPauseTime(event, REF_SOFT,Color.SEAGREEN, referenceGCSummary.getSoftReferencePauseTime());
            addRefCount(event, REF_WEAK,Color.DEEPPINK, referenceGCSummary.getWeakReferenceCount());
            addRefPauseTime(event, REF_WEAK,Color.DEEPPINK, referenceGCSummary.getWeakReferencePauseTime());
            addRefCount(event, REF_FINAL,Color.MEDIUMPURPLE, referenceGCSummary.getFinalReferenceCount());
            addRefPauseTime(event, REF_FINAL,Color.MEDIUMPURPLE, referenceGCSummary.getFinalReferencePauseTime());
        }
    }

    private void processEvent(GenerationalGCPauseEvent event) {
        recordGcPauseEvent(event);
        recordCpuStats(event, event.getCpuSummary());
        recordTotalHeapStats(event.getHeap(), event);
        recordMemPoolStats(METASPACE, event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        recordMemPoolStats(NON_CLASS_SPACE, event.getNonClassspace(), event, Color.MEDIUMPURPLE);
        recordMemPoolStats(CLASS_SPACE, event.getClassspace(), event, Color.AQUAMARINE);
        recordMemPoolStats(TENURED, event.getTenured(), event, Color.SEAGREEN);
        recordMemPoolStats(YOUNG, event.getYoung(), event, Color.GOLD);
        recordCpuStats(event, event.getCpuSummary());
        recordReferenceStats(event, event.getReferenceGCSummary());
    }


    private void processEvent(G1GCPauseEvent event) {
        recordGcPauseEvent(event);
        recordTotalHeapStats(event.getHeap(), event);
        recordMemPoolStats(METASPACE, event.getPermOrMetaspace(), event, Color.CHOCOLATE);
        recordMemPoolStats(TENURED, event.getTenured(), event, Color.SEAGREEN);
        if (event.getSurvivor() != null) {
            recordMemPoolStats(SURVIVOR,
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getSize(),
                    event.getSurvivor().getOccupancyBeforeCollection(),
                    event.getSurvivor().getOccupancyAfterCollection(),
                    event, Color.DEEPSKYBLUE);
        }
        recordMemPoolStats(EDEN, event.getEden(), event, Color.GOLD);
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
