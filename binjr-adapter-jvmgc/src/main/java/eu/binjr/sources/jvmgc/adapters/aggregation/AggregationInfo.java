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
import eu.binjr.core.data.codec.csv.DataSample;
import eu.binjr.core.data.workspace.UnitPrefixes;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public record AggregationInfo(String name,
                              String label,
                              String unit,
                              UnitPrefixes prefix,
                              Set<GarbageCollectionTypes> encounteredGcTypes,
                              ConcurrentNavigableMap<Long, DataSample> data) {
    public AggregationInfo(String name,
                           String label,
                           String unit,
                           UnitPrefixes prefix) {
        this(name, label,unit, prefix, new ConcurrentHashSet<>(), new ConcurrentSkipListMap<>());
    }

    public AggregationInfo(String name,
                           String label,
                           String unit,
                           UnitPrefixes prefix,
                           Set<GarbageCollectionTypes> encounteredGcTypes,
                           ConcurrentNavigableMap<Long, DataSample> data) {
        this.name = name;
        this.label = label;
        this.unit = unit;
        this.prefix = prefix;
        this.encounteredGcTypes = encounteredGcTypes;
        this.data = data;
    }
}

