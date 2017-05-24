/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.data.workspace;

/**
 * An enumeration of the type of charts supported.
 *
 * @author Frederic Thevenet
 */
public enum ChartType {
    STACKED("Stacked area chart"),
    AREA("Area chart"),
    LINE("Line chart");

    private String label;

    ChartType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
