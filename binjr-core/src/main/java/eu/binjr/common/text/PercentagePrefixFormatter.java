/*
 * Copyright 2023 Frederic Thevenet
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

package eu.binjr.common.text;

import eu.binjr.common.text.PrefixFormatter;

import java.util.NavigableMap;
import java.util.TreeMap;

public class PercentagePrefixFormatter extends PrefixFormatter {
    public static final int BASE = 10;
    private static final NavigableMap<Double, String> SUFFIX_MAP = new TreeMap<>() {{
        put(0.01, "");
    }};


    public PercentagePrefixFormatter() {
        super(SUFFIX_MAP, 10);
    }

    public PercentagePrefixFormatter(String pattern) {
        super(SUFFIX_MAP, pattern, 10);
    }
}
