/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013, Christian Schudt
 * Copyright (c) 2016-2020, Frederic Thevenet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.binjr.common.javafx.charts;


import eu.binjr.common.time.ExtraChronoField;
import eu.binjr.core.controllers.TimelineDisplayMode;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import javafx.beans.property.*;
import javafx.scene.chart.Axis;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An axis that displays date and time values.
 * Tick labels are usually automatically set and calculated depending on the range unless you explicitly {@linkplain #setTickLabelFormatter(StringConverter) set an formatter}.
 * You also have the chance to specify fix lower and upper bounds, otherwise they are calculated by your data.
 * This code is a straight forward adaptation of the original DateTimeAxis by Christian Schudt and Diego Cirujano
 * to use JAVA 8 {@link java.time.ZonedDateTime} instead of {@link java.util.Date}
 *
 * @author Christian Schudt
 * @author Diego Cirujano
 * @author Frederic Thevenet
 */
public final class ZonedDateTimeAxis extends Axis<ZonedDateTime> {
    /**
     * These property are used for animation.
     */
    private final LongProperty currentLowerBound = new SimpleLongProperty(this, "currentLowerBound");

    private final LongProperty currentUpperBound = new SimpleLongProperty(this, "currentUpperBound");

    private final ObjectProperty<StringConverter<ZonedDateTime>> tickLabelFormatter = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return ZonedDateTimeAxis.this;
        }

        @Override
        public String getName() {
            return "tickLabelFormatter";
        }
    };

    private final Property<ZoneId> zoneId;

    private final Property<TimelineDisplayMode> timelineDisplayMode = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            invalidateRange();
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return ZonedDateTimeAxis.this;
        }

        @Override
        public String getName() {
            return "timelineDisplayMode";
        }
    };


    /**
     * Stores the min and max date of the list of dates which is used.
     * If {@link #Axis:autoRanging} is true, these values are used as lower and upper bounds.
     */
    private ZonedDateTime minDate, maxDate;

    private final ObjectProperty<ZonedDateTime> lowerBound = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return ZonedDateTimeAxis.this;
        }

        @Override
        public String getName() {
            return "lowerBound";
        }
    };

    private final ObjectProperty<ZonedDateTime> upperBound = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return ZonedDateTimeAxis.this;
        }

        @Override
        public String getName() {
            return "upperBound";
        }
    };

    private Interval actualInterval = Interval.DECADE;

    /**
     * Default constructor. By default the lower and upper bound are calculated by the data.
     */
    public ZonedDateTimeAxis() {
        this.zoneId = new SimpleObjectProperty<>(ZoneId.systemDefault());
    }

    public ZonedDateTimeAxis(ZoneId zoneId) {
        this.zoneId = new SimpleObjectProperty<>(zoneId);
    }

    /**
     * Constructs a date axis with fix lower and upper bounds.
     *
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     */
    public ZonedDateTimeAxis(ZonedDateTime lowerBound, ZonedDateTime upperBound) {
        this(lowerBound.getZone());
        setAutoRanging(false);
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
    }

    /**
     * Constructs a date axis with a label and fix lower and upper bounds.
     *
     * @param axisLabel  The label for the axis.
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     */
    public ZonedDateTimeAxis(String axisLabel, ZonedDateTime lowerBound, ZonedDateTime upperBound) {
        this(lowerBound, upperBound);
        setLabel(axisLabel);
    }

    @Override
    public void invalidateRange(List<ZonedDateTime> list) {
        super.invalidateRange(list);
        Collections.sort(list);
        if (list.isEmpty()) {
            minDate = maxDate = ZonedDateTime.now(zoneId.getValue());
        } else if (list.size() == 1) {
            minDate = maxDate = list.getFirst();
        } else {
            minDate = list.getFirst();
            maxDate = list.getLast();
        }
    }

    @Override
    protected Object autoRange(double length) {
        if (isAutoRanging()) {
            return new Object[]{minDate, maxDate};
        } else {
            if (getLowerBound() == null || getUpperBound() == null) {
                throw new IllegalArgumentException("If autoRanging is false, a lower and upper bound must be set.");
            }
            return getRange();
        }
    }

    @Override
    protected void setRange(Object range, boolean animating) {
        Object[] r = (Object[]) range;
        ZonedDateTime oldLowerBound = getLowerBound();
        ZonedDateTime oldUpperBound = getUpperBound();
        ZonedDateTime lower = (ZonedDateTime) r[0];
        ZonedDateTime upper = (ZonedDateTime) r[1];
        setLowerBound(lower);
        setUpperBound(upper);
        currentLowerBound.set(getLowerBound().toInstant().toEpochMilli());
        currentUpperBound.set(getUpperBound().toInstant().toEpochMilli());
    }

    @Override
    protected Object getRange() {
        return new Object[]{getLowerBound(), getUpperBound()};
    }

    @Override
    public double getZeroPosition() {
        return 0;
    }

    @Override
    public double getDisplayPosition(ZonedDateTime date) {
        final double length = getSide().isHorizontal() ? getWidth() : getHeight();

        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        // Get the actual range of the visible area.
        // The minimal date should start at the zero position, that's why we subtract it.
        double range = length - getZeroPosition();

        // Then get the difference from the actual date to the min date and divide it by the total difference.
        // We get a value between 0 and 1, if the date is within the min and max date.
        double d = (date.toInstant().toEpochMilli() - currentLowerBound.get()) / diff;

        // Multiply this percent value with the range and add the zero offset.
        if (getSide().isVertical()) {
            return getHeight() - d * range + getZeroPosition();
        } else {
            return d * range + getZeroPosition();
        }
    }

    @Override
    public ZonedDateTime getValueForDisplay(double displayPosition) {
        final double length = getSide().isHorizontal() ? getWidth() : getHeight();

        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        // Get the actual range of the visible area.
        // The minimal date should start at the zero position, that's why we subtract it.
        double range = length - getZeroPosition();

        if (getSide().isVertical()) {
            long v = Math.round((displayPosition - getZeroPosition() - getHeight()) / -range * diff + currentLowerBound.get());
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), zoneId.getValue());
        } else {
            long v = Math.round((displayPosition - getZeroPosition()) / range * diff + currentLowerBound.get());
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), zoneId.getValue());
        }
    }

    @Override
    public boolean isValueOnAxis(ZonedDateTime date) {
        return date.toInstant().toEpochMilli() > currentLowerBound.get() && date.toInstant().toEpochMilli() < currentUpperBound.get();
    }

    @Override
    public double toNumericValue(ZonedDateTime date) {
        return date.toInstant().toEpochMilli();
    }

    @Override
    public ZonedDateTime toRealValue(double v) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Math.round(v)), zoneId.getValue());
    }

    @Override
    protected List<ZonedDateTime> calculateTickValues(double v, Object range) {
        Object[] r = (Object[]) range;
        ZonedDateTime lower = (ZonedDateTime) r[0];
        ZonedDateTime upper = (ZonedDateTime) r[1];
        List<ZonedDateTime> dateList = new ArrayList<>();
        // The preferred gap which should be between two tick marks.
        double averageTickGap = 100;
        double averageTicks = v / averageTickGap;

        // Starting with the greatest unit, add one of each calendar unit.
        int i = 0;
        while (i < Interval.values().length && dateList.size() <= averageTicks) {
            Interval interval = Interval.values()[i];
            ZonedDateTime currentLower = ZonedDateTime.from(lower);
            ZonedDateTime currentUpper = ZonedDateTime.from(upper);
            dateList.clear();
            actualInterval = interval;
            // Loop as long we exceeded the upper bound.
            while (currentLower.toInstant().toEpochMilli() <= currentUpper.toInstant().toEpochMilli()) {
                dateList.add(currentLower);
                currentLower = currentLower.plus(interval.amount, interval.unit);
            }
            i++;
        }
        dateList.add(upper);

        List<ZonedDateTime> evenDateList = makeDatesEven(dateList);
        // If there are at least three dates, check if the gap between the lower date and the second date is at least half the gap of the second and third date.
        // Do the same for the upper bound.
        // If gaps between dates are too small, remove one of them.
        // This can occur, e.g. if the lower bound is 25.12.2013 and years are shown. Then the next year shown would be 2014 (01.01.2014) which would be too narrow to 25.12.2013.
        if (evenDateList.size() > 2) {

            ZonedDateTime secondDate = evenDateList.get(1);
            ZonedDateTime thirdDate = evenDateList.get(2);
            ZonedDateTime lastDate = evenDateList.get(dateList.size() - 2);
            ZonedDateTime previousLastDate = evenDateList.get(dateList.size() - 3);

            // If the second date is too near by the lower bound, remove it.
//            if (secondDate.toInstant().toEpochMilli() - lower.toInstant().toEpochMilli() < (thirdDate.toInstant().toEpochMilli() - secondDate.toInstant().toEpochMilli()) / 2) {
//                evenDateList.remove(secondDate);
//            }

            // If difference from the upper bound to the last date is less than the half of the difference of the previous two dates,
            // we better remove the last date, as it comes to close to the upper bound.
            if (upper.toInstant().toEpochMilli() - lastDate.toInstant().toEpochMilli() <
                    (lastDate.toInstant().toEpochMilli() - previousLastDate.toInstant().toEpochMilli()) / 2) {
                evenDateList.remove(lastDate);
            }
        }

        return evenDateList;
    }

    @Override
    protected void layoutChildren() {
        if (!isAutoRanging()) {
            currentLowerBound.set(getLowerBound().toInstant().toEpochMilli());
            currentUpperBound.set(getUpperBound().toInstant().toEpochMilli());
        }
        super.layoutChildren();
    }

    @Override
    protected String getTickMarkLabel(ZonedDateTime date) {
        StringConverter<ZonedDateTime> converter = getTickLabelFormatter();
        if (converter != null) {
            return converter.toString(date);
        }
        DateTimeFormatter formatter;
        if (timelineDisplayMode.getValue() == TimelineDisplayMode.DURATION)
            formatter = switch (actualInterval) {
                case DECADE, YEAR, MONTH_6, MONTH_3 -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.YEAR_OF_ERA)
                        .toFormatter();
                case MONTH_1 -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.MONTH_OF_YEAR)
                        .appendLiteral("mon")
                        .toFormatter();
                case WEEK -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR)
                        .appendLiteral("weeks")
                        .toFormatter();
                case DAY -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.DAY_OF_YEAR)
                        .appendLiteral('d')
                        .toFormatter();
                case HOUR_1, HOUR_3, HOUR_6, HOUR_12 -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.HOUR_OF_DAY)
                        .appendLiteral('h')
                        .toFormatter();
                case MINUTE_1, MINUTE_5, MINUTE_10, MINUTE_30 -> new DateTimeFormatterBuilder()
                        .appendValue(ChronoField.MINUTE_OF_DAY)
                        .appendLiteral("min")
                        .toFormatter();
                case MILLISECOND_500, MILLISECOND_100, SECOND_15, SECOND_5, SECOND_1 -> new DateTimeFormatterBuilder()
                        .appendValue(ExtraChronoField.SECONDS_OF_YEAR)
                        .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                        .appendLiteral('s')
                        .toFormatter();
                case MILLISECOND_10, MILLISECOND_1 -> DateTimeFormatter.ofPattern("A'ms'");
            };
        else {
            formatter = switch (actualInterval.unit) {
                case YEARS -> DateTimeFormatter.ofPattern("yyyy");
                case MONTHS -> DateTimeFormatter.ofPattern("MMM yyyy");
                case HOURS, MINUTES -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
                case SECONDS -> DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                case MILLIS -> DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                default -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            };
        }
        return formatter.withZone(zoneId.getValue()).format(date);
    }

    /**
     * Makes dates even, in the sense of that years always begin in January, months always begin on the 1st and days always at midnight.
     *
     * @param dates The list of dates.
     * @return The new list of dates.
     */
    private List<ZonedDateTime> makeDatesEven(List<ZonedDateTime> dates) {
        // If the dates contain more dates than just the lower and upper bounds, make the dates in between even.
        if (dates.size() > 2) {
            List<ZonedDateTime> evenDates = new ArrayList<>();

            // For each unit, modify the date slightly by a few millis, to make sure they are different days.
            // This is because Axis stores each value and won't update the tick labels, if the value is already known.
            // This happens if you display days and then add a date many years in the future the tick label will still be displayed as day.
            for (int i = 0; i < dates.size(); i++) {
                ZonedDateTime date = dates.get(i);
                boolean isFirstOrLast = i != 0 && i != dates.size() - 1;
                ZonedDateTime normalizedDate = switch (actualInterval.unit) {
                    case YEARS -> ZonedDateTime.of(
                            date.getYear(),
                            isFirstOrLast ? 1 : date.getMonthValue(),
                            isFirstOrLast ? 1 : date.getDayOfMonth(),
                            0, 0, 0, 6, dates.get(i).getZone());
                    case MONTHS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            isFirstOrLast ? 1 : date.getDayOfMonth(),
                            0, 0, 0, 5, dates.get(i).getZone());
                    case WEEKS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            0, 0, 0, 4, dates.get(i).getZone());
                    case DAYS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            0, 0, 0, 3, dates.get(i).getZone());
                    case HOURS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            date.getHour(),
                            isFirstOrLast ? 0 : date.getMinute(),
                            isFirstOrLast ? 0 : date.getSecond(),
                            2, dates.get(i).getZone());
                    case MINUTES -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            date.getHour(),
                            date.getMinute(),
                            isFirstOrLast ? 0 : date.getSecond(),
                            1, dates.get(i).getZone());
                    case SECONDS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            date.getHour(),
                            date.getMinute(),
                            date.getSecond(),
                            1,
                            dates.get(i).getZone());
                    case MILLIS -> ZonedDateTime.of(
                            date.getYear(),
                            date.getMonthValue(),
                            date.getDayOfMonth(),
                            date.getHour(),
                            date.getMinute(),
                            date.getSecond(),
                            (date.getNano() / 1000) * 1000,
                            dates.get(i).getZone());
                    default -> date;
                };
                evenDates.add(normalizedDate);
            }
            return evenDates;
        } else {
            return dates;
        }
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The property.
     * @see #getLowerBound()
     */
    public ObjectProperty<ZonedDateTime> lowerBoundProperty() {
        return lowerBound;
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The lower bound.
     * @see #lowerBoundProperty()
     */
    public ZonedDateTime getLowerBound() {
        return lowerBound.get();
    }

    /**
     * Sets the lower bound of the axis.
     *
     * @param date The lower bound date.
     * @see #lowerBoundProperty()
     */
    public void setLowerBound(ZonedDateTime date) {
        lowerBound.set(date);
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The property.
     * @see #getUpperBound() ()
     */
    public ObjectProperty<ZonedDateTime> upperBoundProperty() {
        return upperBound;
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The upper bound.
     * @see #upperBoundProperty()
     */
    public ZonedDateTime getUpperBound() {
        return upperBound.get();
    }

    /**
     * Sets the upper bound of the axis.
     *
     * @param date The upper bound date.
     * @see #upperBoundProperty() ()
     */
    public void setUpperBound(ZonedDateTime date) {
        upperBound.set(date);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The converter.
     */
    public StringConverter<ZonedDateTime> getTickLabelFormatter() {
        return tickLabelFormatter.getValue();
    }

    /**
     * Sets the tick label formatter for the ticks.
     *
     * @param value The converter.
     */
    public void setTickLabelFormatter(StringConverter<ZonedDateTime> value) {
        tickLabelFormatter.setValue(value);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The property.
     */
    public ObjectProperty<StringConverter<ZonedDateTime>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    public ZoneId getZoneId() {
        return zoneId.getValue();
    }

    public Property<ZoneId> zoneIdProperty() {
        return zoneId;
    }

    public TimelineDisplayMode getTimelineDisplayMode() {
        return timelineDisplayMode.getValue();
    }

    public Property<TimelineDisplayMode> timelineDisplayModeProperty() {
        return timelineDisplayMode;
    }

    public void setTimelineDisplayMode(TimelineDisplayMode timelineDisplayMode) {
        this.timelineDisplayMode.setValue(timelineDisplayMode);
    }


    /**
     * The intervals, which are used for the tick labels. Beginning with the largest unit, the axis tries to calculate the tick values for this unit.
     * If a smaller unit is better suited for, that one is taken.
     */
    private enum Interval {
        DECADE(ChronoUnit.YEARS, 10),
        YEAR(ChronoUnit.YEARS, 1),
        MONTH_6(ChronoUnit.MONTHS, 6),
        MONTH_3(ChronoUnit.MONTHS, 3),
        MONTH_1(ChronoUnit.MONTHS, 1),
        WEEK(ChronoUnit.WEEKS, 1),
        DAY(ChronoUnit.DAYS, 1),
        HOUR_12(ChronoUnit.HOURS, 12),
        HOUR_6(ChronoUnit.HOURS, 6),
        HOUR_3(ChronoUnit.HOURS, 3),
        HOUR_1(ChronoUnit.HOURS, 1),
        MINUTE_30(ChronoUnit.MINUTES, 30),
        MINUTE_10(ChronoUnit.MINUTES, 10),
        MINUTE_5(ChronoUnit.MINUTES, 5),
        MINUTE_1(ChronoUnit.MINUTES, 1),
        SECOND_15(ChronoUnit.SECONDS, 15),
        SECOND_5(ChronoUnit.SECONDS, 5),
        SECOND_1(ChronoUnit.SECONDS, 1),
        MILLISECOND_500(ChronoUnit.MILLIS, 500),
        MILLISECOND_100(ChronoUnit.MILLIS, 100),
        MILLISECOND_10(ChronoUnit.MILLIS, 10),
        MILLISECOND_1(ChronoUnit.MILLIS, 1);

        private final int amount;

        private final transient ChronoUnit unit;

        Interval(ChronoUnit interval, int amount) {
            this.unit = interval;
            this.amount = amount;
        }
    }
}