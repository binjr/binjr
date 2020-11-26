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

package eu.binjr.core.data.indexes.parser.capture;

import java.util.Objects;

public class CaptureGroup implements NamedCaptureGroup {
    private final String name;

    public static NamedCaptureGroup of(String name) {
        return new CaptureGroup(name);
    }

    private CaptureGroup(String name) {
        Objects.requireNonNull(name);
        this.name = name.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamedCaptureGroup)) {
            return false;
        }
        NamedCaptureGroup other = (NamedCaptureGroup) obj;
        return Objects.equals(name(), other.name());
    }
}
