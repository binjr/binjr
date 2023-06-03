/*
 *    Copyright 2020-2021 Frederic Thevenet
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

package eu.binjr.core.data.timeseries;

import java.util.Objects;

public record FacetEntry(String name, String label, int occurrences) {
    public FacetEntry {
        Objects.requireNonNull(name, "Facet name cannot be null");
        Objects.requireNonNull(label, "Facet name cannot be null");
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", label, occurrences);
    }
}
