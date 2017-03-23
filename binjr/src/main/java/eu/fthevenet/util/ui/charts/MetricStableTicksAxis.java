package eu.fthevenet.util.ui.charts;

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
