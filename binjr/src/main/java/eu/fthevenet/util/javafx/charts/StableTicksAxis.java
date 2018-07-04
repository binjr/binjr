/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fthevenet.util.javafx.charts;

import eu.fthevenet.util.text.PrefixFormatter;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.scene.chart.ValueAxis;
import javafx.util.Duration;
import org.gillius.jfxutils.chart.AxisTickFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * The StableTicksAxis places tick marks at consistent (axis value rather than graphical) locations.
 *
 * @author Jason Winnebeck
 */
public abstract class StableTicksAxis extends ValueAxis<Number> {

    /**
     * Possible tick spacing at the 10^1 level. These numbers must be &gt;= 1 and &lt; 10.
     */
    private static final int NUM_MINOR_TICKS = 3;
    public static final int BTN_WITDTH = 21;

    private final Timeline animationTimeline = new Timeline();
    private final WritableValue<Double> scaleValue = new WritableValue<Double>() {
        @Override
        public Double getValue() {
            return getScale();
        }

        @Override
        public void setValue(Double value) {
            setScale(value);
        }
    };
//    private final Button minAxisButton = new Button();
//    private final Button maxAxisButton = new Button();
//    private final ToggleButton centerAxisButton = new ToggleButton();

    private AxisTickFormatter axisTickFormatter;
    private SimpleDoubleProperty tickSpacing = new SimpleDoubleProperty(20);

    private List<Number> minorTicks;

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    private DoubleProperty autoRangePadding = new SimpleDoubleProperty(0.1);

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    private BooleanProperty forceZeroInRange = new SimpleBooleanProperty(true);

    public StableTicksAxis(PrefixFormatter prefixFormatter) {
        super();
        getStyleClass().setAll("axis");

//        minAxisButton.setPrefHeight(10);
//        minAxisButton.setPrefWidth(10);
//        centerAxisButton.setGraphic(new ImageView(this.getClass().getResource("/images/autoScale.png").toString()));
//        maxAxisButton.setPrefHeight(10);
//        maxAxisButton.setPrefWidth(10);
//        getChildren().addAll(maxAxisButton, minAxisButton, centerAxisButton);

        this.axisTickFormatter = new AxisTickFormatter() {
            @Override
            public void setRange(double v, double v1, double v2) {
            }

            @Override
            public String format(Number number) {
                return prefixFormatter.format(number.doubleValue());
            }
        };
    }

    public AxisTickFormatter getAxisTickFormatter() {
        return axisTickFormatter;
    }

    public void setAxisTickFormatter(AxisTickFormatter axisTickFormatter) {
        this.axisTickFormatter = axisTickFormatter;
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public double getAutoRangePadding() {
        return autoRangePadding.get();
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public DoubleProperty autoRangePaddingProperty() {
        return autoRangePadding;
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public void setAutoRangePadding(double autoRangePadding) {
        this.autoRangePadding.set(autoRangePadding);
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public boolean isForceZeroInRange() {
        return forceZeroInRange.get();
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public void setForceZeroInRange(boolean forceZeroInRange) {
        this.forceZeroInRange.set(forceZeroInRange);
    }

    @Override
    protected Range autoRange(double minValue, double maxValue, double length, double labelSize) {
        //By dweil: if the range is very small, display it like a flat line, the scaling doesn't work very well at these
        //values. 1e-300 was chosen arbitrarily.
        if (Math.abs(minValue - maxValue) < 1e-300) {
            //Normally this is the case for all points with the same value
            minValue = minValue - 1;
            maxValue = maxValue + 1;

        }
        else {
            //Add padding
            double delta = maxValue - minValue;
            double paddedMin = minValue - delta * autoRangePadding.get();
            //If we've crossed the 0 line, clamp to 0.
            //noinspection FloatingPointEquality
            if (Math.signum(paddedMin) != Math.signum(minValue)) {
                paddedMin = 0.0;
            }

            double paddedMax = maxValue + delta * autoRangePadding.get();
            //If we've crossed the 0 line, clamp to 0.
            //noinspection FloatingPointEquality
            if (Math.signum(paddedMax) != Math.signum(maxValue)) {
                paddedMax = 0.0;
            }

            minValue = paddedMin;
            maxValue = paddedMax;
        }

        //Handle forcing zero into the range
        if (forceZeroInRange.get()) {
            if (minValue < 0 && maxValue < 0) {
                maxValue = 0;
                minValue -= -minValue * autoRangePadding.get();
            }
            else if (minValue > 0 && maxValue > 0) {
                minValue = 0;
                maxValue += maxValue * autoRangePadding.get();
            }
        }

        Range ret = getRange(minValue, maxValue);
        return ret;
    }

    private Range getRange(double minValue, double maxValue) {
        double length = getLength();
        double delta = maxValue - minValue;
        double scale = calculateNewScale(length, minValue, maxValue);

        int maxTicks = Math.max(1, (int) (length / getTickSpacing()));

        Range ret;
        ret = new Range(minValue, maxValue, calculateTickSpacing(delta, maxTicks), scale);
        return ret;
    }

    public abstract double calculateTickSpacing(double delta, int maxTicks);


    @Override
    protected List<Number> calculateMinorTickMarks() {
        return minorTicks;
    }

    @Override
    protected void setRange(Object range, boolean animate) {
        Range rangeVal = (Range) range;
        if (animate) {
            animationTimeline.stop();
            ObservableList<KeyFrame> keyFrames = animationTimeline.getKeyFrames();
            keyFrames.setAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, getLowerBound()),
                            new KeyValue(scaleValue, getScale())),
                    new KeyFrame(Duration.millis(750),
                            new KeyValue(currentLowerBound, rangeVal.low),
                            new KeyValue(scaleValue, rangeVal.scale)));
            animationTimeline.play();

        }
        else {
            currentLowerBound.set(rangeVal.low);
            setScale(rangeVal.scale);
        }
        setLowerBound(rangeVal.low);
        setUpperBound(rangeVal.high);

        axisTickFormatter.setRange(rangeVal.low, rangeVal.high, rangeVal.tickSpacing);
    }

    @Override
    protected Range getRange() {
        Range ret = getRange(getLowerBound(), getUpperBound());
        return ret;
    }

    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        Range rangeVal = (Range) range;
        //Use floor so we start generating ticks before the axis starts -- this is really only relevant
        //because of the minor ticks before the first visible major tick. We'll generate a first
        //invisible major tick but the ValueAxis seems to filter it out.
        double firstTick = Math.floor(rangeVal.low / rangeVal.tickSpacing) * rangeVal.tickSpacing;
        //Generate one more tick than we expect, for "overlap" to get minor ticks on both sides of the
        //first and last major tick.
        int numTicks = (int) (rangeVal.getDelta() / rangeVal.tickSpacing) + 1;
        List<Number> ret = new ArrayList<Number>(numTicks + 1);
        minorTicks = new ArrayList<Number>((numTicks + 2) * NUM_MINOR_TICKS);
        double minorTickSpacing = rangeVal.tickSpacing / (NUM_MINOR_TICKS + 1);
        for (int i = 0; i <= numTicks; ++i) {
            double majorTick = firstTick + rangeVal.tickSpacing * i;
            ret.add(majorTick);
            for (int j = 1; j <= NUM_MINOR_TICKS; ++j) {
                minorTicks.add(majorTick + minorTickSpacing * j);
            }
        }
        return ret;
    }

    @Override
    protected String getTickMarkLabel(Number number) {
        return axisTickFormatter.format(number);
    }

    private double getLength() {
        if (getSide().isHorizontal()) {
            return getWidth();
        }
        else {
            return getHeight();
        }
    }

    private double getLabelSize() {
        Dimension2D dim = measureTickMarkLabelSize("-888.88E-88", getTickLabelRotation());
        if (getSide().isHorizontal()) {
            return dim.getWidth();
        }
        else {
            return dim.getHeight();
        }
    }

    public double getTickSpacing() {
        return tickSpacing.get();
    }

    public SimpleDoubleProperty tickSpacingProperty() {
        return tickSpacing;
    }

    public void setTickSpacing(double tickSpacing) {
        this.tickSpacing.set(tickSpacing);
    }

    private static class Range {
        public final double low;
        public final double high;
        public final double tickSpacing;
        public final double scale;

        private Range(double low, double high, double tickSpacing, double scale) {
            this.low = low;
            this.high = high;
            this.tickSpacing = tickSpacing;
            this.scale = scale;
        }

        public double getDelta() {
            return high - low;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "low=" + low +
                    ", high=" + high +
                    ", tickSpacing=" + tickSpacing +
                    ", scale=" + scale +
                    '}';
        }
    }
//
//
//    @Override
//    protected double computePrefWidth(double height) {
//        final Side side = getSide();
//        if (side.isVertical()) {
//            return super.computePrefWidth(height) + BTN_WITDTH +2;
//        }
//        else { // HORIZONTAL
//            // TODO for now we have no hard and fast answer here, I guess it should work
//            // TODO out the minimum size needed to display min, max and zero tick mark labels.
//            return 100;
//        }
//    }
//
//    @Override
//    protected double computePrefHeight(double width) {
//        final Side side = getSide();
//        if (side.isVertical()) {
//            // TODO for now we have no hard and fast answer here, I guess it should work
//            // TODO out the minimum size needed to display min, max and zero tick mark labels.
//            return 100;
//        }
//        else { // HORIZONTAL
//
//            return super.computePrefHeight(width) + BTN_WITDTH + 2;
//        }
//    }
//
//    @Override
//    protected void layoutChildren() {
//        super.layoutChildren();
//
//        Side side = this.getSide();
//        final double width = getWidth();
//        final double height = getHeight();
//
//         maxAxisButton.setLayoutX(20);
//        maxAxisButton.setLayoutY(-(BTN_WITDTH/2));
//
//
//
//        maxAxisButton.resize(BTN_WITDTH, BTN_WITDTH); //Math.ceil(maxAxisButton.prefHeight(width)));
//
//        minAxisButton.setLayoutX(20);
//        minAxisButton.setLayoutY(height - (BTN_WITDTH/2));
//        minAxisButton.resize(BTN_WITDTH, BTN_WITDTH);
//
//        centerAxisButton.setLayoutX(20);
//        centerAxisButton.setLayoutY((int)(height/2) -(BTN_WITDTH/2));
//        centerAxisButton.resize(BTN_WITDTH, BTN_WITDTH);
//
//    }
}
