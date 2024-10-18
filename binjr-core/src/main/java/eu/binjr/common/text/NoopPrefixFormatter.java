/*
 *    Copyright 2021 Frederic Thevenet
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
public class NoopPrefixFormatter extends PrefixFormatter {
    private static final NavigableMap<Double, String> SUFFIX_MAP = new TreeMap<>() ;

    /**
     * Initializes a new instance of the {@link NoopPrefixFormatter} class
     */
    public NoopPrefixFormatter() {
        super(SUFFIX_MAP, 10);
    }

    /**
     * Initializes a new instance of the {@link NoopPrefixFormatter} class with the specified format pattern.
     *
     * @param pattern a non-localized pattern string
     */
    public NoopPrefixFormatter(String pattern) {
        super(SUFFIX_MAP, pattern, 10);
    }
}