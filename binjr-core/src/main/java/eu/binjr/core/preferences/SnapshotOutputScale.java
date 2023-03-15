/*
 *    Copyright 2019-2023 Frederic Thevenet
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

package eu.binjr.core.preferences;

/**
 * An enumeration of commonly-used snapshot output scales.
 */
public enum SnapshotOutputScale {
    AUTO("Auto (Same as screen)", 0.0),
    ONE("100%", 1.0),
    ONE_TWENTY_FIVE("125%", 1.25),
    ONE_FIFTY("150%", 1.5),
    ONE_SEVENTY_FIVE("175%", 1.75),
    TWO("200%", 2.0);

    private final String label;
    private final double scaleFactor;

    SnapshotOutputScale(String label, double scaleFactor) {
        this.label = label;
        this.scaleFactor = scaleFactor;
    }

    /**
     * Return the label
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Return the scale factor
     *
     * @return the scale factor
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
