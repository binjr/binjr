/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.fthevenet.util.javafx.charts;

import eu.fthevenet.util.text.MetricPrefixFormatter;

/**
 * An implementation of {@link StableTicksAxis} that divide up large numbers by powers of 10 and apply metric unit prefixes
 *
 * @author Frederic Thevenet
 */
public class MetricStableTicksAxis extends StableTicksAxis {

    public MetricStableTicksAxis() {
        super(new MetricPrefixFormatter());
    }

    @Override
    public double calculateTickSpacing(double delta, int maxTicks) {
        final double[] dividers = new double[]{1.0, 2.5, 5.0};
        if (delta == 0.0) {
            return 0.0;
        }
        if (delta <= 0.0) {
            throw new IllegalArgumentException("delta must be positive");
        }
        if (maxTicks < 1) {
            throw new IllegalArgumentException("must be at least one tick");
        }

        //The factor will be close to the log10, this just optimizes the search
        int factor = (int) Math.log10(delta);
        int divider = 0;
        int base = 10;
        double numTicks = delta / (dividers[divider] * Math.pow(base, factor));

        //We don't have enough ticks, so increase ticks until we're over the limit, then back off once.
        if (numTicks < maxTicks) {
            while (numTicks < maxTicks) {
                //Move up
                --divider;
                if (divider < 0) {
                    --factor;
                    divider = dividers.length - 1;
                }

                numTicks = delta / (dividers[divider] * Math.pow(base, factor));
            }

            //Now back off once unless we hit exactly
            //noinspection FloatingPointEquality
            if (numTicks != maxTicks) {
                ++divider;
                if (divider >= dividers.length) {
                    ++factor;
                    divider = 0;
                }
            }
        }
        else {
            //We have too many ticks or exactly max, so decrease until we're just under (or at) the limit.
            while (numTicks > maxTicks) {
                ++divider;
                if (divider >= dividers.length) {
                    ++factor;
                    divider = 0;
                }

                numTicks = delta / (dividers[divider] * Math.pow(base, factor));
            }
        }
        return dividers[divider] * Math.pow(base, factor);
    }
}
