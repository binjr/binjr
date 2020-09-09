/*
 *    Copyright 2017-2019 Frederic Thevenet
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

package eu.binjr.common.text;

import java.text.DecimalFormat;
import java.util.NavigableMap;

/**
 * This class provides the interface and base implementation for formatting double or long into string
 * with a unit prefix.
 *
 * @author Frederic Thevenet
 */
public abstract class PrefixFormatter {
    private static final String DEFAULT_PATTERN = "###,###.##";
    private final NavigableMap<Double, String> suffixMap;
    private final DecimalFormat formatter;

    /**
     * Initializes a new instance of {@link PrefixFormatter} with the arithmetical base and a
     * representation of the prefixes.
     *
     * @param suffixMap a map of the suffix labels and the associated divider as the key.
     */
    protected PrefixFormatter(NavigableMap<Double, String> suffixMap) {
        this(suffixMap, DEFAULT_PATTERN);
    }

    /**
     * Initializes a new instance of {@link PrefixFormatter} with the arithmetical base and a
     * representation of the prefixes.
     *
     * @param suffixMap a map of the suffix labels and the associated divider as the key.
     * @param pattern   a non-localized pattern string.
     */
    protected PrefixFormatter(NavigableMap<Double, String> suffixMap, String pattern) {
        this.suffixMap = suffixMap;
        this.formatter = new DecimalFormat(pattern);
    }

    /**
     * Formats a {@code double} as a string with a unit prefix
     *
     * @param value the value to format
     * @return the formatted string
     */
    public String format(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return "Infinite";
        }
        if (value == Double.MIN_VALUE) {
            return format(Double.MIN_VALUE + 1);
        }
        if (value < 0) {
            return "-" + format(-value);
        }
        var e = suffixMap.floorEntry(value);
        if (e == null) {
            return formatter.format(value);
        }
        Double divideBy = e.getKey();
        String suffix = e.getValue();
        return formatter.format(value / divideBy) + suffix;
    }
}
