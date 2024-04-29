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
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.shenandoah.ShenandoahCycle;
import com.microsoft.gctoolkit.event.zgc.ZGCCycle;


@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC, EventSource.SHENANDOAH})
public class GcAggregator extends Aggregator<GcAggregation> {

    public GcAggregator(GcAggregation results) {
        super(results);
        register(GenerationalGCPauseEvent.class, this::processEvent);
        register(G1GCPauseEvent.class, this::processEvent);
        register(ZGCCycle.class, this::processEvent);
        register(ShenandoahCycle.class, this::processEvent);
    }

    private void processEvent(GenerationalGCPauseEvent event) {
        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());
        if (event.getHeap() != null) {
            aggregation().addHeapOccupancyDataPoint(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyAfterCollection()*1024L);
        }
    }

    private void processEvent(G1GCPauseEvent event) {
        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());

        if (event.getHeap() != null) {
            aggregation().addHeapOccupancyDataPoint(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getHeap().getOccupancyAfterCollection()*1024L);
        }

    }

    private void processEvent(ZGCCycle event) {
        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());

        if (event.getLive() != null) {
            aggregation().addHeapOccupancyDataPoint(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getLive().getReclaimEnd()*1024L);
        }
    }

    private void processEvent(ShenandoahCycle event) {
        aggregation().recordPauseDuration(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.getDuration());

        //  aggregation().addDataPoint(event.getGarbageCollectionType(), event.getDateTimeStamp(), event.get ());
    }
}
