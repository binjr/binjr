/*
 *    Copyright 2025 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser.profile;

public enum ParsingFailureMode {
    CONCAT("Concatenate with previous event"),
    IGNORE("Ignore and move to next line"),
    ABORT("Abort processing");

    private final String description;

    ParsingFailureMode(String description) {
        this.description = description;

    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
