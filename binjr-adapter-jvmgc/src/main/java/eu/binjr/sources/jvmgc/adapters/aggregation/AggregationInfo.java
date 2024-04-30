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
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import io.vertx.core.impl.ConcurrentHashSet;
import javafx.scene.paint.Color;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public record AggregationInfo(String category,
                              String name,
                              String label,
                              String unit,
                              UnitPrefixes prefix,
                              ChartType chartType,
                              Color color,
                              ConcurrentNavigableMap<Long, Sample> data) {
    public AggregationInfo(String category,
                           String name,
                           String label,
                           String unit,
                           UnitPrefixes prefix,
                           ChartType chartType,
                           Color color) {
        this(category, name, label, unit, prefix, chartType,color, new ConcurrentSkipListMap<>());
    }

    public AggregationInfo(String category,
                           String name,
                           String label,
                           String unit,
                           UnitPrefixes prefix,
                           ChartType chartType,
                           Color color,
                           ConcurrentNavigableMap<Long, Sample> data) {
        this.category = category;
        this.name = name;
        this.label = label;
        this.unit = unit;
        this.prefix = prefix;
        this.chartType = chartType;
        this.color = color;
        this.data = data;
    }
}

