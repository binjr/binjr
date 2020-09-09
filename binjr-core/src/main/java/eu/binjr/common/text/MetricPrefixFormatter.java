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

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * An implementation of {@link PrefixFormatter} for metric prefixes
 *
 * @author Frederic Thevenet
 */
public class MetricPrefixFormatter extends PrefixFormatter {
    private static final NavigableMap<Double, String> SUFFIX_MAP = new TreeMap<>() {{
        put(Math.pow(BASE, -6.0), "a");
        put(Math.pow(BASE, -5.0), "f");
        put(Math.pow(BASE, -4.0), "p");
        put(Math.pow(BASE, -3.0), "n");
        put(Math.pow(BASE, -2.0), "µ");
        put(Math.pow(BASE, -1.0), "m");
        put(Math.pow(BASE, 0.0), "");
        put(Math.pow(BASE, 1.0), "k");
        put(Math.pow(BASE, 2.0), "M");
        put(Math.pow(BASE, 3.0), "G");
        put(Math.pow(BASE, 4.0), "T");
        put(Math.pow(BASE, 5.0), "P");
        put(Math.pow(BASE, 6.0), "E");
    }};

    public static final int BASE = 1000;

    /**
     * Initializes a new instance of the {@link MetricPrefixFormatter} class
     */
    public MetricPrefixFormatter() {
        super(SUFFIX_MAP);
    }

    /**
     * Initializes a new instance of the {@link MetricPrefixFormatter} class with the specified format pattern.
     *
     * @param pattern a non-localized pattern string
     */
    public MetricPrefixFormatter(String pattern) {
        super(SUFFIX_MAP, pattern);
    }
}