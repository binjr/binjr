package eu.fthevenet.binjr.commons.samples;


        import com.sun.javafx.charts.ChartLayoutAnimator;
        import com.sun.javafx.css.converters.SizeConverter;
        import eu.fthevenet.binjr.commons.reflection.ReflectionUtils;
        import javafx.animation.KeyFrame;
        import javafx.animation.KeyValue;
        import javafx.beans.property.*;
        import javafx.beans.value.ChangeListener;
        import javafx.beans.value.ObservableValue;
        import javafx.css.CssMetaData;
        import javafx.css.Styleable;
        import javafx.css.StyleableDoubleProperty;
        import javafx.css.StyleableProperty;
        import javafx.geometry.Dimension2D;
        import javafx.geometry.Side;
        import javafx.scene.chart.ValueAxis;
        import javafx.util.Duration;
        import javafx.util.StringConverter;
        import javafx.util.converter.TimeStringConverter;

        import java.util.*;

/*
* Created with IntelliJ IDEA.
* User: Pedro Duque Vieira
* Date: 15-08-2013
* Time: 18:33
* To change this template use File | Settings | File Templates.
*/
public class TimeAxis extends ValueAxis<Long> {

    /** We use these for auto ranging to pick a user friendly tick unit. (must be increasingly bigger)*/
    private static final double[] TICK_UNIT_DEFAULTS = {
            86400000,       // 1 day
            172800000,      // 2 das
            259200000,      // 3 days
            345600000,      // 4 days
            432000000,      // 5 days
            518400000,      // 6 days
            604800000,      // 7 days
            691200000,      // 8 days
            777600000,      // 9 days
            864000000,      // 10 days
            216000000E1,    // 15 days
            388800000E1,    // 20 days
            604800000E1,    // 25 days
            872640000E1,    // 31 days ~ 1 month
            1226880000E1,   // 41 days
            1667520000E1,   // 51 days
            2203200000E1,   // 62 days ~ 2 months
            2868480000E1,   // 77 days
            3672000000E1,   // 93 days ~ 3 months
            4605120000E1,   // 108 days
            5676480000E1,   // 124 days ~ 4 months
            6877440000E1,   // 139 days
            8216640000E1,   // 155 days ~ 5 months
            9685440000E1,   // 170 days
            1129248000E2,   // 186 days ~ 6 months
            1445472000E2    // 366 days ~ 1 year
    };

    /** These are matching date formatter strings */
    private static final String[] TICK_UNIT_FORMATTER_DEFAULTS = {
            "MM/dd/yy",     // 1 day
            "MM/dd/yy",     // 2 das
            "MM/dd/yy",     // 3 days
            "MM/dd/yy",     // 4 days
            "MM/dd/yy",     // 5 days
            "MM/dd/yy",     // 6 days
            "MM/dd/yy",     // 7 days
            "MM/dd/yy",     // 8 days
            "MM/dd/yy",     // 9 days
            "MM/dd/yy",     // 10 days
            "MM/dd/yy",     // 15 days
            "MM/dd/yy",     // 20 days
            "MM/dd/yy",     // 25 days
            "MMM-yyyy",     // 31 days ~ 1 month
            "MMM-yyyy",     // 41 days
            "MMM-yyyy",     // 51 days
            "MMM-yyyy",     // 62 days ~ 2 months
            "MMM-yyyy",     // 77 days
            "MMM-yyyy",     // 93 days ~ 3 months
            "MMM-yyyy",     // 108 days
            "MMM-yyyy",     // 124 days ~ 4 months
            "MMM-yyyy",     // 139 days
            "MMM-yyyy",     // 155 days ~ 5 months
            "MMM-yyyy",     // 170 days
            "MMM-yyyy",     // 186 days ~ 6 months
            "yyyy"          // 366 days ~ 1 year
    };


    private Object currentAnimationID;
    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);
    private IntegerProperty currentRangeIndexProperty = new SimpleIntegerProperty(this, "currentRangeIndex", -1);
    private DefaultFormatter defaultFormatter = new DefaultFormatter(this);

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    /** When true zero is always included in the visible range. This only has effect if auto-ranging is on. */
    private BooleanProperty forceZeroInRange = new BooleanPropertyBase(true) {
        @Override protected void invalidated() {
            // This will effect layout if we are auto ranging
            if(isAutoRanging()) requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return TimeAxis.this;
        }

        @Override
        public String getName() {
            return "forceZeroInRange";
        }
    };
    public final boolean isForceZeroInRange() { return forceZeroInRange.getValue(); }
    public final void setForceZeroInRange(boolean value) { forceZeroInRange.setValue(value); }
    public final BooleanProperty forceZeroInRangeProperty() { return forceZeroInRange; }

    /**  The value between each major tick mark in data units. This is automatically set if we are auto-ranging. */
    private DoubleProperty tickUnit = new StyleableDoubleProperty(5) {
        @Override protected void invalidated() {
            if(!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public CssMetaData<TimeAxis,Number> getCssMetaData() {
            return StyleableProperties.TICK_UNIT;
        }

        @Override
        public Object getBean() {
            return TimeAxis.this;
        }

        @Override
        public String getName() {
            return "tickUnit";
        }
    };
    public final double getTickUnit() { return tickUnit.get(); }
    public final void setTickUnit(double value) { tickUnit.set(value); }
    public final DoubleProperty tickUnitProperty() { return tickUnit; }

    // -------------- CONSTRUCTORS -------------------------------------------------------------------------------------

    /**
     * Create a auto-ranging TimeAxis
     */
    public TimeAxis() {
        forceZeroInRange.set(false);
    }

    /**
     * Create a non-auto-ranging TimeAxis with the given upper bound, lower bound and tick unit
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public TimeAxis(double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        setTickUnit(tickUnit);
    }

    /**
     * Create a non-auto-ranging TimeAxis with the given upper bound, lower bound and tick unit
     *
     * @param axisLabel The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public TimeAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        setTickUnit(tickUnit);
        setLabel(axisLabel);
    }

    // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    @Override protected String getTickMarkLabel(Long value) {
        StringConverter<Long> formatter = getTickLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        return formatter.toString(value);
    }

    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    @Override protected Object getRange() {
        double[] newParams = recalculateTicks();
        double newMin = newParams[0];
        double newMax = newParams[1];
        double newIndex = newParams[2];
        double newTickUnit = newParams[3];
        return new double[]{
                newMin,
                newMax,
                newTickUnit,
                getScale(),
                newIndex
        };
    }

    private double[] recalculateTicks()
    {
        final Side side = getSide();
        final boolean vertical = Side.LEFT.equals(side) || Side.RIGHT.equals(side);
        final double length = vertical ? getHeight() : getWidth();
        // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
        double labelSize = getTickLabelFont().getSize() * 2;

        double currentRange = getUpperBound() - getLowerBound();

        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int)Math.floor(Math.abs(length)/labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        double tickUnit = currentRange/(double)numOfTickMarks;
        // search for the best tick unit that fits
        double tickUnitRounded = 0;
        double minRounded = 0;
        double maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        int rangeIndex = 10;
        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        while (reqLength > length || count > 20) {
            // find a user friendly match from our default tick units to match calculated tick unit
            for (int i=0; i<TICK_UNIT_DEFAULTS.length; i++) {
                double tickUnitDefault = TICK_UNIT_DEFAULTS[i];
                if (tickUnitDefault > tickUnit) {
                    tickUnitRounded = tickUnitDefault;
                    rangeIndex = i;
                    break;
                }
            }
            // move min and max to nearest tick mark
            minRounded = Math.floor(getLowerBound() / tickUnitRounded) * tickUnitRounded;
            maxRounded = Math.ceil(getUpperBound() / tickUnitRounded) * tickUnitRounded;
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            double last = 0;
            count = 0;
            for (double major = minRounded; major <= maxRounded; major += tickUnitRounded, count ++)  {
                double size = (vertical) ? measureTickMarkSize((long)major, getTickLabelRotation(), rangeIndex).getHeight() :
                        measureTickMarkSize((long)major, getTickLabelRotation(), rangeIndex).getWidth();
                if (major == minRounded) { // first
                    last = size/2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap, last + 6 + (size/2) );
                }
            }
            reqLength = (count-1) * maxReqTickGap;
            tickUnit = tickUnitRounded;
            // check if we already found max tick unit
            if (tickUnitRounded == TICK_UNIT_DEFAULTS[TICK_UNIT_DEFAULTS.length-1]) {
                // nothing we can do so just have to use this
                break;
            }
        }
        return new double[]{minRounded, maxRounded, rangeIndex, tickUnit};
    }

    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this method should
     * animate the range to the new range.
     *
     * @param range A range object returned from autoRange()
     * @param animate If true animate the change in range
     */
    @Override protected void setRange(Object range, boolean animate) {
        final double[] rangeProps = (double[]) range;
        final double lowerBound = rangeProps[0];
        final double upperBound = rangeProps[1];
        final double tickUnit = rangeProps[2];
        final double scale = rangeProps[3];
        final double rangeIndex = rangeProps[4];
        currentRangeIndexProperty.set((int)rangeIndex);
        final double oldLowerBound = getLowerBound();
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
        setTickUnit(tickUnit);

        ReadOnlyDoubleWrapper scalePropertyImplValue = (ReadOnlyDoubleWrapper) ReflectionUtils.forceMethodCall(ValueAxis.class, "scalePropertyImpl", this);

        if(animate) {
            animator.stop(currentAnimationID);
            currentAnimationID = animator.animate(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, oldLowerBound),
                            new KeyValue(scalePropertyImplValue, getScale())
                    ),
                    new KeyFrame(Duration.millis(700),
                            new KeyValue(currentLowerBound, lowerBound),
                            new KeyValue(scalePropertyImplValue, scale)
                    )
            );
        } else {
            currentLowerBound.set(lowerBound);
            setScale(scale);
        }
    }

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override protected List<Long> calculateTickValues(double length, Object range) {
        final double[] rangeProps = (double[]) range;
        final double lowerBound = rangeProps[0];
        final double upperBound = rangeProps[1];
        final double tickUnit = rangeProps[2];
        List<Long> tickValues =  new ArrayList<Long>();
        if (tickUnit <= 0 || lowerBound == upperBound) {
            tickValues.add((long)lowerBound);
        } else if (getTickUnit() > 0) {
            for (double major = lowerBound; major <= upperBound; major += tickUnit)  {
                tickValues.add((long)major);
                if(tickValues.size()>2000) {
                    // This is a ridiculous amount of major tick marks, something has probably gone wrong
                    System.err.println("Warning we tried to create more than 2000 major tick marks on a NumberAxis. " +
                            "Lower Bound=" + lowerBound + ", Upper Bound=" + upperBound + ", Tick Unit=" + tickUnit);
                    break;
                }
            }
        }
        return tickValues;
    }

    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */
    protected List<Long> calculateMinorTickMarks() {
        final List<Long> minorTickMarks = new ArrayList<Long>();
        final double lowerBound = getLowerBound();
        final double upperBound = getUpperBound();
        final double tickUnit = getTickUnit();
        final double minorUnit = tickUnit/getMinorTickCount();
        if (getTickUnit() > 0) {
            for (double major = lowerBound; major < upperBound; major += tickUnit)  {
                for (double minor=major+minorUnit; minor < (major+tickUnit); minor += minorUnit) {
                    minorTickMarks.add((long)minor);
                    if(minorTickMarks.size()>10000) {
                        // This is a ridiculous amount of major tick marks, something has probably gone wrong
                        System.err.println("Warning we tried to create more than 10000 minor tick marks on a NumberAxis. " +
                                "Lower Bound=" + getLowerBound() + ", Upper Bound=" + getUpperBound() + ", Tick Unit=" + tickUnit);
                        break;
                    }
                }
            }
        }
        return minorTickMarks;
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value tick mark value
     * @param range range to use during calculations
     * @return size of tick mark label for given value
     */
    @Override protected Dimension2D measureTickMarkSize(Long value, Object range) {
        final double[] rangeProps = (double[]) range;
        final double rangeIndex = rangeProps[4];
        return measureTickMarkSize(value, getTickLabelRotation(), (int)rangeIndex);
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value     tick mark value
     * @param rotation  The text rotation
     * @param rangeIndex The index of the tick unit range
     * @return size of tick mark label for given value
     */
    private Dimension2D measureTickMarkSize(Long value, double rotation, int rangeIndex) {
        String labelText;
        StringConverter<Long> formatter = getTickLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        if(formatter instanceof DefaultFormatter) {
            labelText = ((DefaultFormatter)formatter).toString(value, rangeIndex);
        } else {
            labelText = formatter.toString(value);
        }
        return measureTickMarkLabelSize(labelText, rotation);
    }

    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param length The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    @Override protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        final Side side = getSide();
        final boolean vertical = Side.LEFT.equals(side) || Side.RIGHT.equals(side);
        // check if we need to force zero into range
        if (isForceZeroInRange()) {
            if (maxValue < 0) {
                maxValue = 0;
            } else if (minValue > 0) {
                minValue = 0;
            }
        }
        final double range = maxValue-minValue;

//        // pad min and max by 2%, checking if the range is zero
        final double paddedRange = (range==0) ? 2 : Math.abs(range)*1.02;

        final double padding = (paddedRange - range) / 2;
        // if min and max are not zero then add padding to them
        double paddedMin = minValue - padding;
        double paddedMax = maxValue + padding;
        // check padding has not pushed min or max over zero line
        if ((paddedMin < 0 && minValue >= 0) || (paddedMin > 0 && minValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMin = 0;
        }
        if ((paddedMax < 0 && maxValue >= 0) || (paddedMax > 0 && maxValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMax = 0;
        }
        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int)Math.floor(Math.abs(length)/labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        double tickUnit = paddedRange/(double)numOfTickMarks;
        // search for the best tick unit that fits
        double tickUnitRounded = 0;
        double minRounded = 0;
        double maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        int rangeIndex = 10;
        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        while (reqLength > length || count > 20) {
            // find a user friendly match from our default tick units to match calculated tick unit
            for (int i=0; i<TICK_UNIT_DEFAULTS.length; i++) {
                double tickUnitDefault = TICK_UNIT_DEFAULTS[i];
                if (tickUnitDefault > tickUnit) {
                    tickUnitRounded = tickUnitDefault;
                    rangeIndex = i;
                    break;
                }
            }
            // move min and max to nearest tick mark
            minRounded = Math.floor(paddedMin / tickUnitRounded) * tickUnitRounded;
            maxRounded = Math.ceil(paddedMax / tickUnitRounded) * tickUnitRounded;
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            double last = 0;
            count = 0;
            for (double major = minRounded; major <= maxRounded; major += tickUnitRounded, count ++)  {
                double size = (vertical) ? measureTickMarkSize((long)major, getTickLabelRotation(), rangeIndex).getHeight() :
                        measureTickMarkSize((long)major, getTickLabelRotation(), rangeIndex).getWidth();
                if (major == minRounded) { // first
                    last = size/2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap, last + 6 + (size/2) );
                }
            }
            reqLength = (count-1) * maxReqTickGap;
            tickUnit = tickUnitRounded;
            // check if we already found max tick unit
            if (tickUnitRounded == TICK_UNIT_DEFAULTS[TICK_UNIT_DEFAULTS.length-1]) {
                // nothing we can do so just have to use this
                break;
            }
        }
        // calculate new scale
        final double newScale = calculateNewScale(length, minRounded, maxRounded);
        // return new range
        return new double[]{minRounded, maxRounded, tickUnitRounded, newScale, rangeIndex};
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    /** @treatAsPrivate implementation detail */
    private static class StyleableProperties {
        private static final CssMetaData<TimeAxis,Number> TICK_UNIT =
                new CssMetaData<TimeAxis,Number>("-fx-tick-unit",
                        SizeConverter.getInstance(), 5.0) {

                    @Override
                    public boolean isSettable(TimeAxis n) {
                        return n.tickUnit == null || !n.tickUnit.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(TimeAxis n) {
                        return (StyleableProperty<Number>)n.tickUnitProperty();
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(ValueAxis.getClassCssMetaData());
            styleables.add(TICK_UNIT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * Default number formatter for TimeAxis, this stays in sync with auto-ranging and formats values appropriately.
     * You can wrap this formatter to add prefixes or suffixes;
     * @since JavaFX 2.0
     */
    public static class DefaultFormatter extends StringConverter<Long> {
        private TimeStringConverter formatter;
        private String prefix = null;
        private String suffix = null;

        private Date tempDate = new Date();

        /** used internally */
        private DefaultFormatter() {
            formatter = new TimeStringConverter("MM/dd/yy");
        }

        /**
         * Construct a DefaultFormatter for the given TimeAxis
         *
         * @param axis The axis to format tick marks for
         */
        public DefaultFormatter(final TimeAxis axis) {
            formatter = getFormatter(axis.isAutoRanging()? axis.currentRangeIndexProperty.get() : -1);
            final ChangeListener axisListener = new ChangeListener() {
                @Override public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    formatter = getFormatter(axis.isAutoRanging()? axis.currentRangeIndexProperty.get() : -1);
                }
            };
            axis.currentRangeIndexProperty.addListener(axisListener);
            axis.autoRangingProperty().addListener(axisListener);
        }

        /**
         * Construct a DefaultFormatter for the given TimeAxis with a prefix and/or suffix.
         *
         * @param axis The axis to format tick marks for
         * @param prefix The prefix to append to the start of formatted number, can be null if not needed
         * @param suffix The suffix to append to the end of formatted number, can be null if not needed
         */
        public DefaultFormatter(TimeAxis axis, String prefix, String suffix) {
            this(axis);
            this.prefix = prefix;
            this.suffix = suffix;
        }

        private static TimeStringConverter getFormatter(int rangeIndex) {
            if (rangeIndex < 0) {
                return new TimeStringConverter("MM/dd/yy");
            } else if(rangeIndex >= TICK_UNIT_FORMATTER_DEFAULTS.length) {
                return new TimeStringConverter(TICK_UNIT_FORMATTER_DEFAULTS[TICK_UNIT_FORMATTER_DEFAULTS.length-1]);
            } else {
                return new TimeStringConverter(TICK_UNIT_FORMATTER_DEFAULTS[rangeIndex]);
            }
        }

        /**
         * Converts the object provided into its string form.
         * Format of the returned string is defined by this converter.
         * @return a string representation of the object passed in.
         * @see StringConverter#toString
         */
        @Override public String toString(Long object) {
            return toString(object, formatter);
        }

        private String toString(Long object, int rangeIndex) {
            return toString(object, getFormatter(rangeIndex));
        }

        private String toString(Long object, TimeStringConverter formatter) {
            tempDate.setTime(object);
            if (prefix != null && suffix != null) {
                return prefix + formatter.toString(tempDate) + suffix;
            } else if (prefix != null) {
                return prefix + formatter.toString(tempDate);
            } else if (suffix != null) {
                return formatter.toString(tempDate) + suffix;
            } else {
                return formatter.toString(tempDate);
            }
        }

        /**
         * Converts the string provided into a Number defined by the this converter.
         * Format of the string and type of the resulting object is defined by this converter.
         * @return a Number representation of the string passed in.
         * @see StringConverter#toString
         */
        @Override public Long fromString(String string) {
            int prefixLength = (prefix == null)? 0: prefix.length();
            int suffixLength = (suffix == null)? 0: suffix.length();
            return formatter.fromString(string.substring(prefixLength, string.length() - suffixLength)).getTime();
        }


    }


    public static void main (String [] args)
    {
        // Date construction test
        GregorianCalendar calendar = new GregorianCalendar(1900, 0, 1); // year, month, day
        Date date = calendar.getTime();
        TimeStringConverter timeConverter = new TimeStringConverter("MM/dd/yyyy");
        System.out.println("This is the date toString = " + timeConverter.toString(date));

        // What is 1 day converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date secondDate = calendar.getTime();
        long firstDateValue = date.getTime();
        long secondDateValue  = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (1 day) - \t" + (secondDateValue - firstDateValue));

        // What is 2 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (2 day) - \t" + (secondDateValue - firstDateValue));

        // What is 3 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (3 day) - \t" + (secondDateValue - firstDateValue));

        // What is 4 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 4);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (4 day) - \t" + (secondDateValue - firstDateValue));

        // What is 5 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 5);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (5 day) - \t" + (secondDateValue - firstDateValue));

        // What is 6 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (6 day) - \t" + (secondDateValue - firstDateValue));

        // What is 7 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (7 day) - \t" + (secondDateValue - firstDateValue));

        // What is 8 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 8);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (8 day) - \t" + (secondDateValue - firstDateValue));

        // What is 9 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 9);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (9 day) - \t" + (secondDateValue - firstDateValue));

        // What is 10 days converted to long
        calendar = new GregorianCalendar(1900, 0, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 10);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (10 day) - \t" + (secondDateValue - firstDateValue));

        // What is 15 days? With a long type
        calendar.add(Calendar.DAY_OF_MONTH, 15);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (15 days) - \t" + (secondDateValue - firstDateValue));

        // What is 20 days? With a long type
        calendar.add(Calendar.DAY_OF_MONTH, 20);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (20 days) - \t" + (secondDateValue - firstDateValue));

        // What is 25 days? With a long type
        calendar.add(Calendar.DAY_OF_MONTH, 25);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (25 days) - \t" + (secondDateValue - firstDateValue));


        // What is 1 mont converted to long
        calendar.add(Calendar.DAY_OF_MONTH, 31);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (31 day) - \t" + (secondDateValue - firstDateValue));

        // What is 41 days
        calendar.add(Calendar.DAY_OF_MONTH, 41);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (41 day) - \t" + (secondDateValue - firstDateValue));

        // What is 51 days
        calendar.add(Calendar.DAY_OF_MONTH, 51);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (51 day) - \t" + (secondDateValue - firstDateValue));

        // What is 62 days ( 2 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 62);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (62 days - 2 months) - \t" + (secondDateValue - firstDateValue));

        // What is 77 days ( 2 monhts and a half
        calendar.add(Calendar.DAY_OF_MONTH, 77);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (77 days - 2.5 month) - \t" + (secondDateValue - firstDateValue));

        // What is 93 days ( 3 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 93);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (93 days - 3 month) - \t" + (secondDateValue - firstDateValue));

        // What is 108  ( 3 monhts and a half
        calendar.add(Calendar.DAY_OF_MONTH, 108);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (108 days - 3.5 month) - \t" + (secondDateValue - firstDateValue));

        // What is 124  ( 4 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 124);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (124 days - 4 month) - \t" + (secondDateValue - firstDateValue));

        // What is 139  ( 4.5 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 139);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (139 days - 4.5 month) - \t" + (secondDateValue - firstDateValue));

        // What is 139  ( 5 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 155);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (155 days - 5 month) - \t" + (secondDateValue - firstDateValue));

        // What is 139  ( 5.5 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 170);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (170 days - 5.5 month) - \t" + (secondDateValue - firstDateValue));

        // What is 139  ( 6 monhts
        calendar.add(Calendar.DAY_OF_MONTH, 186);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (186 days - 6 month) - \t" + (secondDateValue - firstDateValue));

        // What is 366 ( 1 year)
        calendar.add(Calendar.DAY_OF_MONTH, 366);
        secondDate = calendar.getTime();
        secondDateValue = secondDate.getTime();
        System.out.println("This is the difference of value between the first and second date (366 days - 1 year) - \t" + (secondDateValue - firstDateValue));
    }

}





