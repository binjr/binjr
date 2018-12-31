/*
 *    Copyright 2017-2018 Frederic Thevenet
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
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * This class provides the interface and base implementation for formatting double or long into string
 * with a unit prefix.
 *
 * @author Frederic Thevenet
 */
public abstract class PrefixFormatter {
    public static final String PATTERN = "###,###.##";
    protected final int base;
    private final NavigableMap<Long, String> longSuffixes = new TreeMap<>();
    private final NavigableMap<Double, String> doubleSuffixes = new TreeMap<>();

    /**
     * Initializes a new instance of {@link PrefixFormatter} with the arithmetical base and a representation of the prefixes.
     *
     * @param base     the arithmetical base for the unit
     * @param suffixes a representation of the unit prefixes
     */
    public PrefixFormatter(int base, String[] suffixes) {
        this.base = base;
        for (int i = 0; i < suffixes.length; i++) {
            this.longSuffixes.put(pow(base, i + 1), suffixes[i]);
            this.doubleSuffixes.put(Math.pow(base, i + 1.0), suffixes[i]);
        }
    }

    private long pow(long a, int b) {
        if (b == 0) {
            return 1;
        }
        if (b == 1) {
            return a;
        }
        if (b % 2 == 0) {
            return pow(a * a, b / 2);
        } else {
            return a * pow(a * a, b / 2);
        }
    }

    /**
     * Formats a {@code long} as a string with a unit prefix
     *
     * @param value the value to format
     * @return the formatted string
     */
    public String format(long value) {
        if (value == Long.MIN_VALUE) {
            return format(Long.MIN_VALUE + 1);
        }
        if (value < 0) {
            return "-" + format(-value);
        }
        if (value < base) {
            return Long.toString(value);
        }

        Map.Entry<Long, String> e = longSuffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    /**
     * Formats a {@code double} as a string with a unit prefix
     *
     * @param value the value to format
     * @return the formatted string
     */
    public String format(double value) {
        DecimalFormat formatter = new DecimalFormat(PATTERN);
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return "Infinite";
        }

        if (value == Long.MIN_VALUE) {
            return format(Long.MIN_VALUE + 1);
        }
        if (value < 0) {
            return "-" + format(-value);
        }
        if (value < base) {
            return formatter.format(value);
        }

        Map.Entry<Double, String> e = doubleSuffixes.floorEntry(value);
        Double divideBy = e.getKey();
        String suffix = e.getValue();
        double truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        truncated = hasDecimal ? (truncated / 10) : (truncated / 10d);
        return formatter.format(truncated) + suffix;
    }
}
