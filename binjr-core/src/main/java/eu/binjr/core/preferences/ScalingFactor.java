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
public enum ScalingFactor {
    AUTO("Auto-detect", -1.0),
    TO_50_PERCENT("50%", 0.5),
    TO_75_PERCENT("75%", 0.75),
    TO_100_PERCENT("100%", 1.0),
    TO_125_PERCENT("125%", 1.25),
    TO_150_PERCENT("150%", 1.5),
    TO_175_PERCENT("175%", 1.75),
    TO_200_PERCENT("200%", 2.0),
    TO_225_PERCENT("225%", 2.25),
    TO_250_PERCENT("250%", 2.5),
    TO_275_PERCENT("275%", 2.75),
    TO_300_PERCENT("300%", 3.0),
    TO_325_PERCENT("325%", 3.25),
    TO_350_PERCENT("350%", 3.5);
    private final String label;
    private final double scaleFactor;

    ScalingFactor(String label, double scaleFactor) {
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
