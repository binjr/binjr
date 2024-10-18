/*
 *    Copyright 2017-2024 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.common.javafx.charts;

import eu.binjr.common.text.MetricPrefixFormatter;

/**
 * An implementation of {@link StableTicksAxis} that divide up large numbers by powers of 10 and apply metric unit prefixes
 *
 * @author Frederic Thevenet
 */
@Deprecated
public class MetricStableTicksAxis<T extends Number> extends StableTicksAxis<T> {
    public MetricStableTicksAxis() {
        super(new MetricPrefixFormatter());
    }
}
