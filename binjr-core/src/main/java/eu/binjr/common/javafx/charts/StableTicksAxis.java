/*
 * Copyright 2013 Jason Winnebeck
 * Copyright 2019-2021 Frederic Thevenet
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

package eu.binjr.common.javafx.charts;

import eu.binjr.common.text.PrefixFormatter;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Dimension2D;
import javafx.geometry.Side;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.gillius.jfxutils.chart.AxisTickFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The StableTicksAxis places tick marks at consistent (axis value rather than graphical) locations.
 *
 * @author Jason Winnebeck
 */
public class StableTicksAxis<T extends Number> extends ValueAxis<T> {
    private T dataMaxValue = (T) Double.valueOf(0);
    private T dataMinValue = (T) Double.valueOf(0);
    private final double[] dividers;
    private final int base;

    public static class SelectableRegion extends Pane {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
        private final BooleanProperty selected = new BooleanPropertyBase(false) {
            public void invalidated() {
                pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, get());
            }

            @Override
            public Object getBean() {
                return SelectableRegion.this;
            }

            @Override
            public String getName() {
                return "selected";
            }
        };

        public boolean isSelected() {
            return selected.get();
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

    }

    /**
     * Possible tick spacing at the 10^1 level. These numbers must be &gt;= 1 and &lt; 10.
     */
    private static final int NUM_MINOR_TICKS = 3;
    public static final int BTN_WITDTH = 21;
    private final SelectableRegion selectionMarker = new SelectableRegion();
    private final BooleanProperty selectionMarkerVisible = selectionMarker.visibleProperty();
    private final BooleanProperty selected = selectionMarker.selectedProperty(); //new SimpleBooleanProperty(false);

    private final Timeline animationTimeline = new Timeline();
    private final WritableValue<Double> scaleValue = new WritableValue<>() {
        @Override
        public Double getValue() {
            return getScale();
        }

        @Override
        public void setValue(Double value) {
            setScale(value);
        }
    };

    private AxisTickFormatter axisTickFormatter;
    private final SimpleDoubleProperty tickSpacing = new SimpleDoubleProperty(20);

    private final StringProperty displayUnit = new SimpleStringProperty("");

    private List<T> minorTicks;

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    private final DoubleProperty autoRangePadding = new SimpleDoubleProperty(0.1);

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    private final BooleanProperty forceZeroInRange = new SimpleBooleanProperty(true);

    /**
     * Initializes a new instance of the {@link StableTicksAxis} class.
     *
     * @param prefixFormatter the {@link PrefixFormatter} instance to use.
     * @param base            the numerical base used to determine tick positions.
     * @param dividers        a list of divider candidates.
     */
    public StableTicksAxis(PrefixFormatter prefixFormatter, int base, double[] dividers) {
        super();
        this.base = base;
        this.dividers = dividers != null ? dividers : new double[]{1.0, 2.5, 5.0};
        getStyleClass().setAll("axis");

        selectionMarker.getStyleClass().add("selection-marker");
        var states = selectionMarker.getPseudoClassStates();
        this.getChildren().add(selectionMarker);

        this.axisTickFormatter = new AxisTickFormatter() {
            @Override
            public void setRange(double v, double v1, double v2) {
            }

            @Override
            public String format(Number number) {
                return prefixFormatter.format(number.doubleValue()) + displayUnit.getValue();
            }

        };
    }

    /**
     * Returns the axis tick formatter.
     *
     * @return the axis tick formatter.
     */
    public AxisTickFormatter getAxisTickFormatter() {
        return axisTickFormatter;
    }

    /**
     * Sets  the axis tick formatter.
     *
     * @param axisTickFormatter the axis tick formatter.
     */
    public void setAxisTickFormatter(AxisTickFormatter axisTickFormatter) {
        this.axisTickFormatter = axisTickFormatter;
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     *
     * @return Amount of padding to add on the each end of the axis when auto ranging.
     */
    public double getAutoRangePadding() {
        return autoRangePadding.get();
    }

    /**
     * The autoRangePadding property.
     *
     * @return the autoRangePadding property.
     */
    public DoubleProperty autoRangePaddingProperty() {
        return autoRangePadding;
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     *
     * @param autoRangePadding Amount of padding to add on the each end of the axis when auto ranging.
     */
    public void setAutoRangePadding(double autoRangePadding) {
        this.autoRangePadding.set(autoRangePadding);
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     *
     * @return true if force 0 to be the min or max end of the range when auto-ranging, false otherwize.
     */
    public boolean isForceZeroInRange() {
        return forceZeroInRange.get();
    }

    /**
     * The forceZeroInRange property
     *
     * @return the forceZeroInRange property
     */
    public BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     *
     * @param forceZeroInRange set to true, when auto-ranging, to force 0 to be the min or max end of the range.
     */
    public void setForceZeroInRange(boolean forceZeroInRange) {
        this.forceZeroInRange.set(forceZeroInRange);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        Side side = getSide();
        if (side.isVertical()) {
            double xShift = side == Side.LEFT ? -10 : 0;
            double contentX = this.getLayoutX();
            double contentY = this.getLayoutY();
            double contentWidth = this.getWidth() + 10;//(xShift * -1);
            double contentHeight = this.getHeight();

            // this.selectionMarker.setPrefWidth(contentWidth);
            this.selectionMarker.setPrefHeight(contentHeight);

            selectionMarker.resizeRelocate(
                    snapPositionX(xShift),
                    snapPositionY(0),
                    snapSizeX(contentWidth),
                    snapSizeY(contentHeight));
            selectionMarker.toFront();
        }
    }

    @Override
    public void invalidateRange(List<T> data) {
        // Calculate min and max value not taking NaN values into account
        dataMaxValue = data.stream()
                .filter(d -> d != null && !Double.isNaN(d.doubleValue()))
                .max(Comparator.comparingDouble(T::doubleValue))
                .orElse((T) Double.valueOf(getUpperBound()));
        dataMinValue = data.stream()
                .filter(d -> d != null && !Double.isNaN(d.doubleValue()))
                .min(Comparator.comparingDouble(T::doubleValue))
                .orElse((T) Double.valueOf(getLowerBound()));
        // Invoke super with an empty list so that requestLayout is called while avoiding iterating the sample list again.
        super.invalidateRange(Collections.emptyList());
    }

    @Override
    protected Range autoRange(double minValue, double maxValue, double length, double labelSize) {
        // By fthevenet: Override the provided min and max values by those calculated by our override.
        minValue = dataMinValue.doubleValue();
        maxValue = dataMaxValue.doubleValue();
        //By dweil: if the range is very small, display it like a flat line, the scaling doesn't work very well at these
        //values. 1e-300 was chosen arbitrarily.
        if (Math.abs(minValue - maxValue) < 1e-300) {
            //Normally this is the case for all points with the same value
            minValue = minValue - 1;
            maxValue = maxValue + 1;

        } else {
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
            } else if (minValue > 0 && maxValue > 0) {
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

    public double calculateTickSpacing(double delta, int maxTicks) {
        if (delta == 0.0) {
            return 0.0;
        }
        if (delta <= 0.0) {
            throw new IllegalArgumentException("delta must be positive");
        }
        if (maxTicks < 1) {
            throw new IllegalArgumentException("must be at least one tick");
        }
        int divider = 0;
        int factor = (int) (Math.log(delta) / Math.log(base));
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
        } else {
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


    @Override
    protected List<T> calculateMinorTickMarks() {
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

        } else {
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
    protected List<T> calculateTickValues(double length, Object range) {
        Range rangeVal = (Range) range;
        //Use floor so we start generating ticks before the axis starts -- this is really only relevant
        //because of the minor ticks before the first visible major tick. We'll generate a first
        //invisible major tick but the ValueAxis seems to filter it out.
        double firstTick = Math.floor(rangeVal.low / rangeVal.tickSpacing) * rangeVal.tickSpacing;
        //Generate one more tick than we expect, for "overlap" to get minor ticks on both sides of the
        //first and last major tick.
        int numTicks = (int) (rangeVal.getDelta() / rangeVal.tickSpacing) + 1;
        List<T> ret = new ArrayList<>(numTicks + 1);
        minorTicks = new ArrayList<>((numTicks + 2) * NUM_MINOR_TICKS);
        double minorTickSpacing = rangeVal.tickSpacing / (NUM_MINOR_TICKS + 1);
        for (int i = 0; i <= numTicks; ++i) {
            double majorTick = firstTick + rangeVal.tickSpacing * i;
            ret.add((T) Double.valueOf(majorTick));
            for (int j = 1; j <= NUM_MINOR_TICKS; ++j) {
                minorTicks.add((T) Double.valueOf(majorTick + minorTickSpacing * j));
            }
        }
        return ret;
    }

    @Override
    protected String getTickMarkLabel(T number) {
        return axisTickFormatter.format(number);
    }

    private double getLength() {
        if (getSide().isHorizontal()) {
            return getWidth();
        } else {
            return getHeight();
        }
    }

    private double getLabelSize() {
        Dimension2D dim = measureTickMarkLabelSize("-888.88E-88", getTickLabelRotation());
        if (getSide().isHorizontal()) {
            return dim.getWidth();
        } else {
            return dim.getHeight();
        }
    }

    /**
     * Returns the tick spacing value.
     *
     * @return the tick spacing value.
     */
    public double getTickSpacing() {
        return tickSpacing.get();
    }

    /**
     * The tickSpacing property
     *
     * @return the tickSpacing property
     */
    public SimpleDoubleProperty tickSpacingProperty() {
        return tickSpacing;
    }

    /**
     * Sets the value of the space space in between ticks.
     *
     * @param tickSpacing the value of the space space in between ticks.
     */
    public void setTickSpacing(double tickSpacing) {
        this.tickSpacing.set(tickSpacing);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.setValue(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean getSelectionMarkerVisible() {
        return selectionMarkerVisible.get();
    }

    public BooleanProperty selectionMarkerVisibleProperty() {
        return selectionMarkerVisible;
    }

    public void setSelectionMarkerVisible(boolean selectionMarkerVisible) {
        this.selectionMarkerVisible.set(selectionMarkerVisible);
    }

    public SelectableRegion getSelectionMarker() {
        return selectionMarker;
    }

    public String getDisplayUnit() {
        return displayUnit.get();
    }

    public StringProperty displayUnitProperty() {
        return displayUnit;
    }

    public void setDisplayUnit(String displayUnit) {
        this.displayUnit.set(displayUnit);
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
}
