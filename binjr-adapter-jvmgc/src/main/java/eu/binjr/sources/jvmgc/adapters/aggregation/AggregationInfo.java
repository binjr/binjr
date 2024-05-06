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

import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public record AggregationInfo(List<String> category,
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
        this(List.of(category), name, label, unit, prefix, chartType,color, new ConcurrentSkipListMap<>());
    }
    public AggregationInfo(List<String> categories,
                           String name,
                           String label,
                           String unit,
                           UnitPrefixes prefix,
                           ChartType chartType,
                           Color color) {
        this(categories, name, label, unit, prefix, chartType,color, new ConcurrentSkipListMap<>());
    }

}

