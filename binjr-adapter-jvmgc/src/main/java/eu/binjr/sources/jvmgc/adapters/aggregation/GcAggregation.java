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

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Collates;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.microsoft.gctoolkit.time.DateTimeStamp;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;


@Collates(GcAggregator.class)
public abstract class GcAggregation extends Aggregation {


    //    public abstract void addHeapSizeBeforeGc(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double... values);
//
//    abstract public void addHeapSizeAfterGc(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double... samples);
//
//    public abstract void addHeapOccupancyBeforeGc(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double... values);
//
//    abstract public void addHeapOccupancyAfterGc(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double... samples);
//
//    abstract public void recordPauseDuration(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, double duration);
    public void storeSample(String poolName,
                            String key,
                            String label,
                            String unit,
                            UnitPrefixes prefix,
                            ChartType chartType,
                            GarbageCollectionTypes gcType,
                            DateTimeStamp timeStamp,
                            double value) {
        storeSample(poolName, key, label, unit, prefix, chartType, null, gcType, timeStamp, value);
    }

    public abstract void storeSample(String poolName,
                                     String key,
                                     String label,
                                     String unit,
                                     UnitPrefixes prefix,
                                     ChartType chartType,
                                     Color color,
                                     GarbageCollectionTypes gcType,
                                     DateTimeStamp timeStamp,
                                     double value);
}
