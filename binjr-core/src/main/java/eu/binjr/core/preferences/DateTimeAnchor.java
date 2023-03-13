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

package eu.binjr.core.preferences;

public enum DateTimeAnchor {
    EPOCH("1970-01-01 00:00:00"),
    TODAY("Current date (midnight)"),
    NOW("Current date and time");

    private final String label;

    DateTimeAnchor(String label){
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
