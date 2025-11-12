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
import com.microsoft.gctoolkit.event.*;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.shenandoah.ShenandoahCycle;
import com.microsoft.gctoolkit.event.zgc.*;
import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.common.io.IOUtils;
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
    public static final String CAT_REGIONS_AFTER_GC = "Regions (After GC)";
    public static final String CAT_REGIONS_BEFORE_GC = "Regions (Before GC)";
    public static final String SUFFIX_AFTER_GC = " (After GC)";
    public static final String SUFFIX_BEFORE_GC = " (Before GC)";
    public static final String CAT_TOTAL_HEAP = "Total Heap";
    public static final String CAT_HEAP_AFTER_GC = "Heap (After GC)";
    public static final String CAT_HEAP_BEFORE_GC = "Heap (Before GC)";
    public static final String ID_ALLOCATION_SIZE = "AllocationSize";
    public static final String CAT_ALLOCATION_SIZE = "Allocation Size";
    public static final String CAT_ZGC_PHASES = "Concurrent Phases";
    public static final String CAT_ZGC_PAUSES = "Pauses";
    public static final String ZGC_LIVE = "Live";
    public static final String ZGC_GARBAGE = "Garbage";
    public static final String ZGC_CAPACITY_SUMMARY = "Capacity Summary";
    public static final String ZGC_HEAP_STATISTICS = "Heap Statistics";
    public static final String CAT_CAPACITY = "Capacity";
    public static final String CAT_REGIONS = "Regions";
    public static final String ZGC_MARK_START = "Mark Start";
    public static final String ZGC_MARK_END = "Mark End";
    public static final String ZGC_RELOCATE_START = "Relocate Start";
    public static final String ZGC_RELOCATE_END = "Relocate End";
    public static final String ZGC_ALLOCATED = "Allocated";
    public static final String ZGC_RECLAIMED = "Reclaimed";
    public static final String ZGC_PROMOTED = "Promoted";
    public static final String ZGC_COMPACTED = "Compacted";
    public static final String ZGC_USED = "Used";
    public static final String ZGC_HEAP_FREE = "Free";
    public static final String ZGC_HEAP_USED = "Used";
    public static final String ZGC_COMMITTED = "Committed";
    public static final String ZGC_RESERVED = "Reserved";
    public static final String ZGC_CONCURRENT_REMAP_REMEMBERED = "Concurrent Remap Remembered";
    public static final String ZGC_CONCURRENT_REMAP_ROOTS_UNCOLORED = "Concurrent Remap Roots Uncolored";
    public static final String ZGC_CONCURRENT_REMAP_ROOTS_COLORED = "Concurrent Remap Roots Colored";
    public static final String ZGC_CONCURRENT_REMAP_ROOTS = "Concurrent Remap Roots";
    public static final String AGC_CONCURRENT_RELOCATE = "Concurrent Relocate";
    public static final String ZGC_CONCURRENT_RELOCATION_SET_SELECTION = "Concurrent Relocation Set Selection";
    public static final String ZGC_CONCURRENT_REFERENCE_PROCESSING = "Concurrent Reference Processing";
    public static final String ZGC_CONCURRENT_MARK_FOLLOW = "Concurrent Mark Follow";
    public static final String ZGC_CONCURRENT_MARK = "Concurrent Mark";
    public static final String ZGC_CONCURRENT_MARK_CONTINUE = "Concurrent Mark Continue";
    public static final String ZGC_CONCURRENT_MARK_FREE = "Concurrent Mark Free";
    public static final String ZGC_CONCURRENT_MARK_ROOTS = "Concurrent Mark Roots";
    public static final String MIN_CAPACITY = "Min Capacity";
    public static final String MAX_CAPACITY = "Max Capacity";
    public static final String SOFT_MAX_CAPACITY = "Soft Max Capacity";
    public static final String POOL_OLD = "Old";
    public static final String POOL_HUMONGOUS = "Humongous";
    public static final String CAT_HEAP = "Heap";
    public static final String CAT_METASPACE = "Metaspace";
    public static final String CAT_CLASS_SPACE = "Class Space";
    private static final String SUFFIX_SIZE = " Size";
    private static final String CAT_NON_CLASS_SPACE = "Non Class Space";
    public static final String POOL_TOTAL_METASPACE = "Total Metaspace";


    public GcAggregator(GcAggregation results) {
        super(results);
        register(GenerationalGCPauseEvent.class, this::processEvent);
        register(G1GCPauseEvent.class, this::processEvent);
        register(ZGCCollection.class, this::processEvent);
        register(ShenandoahCycle.class, this::processEvent);
    }


    private void storeZgcPhaseEvent(List<String> categories, String label,
                                    DateTimeStamp timeStamp, double duration) {
        if (timeStamp != null) {
            aggregation().storeSample(categories,
                    (String.join("_", categories) + label).replace(" ", ""),
                    label,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.SCATTER,
                    null,
                    timeStamp,
                    duration / 1000.0);
        }
    }

    private void storeZgcMemSummaryEvent(List<String> categories, String label, ChartType chartType,
                                         DateTimeStamp timeStamp, long size) {
        if (timeStamp != null) {
            aggregation().storeSample(categories,
                    (String.join("_", categories) + label).replace(" ", ""),
                    label,
                    UNIT_BYTES,
                    UnitPrefixes.BINARY,
                    chartType,
                    null,
                    timeStamp,
                    size * 1024);
        }
    }

    private void storeZgcPageSummary(String pageSize, ZGCPageSummary pageSummary, DateTimeStamp timeStamp) {
        if (pageSummary != null && timeStamp != null) {
            storeZgcCountLine(List.of("Pages", "Count", pageSize),
                    pageSize + " Candidates",
                    timeStamp,
                    pageSummary.getCandidates());
            storeZgcCountLine(List.of("Pages", "Count", pageSize),
                    pageSize + " Selected",
                    timeStamp,
                    pageSummary.getSelected());
            storeZgcCountLine(List.of("Pages", "Count", pageSize),
                    pageSize + "  In-Place",
                    timeStamp,
                    pageSummary.getInPlace());

            storeZgcMemSummaryEvent(List.of("Pages", "Footprint", pageSize),
                    pageSize + " Size",
                    ChartType.AREA,
                    timeStamp,
                    pageSummary.getSize());
            storeZgcMemSummaryEvent(List.of("Pages", "Footprint", pageSize),
                    pageSize + " Empty",
                    ChartType.AREA,
                    timeStamp,
                    pageSummary.getEmpty());
            storeZgcMemSummaryEvent(List.of("Pages", "Footprint", pageSize),
                    pageSize + "  Relocated",
                    ChartType.AREA,
                    timeStamp,
                    pageSummary.getRelocated());
        }
    }

    private void storeZgcReferenceSummary(String referenceType, ZGCReferenceSummary referenceSummary, DateTimeStamp timeStamp) {
        if (referenceSummary != null && timeStamp != null) {
            storeZgcCountLine(List.of("References", referenceType),
                    referenceType + " Encountered",
                    timeStamp,
                    referenceSummary.getEncountered());
            storeZgcCountLine(List.of("References", referenceType),
                    referenceType + " Discovered",
                    timeStamp,
                    referenceSummary.getDiscovered());
            storeZgcCountLine(List.of("References", referenceType),
                    referenceType + " Enqueued",
                    timeStamp,
                    referenceSummary.getEnqueued());
        }
    }

    private void storeZgcCountPoint(List<String> categories, String label, DateTimeStamp timeStamp, double count) {
        storeZgcCount(categories, label, ChartType.SCATTER, timeStamp, count);
    }

    private void storeZgcCountLine(List<String> categories, String label, DateTimeStamp timeStamp, double count) {
        storeZgcCount(categories, label, ChartType.LINE, timeStamp, count);
    }

    private void storeZgcPercentage(List<String> categories, String label, ChartType chartType, DateTimeStamp timeStamp, double percent) {
        aggregation().storeSample(categories,
                (String.join("_", categories) + label).replace(" ", ""),
                label,
                "%",
                UnitPrefixes.PERCENTAGE,
                chartType,
                null,
                timeStamp,
                percent / 100.0);
    }

    private void storeZgcCount(List<String> categories, String label, ChartType chartType, DateTimeStamp timeStamp, double count) {
        aggregation().storeSample(categories,
                (String.join("_", categories) + label).replace(" ", ""),
                label,
                "count",
                UnitPrefixes.METRIC,
                chartType,
                null,
                timeStamp,
                count);
    }

    private void recordZgcHeapStats(String phase, ZGCMemoryPoolSummary memoryPoolSummary, DateTimeStamp timeStamp) {
        if (memoryPoolSummary != null && timeStamp != null) {
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_HEAP_STATISTICS, CAT_CAPACITY),
                    phase,
                    ChartType.LINE,
                    timeStamp,
                    memoryPoolSummary.getCapacity());
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_HEAP_STATISTICS, CAT_OCCUPANCY, phase),
                    ZGC_HEAP_USED,
                    ChartType.STACKED,
                    timeStamp,
                    memoryPoolSummary.getUsed());
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_HEAP_STATISTICS, CAT_OCCUPANCY, phase),
                    ZGC_HEAP_FREE,
                    ChartType.STACKED,
                    timeStamp,
                    memoryPoolSummary.getFree());
        }
    }

    private void recordZgcHeapSummary(ZGCPhase zgcPhase,
                                      ZGCUsedSummary occupancy,
                                      ZGCLiveSummary live,
                                      ZGCGarbageSummary garbage,
                                      ZGCAllocatedSummary allocated,
                                      ZGCReclaimedSummary reclaimed,
                                      ZGCPromotedSummary promoted,
                                      ZGCCompactedSummary compacted,
                                      DateTimeStamp timeStamp) {
        var catRoot = inferCategoryFromGen(List.of(POOL_HEAP, ZGC_HEAP_STATISTICS, CAT_REGIONS), zgcPhase);
        if (occupancy != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_MARK_START),
                    ZGC_USED,
                    ChartType.LINE,
                    timeStamp,
                    occupancy.getMarkStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_MARK_END),
                    ZGC_USED,
                    ChartType.LINE,
                    timeStamp,
                    occupancy.getMarkEnd());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_USED,
                    ChartType.LINE,
                    timeStamp,
                    occupancy.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_USED,
                    ChartType.LINE,
                    timeStamp,
                    occupancy.getRelocateEnd());

        }
        if (live != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_MARK_END),
                    ZGC_LIVE,
                    ChartType.LINE,
                    timeStamp,
                    live.getMarkEnd());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_LIVE,
                    ChartType.LINE,
                    timeStamp,
                    live.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_LIVE,
                    ChartType.LINE,
                    timeStamp,
                    live.getRelocateEnd());
        }
        if (garbage != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_MARK_END),
                    ZGC_GARBAGE,
                    ChartType.LINE,
                    timeStamp,
                    garbage.getMarkEnd());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_GARBAGE,
                    ChartType.LINE,
                    timeStamp,
                    garbage.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_GARBAGE,
                    ChartType.LINE,
                    timeStamp,
                    garbage.getRelocateEnd());
        }
        if (allocated != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_MARK_END),
                    ZGC_ALLOCATED,
                    ChartType.LINE,
                    timeStamp,
                    allocated.getMarkEnd());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_ALLOCATED,
                    ChartType.LINE,
                    timeStamp,
                    allocated.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_ALLOCATED,
                    ChartType.LINE,
                    timeStamp,
                    allocated.getRelocateEnd());
        }
        if (reclaimed != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_RECLAIMED,
                    ChartType.LINE,
                    timeStamp,
                    reclaimed.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_RECLAIMED,
                    ChartType.LINE,
                    timeStamp,
                    reclaimed.getRelocateEnd());
        }
        if (promoted != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_START),
                    ZGC_PROMOTED,
                    ChartType.LINE,
                    timeStamp,
                    promoted.getRelocateStart());
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_PROMOTED,
                    ChartType.LINE,
                    timeStamp,
                    promoted.getRelocateEnd());
        }
        if (compacted != null) {
            storeZgcMemSummaryEvent(IOUtils.concat(catRoot, ZGC_RELOCATE_END),
                    ZGC_COMPACTED,
                    ChartType.LINE,
                    timeStamp,
                    compacted.getRelocateEnd());
        }

    }

    private List<String> inferCategoryFromGen(List<String> catRoot, ZGCPhase phase) {
        return switch (phase) {
            case FULL -> catRoot;
            case MAJOR_YOUNG, MINOR_YOUNG -> IOUtils.concat(catRoot, "Young Generation");
            case MAJOR_OLD -> IOUtils.concat(catRoot, "Old Generation");
        };
    }

    private void recordZgCReferenceAndPagesStats(ZGCCollection zgcCollection) {
        storeZgcReferenceSummary("Soft References", zgcCollection.getSoftRefSummary(), zgcCollection.getDateTimeStamp());
        storeZgcReferenceSummary("Weak References", zgcCollection.getWeakRefSummary(), zgcCollection.getDateTimeStamp());
        storeZgcReferenceSummary("Final References", zgcCollection.getFinalRefSummary(), zgcCollection.getDateTimeStamp());
        storeZgcReferenceSummary("Phantom References", zgcCollection.getPhantomRefSummary(), zgcCollection.getDateTimeStamp());

        storeZgcPageSummary("Small Pages", zgcCollection.getSmallPageSummary(), zgcCollection.getDateTimeStamp());
        storeZgcPageSummary("Medium Pages", zgcCollection.getMediumPageSummary(), zgcCollection.getDateTimeStamp());
        storeZgcPageSummary("Large Pages", zgcCollection.getLargePageSummary(), zgcCollection.getDateTimeStamp());

        var ageSummaries = zgcCollection.getAgeTableSummary();
        if (ageSummaries != null) {
            for (var summary : ageSummaries) {
                storeZgcMemSummaryEvent(List.of("Pages", "Age", summary.getName(), "Footprint"),
                        ZGC_LIVE,
                        ChartType.STACKED,
                        zgcCollection.getDateTimeStamp(),
                        summary.getLive());
                storeZgcMemSummaryEvent(List.of("Pages", "Age", summary.getName(), "Footprint"),
                        ZGC_GARBAGE,
                        ChartType.STACKED,
                        zgcCollection.getDateTimeStamp(),
                        summary.getGarbage());

                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Small Pages"),
                        "Candidates",
                        zgcCollection.getDateTimeStamp(),
                        summary.getSmallPageCandidates());

                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Small Pages"),
                        "Selected",
                        zgcCollection.getDateTimeStamp(),
                        summary.getSmallPageSelected());
                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Medium Pages"),
                        "Candidates",
                        zgcCollection.getDateTimeStamp(),
                        summary.getMediumPageCandidates());

                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Medium Pages"),
                        "Selected",
                        zgcCollection.getDateTimeStamp(),
                        summary.getMediumPageSelected());
                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Large Pages"),
                        "Candidates",
                        zgcCollection.getDateTimeStamp(),
                        summary.getLargePageCandidates());

                storeZgcCountLine(List.of("Pages", "Age", summary.getName(), "Large Pages"),
                        "Selected",
                        zgcCollection.getDateTimeStamp(),
                        summary.getLargePageSelected());
            }
        }

        storeZgcMemSummaryEvent(List.of("Pages", "Forwarding Usage"),
                "Forwarding Usage",
                ChartType.AREA,
                zgcCollection.getDateTimeStamp(),
                zgcCollection.getForwardingUsage());

    }

    private void recordZgcPhasesStats(ZGCCollection zgcCollection) {
        var catPauseRoot = inferCategoryFromGen(List.of(CAT_ZGC_PAUSES), zgcCollection.getPhase());
        storeZgcPhaseEvent(catPauseRoot,
                ZGCPauseTypes.MARK_START.getLabel(),
                zgcCollection.getPauseMarkStartTimeStamp(),
                zgcCollection.getPauseMarkStartDuration());
        storeZgcPhaseEvent(catPauseRoot,
                ZGCPauseTypes.MARK_END.getLabel(),
                zgcCollection.getPauseMarkEndTimeStamp(),
                zgcCollection.getPauseMarkEndDuration());

        storeZgcPhaseEvent(catPauseRoot,
                ZGCPauseTypes.RELOCATE_START.getLabel(),
                zgcCollection.getPauseRelocateStartTimeStamp(),
                zgcCollection.getPauseRelocateStartDuration());

        var catPhaseRoot = inferCategoryFromGen(List.of(CAT_ZGC_PHASES), zgcCollection.getPhase());
        var catPhaseDurations = IOUtils.concat(catPhaseRoot, "Durations");
        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_MARK,
                zgcCollection.getConcurrentMarkTimeStamp(),
                zgcCollection.getConcurrentMarkDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_MARK_CONTINUE,
                zgcCollection.getConcurrentMarkContinueTimeStamp(),
                zgcCollection.getConcurrentMarkContinueDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_MARK_FREE,
                zgcCollection.getConcurrentMarkFreeTimeStamp(),
                zgcCollection.getConcurrentMarkFreeDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_MARK_ROOTS,
                zgcCollection.getMarkRootsStart(),
                zgcCollection.getMarkRootsDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_MARK_FOLLOW,
                zgcCollection.getMarkFollowStart(),
                zgcCollection.getMarkFollowDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_REFERENCE_PROCESSING,
                zgcCollection.getConcurrentProcessNonStrongReferencesTimeStamp(),
                zgcCollection.getConcurrentProcessNonStrongReferencesDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_RELOCATION_SET_SELECTION,
                zgcCollection.getConcurrentSelectRelocationSetTimeStamp(),
                zgcCollection.getConcurrentSelectRelocationSetDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                AGC_CONCURRENT_RELOCATE,
                zgcCollection.getConcurrentRelocateTimeStamp(),
                zgcCollection.getConcurrentRelocateDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_REMAP_ROOTS,
                zgcCollection.getConcurrentRemapRootsStart(),
                zgcCollection.getConcurrentRemapRootsDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_REMAP_ROOTS_COLORED,
                zgcCollection.getRemapRootColoredStart(),
                zgcCollection.getRemapRootsColoredDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_REMAP_ROOTS_UNCOLORED,
                zgcCollection.getRemapRootsUncoloredStart(),
                zgcCollection.getRemapRootsUncoloredDuration());

        storeZgcPhaseEvent(catPhaseDurations,
                ZGC_CONCURRENT_REMAP_REMEMBERED,
                zgcCollection.getRemapRememberedStart(),
                zgcCollection.getRemapRememberedDuration());


        var catMarkSummary = IOUtils.concat(catPhaseRoot, "Mark Summary");
        var markSummary = zgcCollection.getMarkSummary();
        if (markSummary != null) {
            storeZgcCountPoint(catMarkSummary,
                    "Stripes",
                    zgcCollection.getDateTimeStamp(),
                    markSummary.getStripes());
            storeZgcCountPoint(catMarkSummary,
                    "Proactive Flushes",
                    zgcCollection.getDateTimeStamp(),
                    markSummary.getProactiveFlushes());
            storeZgcCountPoint(catMarkSummary,
                    "Terminate Flushes",
                    zgcCollection.getDateTimeStamp(),
                    markSummary.getTerminatedFlushes());
            storeZgcCountPoint(catMarkSummary,
                    "Completions",
                    zgcCollection.getDateTimeStamp(),
                    markSummary.getCompletions());
            storeZgcCountPoint(catMarkSummary,
                    "Continuations",
                    zgcCollection.getDateTimeStamp(),
                    markSummary.getContinuations());
        }
    }

    private void recordZgcHeapStats(ZGCCollection zgcCollection) {
        recordZgcHeapStats(ZGC_MARK_START, zgcCollection.getMarkStart(), zgcCollection.getDateTimeStamp());
        recordZgcHeapStats(ZGC_MARK_END, zgcCollection.getMarkEnd(), zgcCollection.getDateTimeStamp());
        recordZgcHeapStats(ZGC_RELOCATE_START, zgcCollection.getRelocateStart(), zgcCollection.getDateTimeStamp());
        recordZgcHeapStats(ZGC_RELOCATE_END, zgcCollection.getRelocateEnd(), zgcCollection.getDateTimeStamp());

        recordZgcHeapSummary(zgcCollection.getPhase(),
                zgcCollection.getUsedSummary(),
                zgcCollection.getLiveSummary(),
                zgcCollection.getGarbageSummary(),
                zgcCollection.getAllocatedSummary(),
                zgcCollection.getReclaimedSummary(),
                zgcCollection.getPromotedSummary(),
                zgcCollection.getCompactedSummary(),
                zgcCollection.getDateTimeStamp());

        var heapCapacitySummary = zgcCollection.getHeapCapacitySummary();
        if (heapCapacitySummary != null) {
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_CAPACITY_SUMMARY),
                    MIN_CAPACITY,
                    ChartType.LINE,
                    zgcCollection.getDateTimeStamp(),
                    heapCapacitySummary.getMinCapacity());
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_CAPACITY_SUMMARY),
                    MAX_CAPACITY,
                    ChartType.LINE,
                    zgcCollection.getDateTimeStamp(),
                    heapCapacitySummary.getMaxCapacity());
            storeZgcMemSummaryEvent(List.of(POOL_HEAP, ZGC_CAPACITY_SUMMARY),
                    SOFT_MAX_CAPACITY,
                    ChartType.LINE,
                    zgcCollection.getDateTimeStamp(),
                    heapCapacitySummary.getSoftMaxCapacity());
        }

    }

    private void processEvent(ZGCCollection zgcCollection) {
        recordZgcPhasesStats(zgcCollection);
        recordZgcHeapStats(zgcCollection);
        recordZgCReferenceAndPagesStats(zgcCollection);

        var metaspaceSummary = zgcCollection.getMetaspaceSummary();
        if (metaspaceSummary != null) {
            storeZgcMemSummaryEvent(List.of(POOL_METASPACE),
                    ZGC_RESERVED,
                    ChartType.AREA,
                    zgcCollection.getDateTimeStamp(),
                    metaspaceSummary.getReserved());
            storeZgcMemSummaryEvent(List.of(POOL_METASPACE),
                    ZGC_COMMITTED,
                    ChartType.AREA,
                    zgcCollection.getDateTimeStamp(),
                    metaspaceSummary.getCommitted());
            storeZgcMemSummaryEvent(List.of(POOL_METASPACE),
                    ZGC_USED,
                    ChartType.AREA,
                    zgcCollection.getDateTimeStamp(),
                    metaspaceSummary.getUsed());
        }

        var nMethodSummary = zgcCollection.getNMethodSummary();
        if (nMethodSummary != null) {
            storeZgcCountLine(List.of("NMethods"),
                    "Registered",
                    zgcCollection.getDateTimeStamp(),
                    nMethodSummary.getRegistered());
            storeZgcCountLine(List.of("NMethods"),
                    "Unregistered",
                    zgcCollection.getDateTimeStamp(),
                    nMethodSummary.getUnregistered());

        }
        var catMmu = List.of("MMU");
        String[] mmuLabels = new String[]{"2ms", "5ms", "10ms", "20ms", "50ms", "100ms"};
        for (int i = 0; i < mmuLabels.length; i++) {
            storeZgcPercentage(catMmu,
                    mmuLabels[i],
                    ChartType.LINE,
                    zgcCollection.getDateTimeStamp(),
                    zgcCollection.getMmu()[i]);
        }
        var catLoad = List.of("Load Average");
        String[] loadLabels = new String[]{"5", "10", "15"};
        for (int i = 0; i < loadLabels.length; i++) {
            storeZgcCountLine(catLoad,
                    loadLabels[i],
                    zgcCollection.getDateTimeStamp(),
                    zgcCollection.getLoad()[i]);
        }
    }

    private void recordTotalHeapStats(MemoryPoolSummary memPool,
                                      DateTimeStamp tsBeforeGc,
                                      DateTimeStamp tsAfterGC) {
        if (memPool != null) {
            var occupancyBeforeGc = memPool.getOccupancyBeforeCollection();
            if (occupancyBeforeGc >= 0) {
                aggregation().storeSample(List.of(CAT_HEAP, CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_MERGED, CAT_HEAP,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.STEELBLUE,
                        tsBeforeGc,
                        occupancyBeforeGc * 1024L);
                aggregation().storeSample(List.of(CAT_HEAP, CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_BEFORE_COLLECTION, CAT_HEAP_BEFORE_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.SEAGREEN,
                        tsBeforeGc,
                        occupancyBeforeGc * 1024L);
            }
            var occupancyAfterGc = memPool.getOccupancyAfterCollection();
            if (occupancyAfterGc >= 0) {
                aggregation().storeSample(List.of(CAT_HEAP, CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_AFTER_COLLECTION, CAT_HEAP_AFTER_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.TOMATO,
                        tsAfterGC,
                        occupancyAfterGc * 1024L);

                aggregation().storeSample(List.of(CAT_HEAP, CAT_OCCUPANCY, CAT_TOTAL_HEAP),
                        POOL_HEAP + ID_OCCUPANCY_MERGED, CAT_HEAP,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.LINE, Color.STEELBLUE,
                        tsAfterGC,
                        occupancyAfterGc * 1024L);
            }
        }
    }

    private void recordMetaspaceStats(GenerationalGCPauseEvent event,
                                      DateTimeStamp tsBeforeGc,
                                      DateTimeStamp tsAfterGC) {
      if (event != null) {
          recordMetaspaceStats(CAT_METASPACE, event.getPermOrMetaspace(), tsBeforeGc, tsAfterGC);
          recordMetaspaceStats(CAT_CLASS_SPACE, event.getClassspace(), tsBeforeGc, tsAfterGC);
          recordMetaspaceStats(CAT_NON_CLASS_SPACE, event.getNonClassspace(), tsBeforeGc, tsAfterGC);
      }
    }

    private void recordMetaspaceStats(G1GCPauseEvent event,
                                      DateTimeStamp tsBeforeGc,
                                      DateTimeStamp tsAfterGC) {
        if (event != null) {
            recordMetaspaceStats(POOL_TOTAL_METASPACE, event.getPermOrMetaspace(), tsBeforeGc, tsAfterGC);
            recordMetaspaceStats(CAT_CLASS_SPACE, event.getClassSpace(), tsBeforeGc, tsAfterGC);
        }
    }

    private void recordMetaspaceStats(String poolName,
                                      MemoryPoolSummary memPool,
                                      DateTimeStamp tsBeforeGc,
                                      DateTimeStamp tsAfterGC) {
        if (memPool != null) {
            var occupancyBeforeGc = memPool.getOccupancyBeforeCollection();
            if (occupancyBeforeGc >= 0) {
                aggregation().storeSample(List.of(CAT_METASPACE, CAT_OCCUPANCY + SUFFIX_BEFORE_GC),
                        poolName + ID_OCCUPANCY_BEFORE_COLLECTION, poolName + SUFFIX_BEFORE_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.AREA,
                        tsBeforeGc,
                        occupancyBeforeGc * 1024L);
            }

            var occupancyAfterGc = memPool.getOccupancyAfterCollection();
            if (occupancyAfterGc >= 0) {
                aggregation().storeSample(List.of(CAT_METASPACE, CAT_OCCUPANCY + SUFFIX_AFTER_GC),
                        poolName + ID_OCCUPANCY_AFTER_COLLECTION, poolName + SUFFIX_AFTER_GC,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.AREA,
                        tsAfterGC,
                        occupancyAfterGc * 1024L);
            }

            var sizeAfterGc = memPool.getSizeAfterCollection();
            if (sizeAfterGc >= 0) {
                aggregation().storeSample(List.of(CAT_METASPACE, CAT_SIZE),
                        poolName + ID_SIZE_AFTER_COLLECTION, poolName + SUFFIX_SIZE,
                        UNIT_BYTES, UnitPrefixes.BINARY, ChartType.AREA,
                        tsAfterGC,
                        sizeAfterGc * 1024L);
            }
        }
    }

    private void recordHeapGenerationStats(String poolName,
                                           MemoryPoolSummary memPool,
                                           DateTimeStamp tsBeforeGc,
                                           DateTimeStamp tsAfterGC,
                                           Color color) {
        if (memPool != null) {
            recordHeapGenerationStats(
                    poolName,
                    memPool.getOccupancyBeforeCollection(),
                    memPool.getOccupancyAfterCollection(),
                    tsBeforeGc,
                    tsAfterGC,
                    color);
        }
    }

    private void recordHeapGenerationStats(String poolName,
                                           long occupancyBeforeGc,
                                           long occupancyAfterGc,
                                           DateTimeStamp tsBeforeGc,
                                           DateTimeStamp tsAfterGC,
                                           Color color) {
        if (occupancyBeforeGc >= 0) {
            aggregation().storeSample(List.of(GcAggregator.CAT_HEAP, CAT_OCCUPANCY, CAT_REGIONS),
                    poolName + ID_OCCUPANCY_MERGED, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    tsBeforeGc,
                    occupancyBeforeGc * 1024L);
            aggregation().storeSample(List.of(GcAggregator.CAT_HEAP, CAT_OCCUPANCY, CAT_REGIONS_BEFORE_GC),
                    poolName + ID_OCCUPANCY_BEFORE_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY, ChartType.STACKED, color,
                    tsBeforeGc,
                    occupancyBeforeGc * 1024L);
        }
        if (occupancyAfterGc >= 0) {
            aggregation().storeSample(List.of(GcAggregator.CAT_HEAP, CAT_OCCUPANCY, CAT_REGIONS),
                    poolName + ID_OCCUPANCY_MERGED, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY,
                    ChartType.STACKED, color,
                    tsAfterGC,
                    occupancyAfterGc * 1024L);
            aggregation().storeSample(List.of(GcAggregator.CAT_HEAP, CAT_OCCUPANCY, CAT_REGIONS_AFTER_GC),
                    poolName + ID_OCCUPANCY_AFTER_COLLECTION, poolName,
                    UNIT_BYTES, UnitPrefixes.BINARY,
                    ChartType.STACKED, color,
                    tsAfterGC,
                    occupancyAfterGc * 1024L);
        }
    }

    private DateTimeStamp shiftDateTimeSamp(DateTimeStamp timeStamp, double shift) {
        if (timeStamp.getDateTime() == null) {
            return new DateTimeStamp(timeStamp.getTimeStamp() + shift);
        }
        return new DateTimeStamp(timeStamp.getDateTime().plus(Duration.ofMillis(Math.round(shift * 1000))),
                timeStamp.getTimeStamp() + shift);
    }

    private void recordGcPauseEvent(GCEvent event) {
        if (event.getDuration() >= 0) {
            aggregation().storeSample(CAT_PAUSE_TIME,
                    event.getGarbageCollectionType().name(),
                    event.getGarbageCollectionType().getLabel(),
                    UNIT_SECONDS, UnitPrefixes.METRIC, ChartType.SCATTER,
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
                    event.getDateTimeStamp(),
                    cpuSummary.getKernel());
            aggregation().storeSample(List.of(CAT_CPU, CAT_CPU_TIME),
                    ID_CPU_SUMMARY_USER,
                    CAT_CPU_USER,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.STACKED,
                    Color.GREEN,
                    event.getDateTimeStamp(),
                    cpuSummary.getUser());
            aggregation().storeSample(List.of(CAT_CPU, CAT_CPU_WALL_CLOCK),
                    ID_CPU_SUMMARY_WALL_CLOCK,
                    CAT_CPU_WALL_CLOCK,
                    UNIT_SECONDS,
                    UnitPrefixes.METRIC,
                    ChartType.AREA,
                    Color.STEELBLUE,
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
        var tsBeforeGc = event.getDateTimeStamp();
        var tsAfterGC = shiftDateTimeSamp(tsBeforeGc, event.getDuration());
        recordGcPauseEvent(event);
        recordCpuStats(event, event.getCpuSummary());
        recordTotalHeapStats(event.getHeap(), tsBeforeGc, tsAfterGC);
        recordMetaspaceStats(event, tsBeforeGc, tsAfterGC);
        recordHeapGenerationStats(POOL_NON_CLASS_SPACE, event.getNonClassspace(), tsBeforeGc, tsAfterGC, Color.MEDIUMPURPLE);
        recordHeapGenerationStats(POOL_CLASS_SPACE, event.getClassspace(), tsBeforeGc, tsAfterGC, Color.AQUAMARINE);
        recordHeapGenerationStats(POOL_TENURED, event.getTenured(), tsBeforeGc, tsAfterGC, Color.SEAGREEN);
        recordHeapGenerationStats(POOL_YOUNG, event.getYoung(), tsBeforeGc, tsAfterGC, Color.GOLD);
        recordCpuStats(event, event.getCpuSummary());
        recordReferenceStats(event, event.getReferenceGCSummary());
    }


    private void processEvent(G1GCPauseEvent event) {
        var tsBeforeGc = event.getDateTimeStamp();
        var tsAfterGC = shiftDateTimeSamp(tsBeforeGc, event.getDuration());
        recordGcPauseEvent(event);
        recordMetaspaceStats(event, tsBeforeGc, tsAfterGC);
        recordTotalHeapStats(event.getHeap(), tsBeforeGc, tsAfterGC);
        recordHeapGenerationStats(POOL_HUMONGOUS, event.getHumongous(), tsBeforeGc, tsAfterGC, Color.DARKOLIVEGREEN);
        recordHeapGenerationStats(POOL_OLD, event.getOld(), tsBeforeGc, tsAfterGC, Color.SEAGREEN);
        if (event.getSurvivor() != null) {
            recordHeapGenerationStats(
                    POOL_SURVIVOR,
                    event.getSurvivor().getOccupancyBeforeCollection(),
                    event.getSurvivor().getOccupancyAfterCollection(),
                    tsBeforeGc,
                    tsAfterGC,
                    Color.DEEPSKYBLUE);
        }
        recordHeapGenerationStats(POOL_EDEN, event.getEden(), tsBeforeGc, tsAfterGC, Color.GOLD);
        recordCpuStats(event, event.getCpuSummary());
        recordReferenceStats(event, event.getReferenceGCSummary());
    }

    private void processEvent(ShenandoahCycle event) {
        recordGcPauseEvent(event);
    }

}
