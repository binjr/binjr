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

import java.util.TreeMap;

/**
 * An implementation of {@link PrefixFormatter} for binary prefixes
 *
 * @author Frederic Thevenet
 */
public class BinaryPrefixFormatter extends PrefixFormatter {

    public static final int BASE = 1024;
    public static final TreeMap<Double, String> SUFFIX_MAP = new TreeMap<>() {{
        put(Math.pow(BASE, -4.0), "*2⁻¹⁰⁰⁰⁰⁰⁰⁰⁰⁰");
        put(Math.pow(BASE, -3.0), "*2⁻¹⁰⁰⁰⁰⁰⁰");
        put(Math.pow(BASE, -2.0), "*2⁻¹⁰⁰⁰");
        put(Math.pow(BASE, -1.0), "*2⁻¹⁰");
        put(Math.pow(BASE, 0.0), "");
        put(Math.pow(BASE, 1.0), "ki");
        put(Math.pow(BASE, 2.0), "Mi");
        put(Math.pow(BASE, 3.0), "Gi");
        put(Math.pow(BASE, 4.0), "Ti");
        put(Math.pow(BASE, 5.0), "Pi");
        put(Math.pow(BASE, 6.0), "Ei");
    }};

    /**
     * Initializes a new instance of the {@link BinaryPrefixFormatter} class
     */
    public BinaryPrefixFormatter() {
        super(SUFFIX_MAP, 2);
    }

    /**
     * Initializes a new instance of the {@link BinaryPrefixFormatter} class with the specified format pattern.
     *
     * @param pattern a non-localized pattern string
     */
    public BinaryPrefixFormatter(String pattern) {
        super(SUFFIX_MAP, pattern, 2);
    }
}
