/*
 *    Copyright 2017-2021 Frederic Thevenet
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

package eu.binjr.core.data.workspace;

import eu.binjr.common.text.*;

/**
 * An enumeration of the support unit prefixes
 *
 * @author Frederic Thevenet
 */
public enum UnitPrefixes {
    METRIC("Metric", new MetricPrefixFormatter()),
    BINARY("Binary", new BinaryPrefixFormatter()),
    PERCENTAGE("Percentage",  new PercentagePrefixFormatter()),
    NONE("None", new NoopPrefixFormatter());

    private String label;
    private PrefixFormatter prefixFormatter;

    UnitPrefixes(String label, PrefixFormatter prefixFormatter) {
        this.label = label;
        this.prefixFormatter = prefixFormatter;
    }

    @Override
    public String toString() {
        return label;
    }

    public PrefixFormatter getPrefixFormatter() {
        return prefixFormatter;
    }
}
