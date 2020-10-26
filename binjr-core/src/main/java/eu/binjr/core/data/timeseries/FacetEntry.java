/*
 *    Copyright 2020 Frederic Thevenet
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

/*
 *    Copyright 2020 Frederic Thevenet
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

/*
 *    Copyright 2020 Frederic Thevenet
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

public class FacetEntry {
    private final String facetName;
    private final String label;
    private final int nbOccurrences;


    public FacetEntry(String facetName, String label, int nbOccurrences) {
        Objects.requireNonNull(facetName, "Facet name cannot be null");
        Objects.requireNonNull(label, "Facet name cannot be null");
        this.facetName = facetName;
        this.label = label;
        this.nbOccurrences = nbOccurrences;
    }

    public int getNbOccurrences() {
        return nbOccurrences;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", label, nbOccurrences);
    }

    public String getFacetName() {
        return facetName;
    }

    @Override
    public int hashCode() {
        return facetName.hashCode() + label.hashCode() + Integer.hashCode(nbOccurrences);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var other = (FacetEntry) obj;
        return Objects.equals(this.facetName, other.facetName) &&
                Objects.equals(this.label, other.label) &&
                Objects.equals(this.nbOccurrences, other.nbOccurrences);
    }
}
