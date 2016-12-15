package eu.fthevenet.binjr.commons.charts;
/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013, Christian Schudt
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

import com.sun.javafx.charts.ChartLayoutAnimator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.chart.Axis;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An axis that displays date and time values.
 * <p/>
 * Tick labels are usually automatically set and calculated depending on the range unless you explicitly {@linkplain #setTickLabelFormatter(StringConverter) set an formatter}.
 * <p/>
 * You also have the chance to specify fix lower and upper bounds, otherwise they are calculated by your data.
 * <p/>
 * <p/>
 * <h3>Screenshots</h3>
 * <p>
 * Displaying date values, ranging over several months:</p>
 * <img src="doc-files/DateAxisMonths.png" alt="DateAxisMonths" />
 * <p>
 * Displaying date values, ranging only over a few hours:</p>
 * <img src="doc-files/DateAxisHours.png" alt="DateAxisHours" />
 * <p/>
 * <p/>
 * <h3>Sample Usage</h3>
 * <pre>
 * {@code
 * ObservableList<XYChart.Series<Date, Number>> series = FXCollections.observableArrayList();
 *
 * ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
 * series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2012, 11, 15).getTime(), 2));
 * series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 5, 3).getTime(), 4));
 *
 * ObservableList<XYChart.Data<Date, Number>> series2Data = FXCollections.observableArrayList();
 * series2Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 0, 13).getTime(), 8));
 * series2Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 7, 27).getTime(), 4));
 *
 * series.add(new XYChart.Series<>("Series1", series1Data));
 * series.add(new XYChart.Series<>("Series2", series2Data));
 *
 * NumberAxis numberAxis = new NumberAxis();
 * TimeAxis dateAxis = new TimeAxis();
 * LineChart<Date, Number> lineChart = new LineChart<>(dateAxis, numberAxis, series);
 * }
 * </pre>
 *
 * @author Christian Schudt
 * @author Diego Cirujano
 */
public final class ZonedDateTimeAxis extends Axis<ZonedDateTime> {

    /**
     * These property are used for animation.
     */
    private final LongProperty currentLowerBound = new SimpleLongProperty(this, "currentLowerBound");

    private final LongProperty currentUpperBound = new SimpleLongProperty(this, "currentUpperBound");

    private final ObjectProperty<StringConverter<ZonedDateTime>> tickLabelFormatter = new ObjectPropertyBase<StringConverter<ZonedDateTime>>() {
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
    private final ZoneId zoneId;

    /**
     * Stores the min and max date of the list of dates which is used.
     * If {@link #autoRanging} is true, these values are used as lower and upper bounds.
     */
    private ZonedDateTime minDate, maxDate;

    private ObjectProperty<ZonedDateTime> lowerBound = new ObjectPropertyBase<ZonedDateTime>() {
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

    private ObjectProperty<ZonedDateTime> upperBound = new ObjectPropertyBase<ZonedDateTime>() {
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

    private ChartLayoutAnimator animator = new ChartLayoutAnimator(this);

    private Object currentAnimationID;

    private Interval actualInterval = Interval.DECADE;
    private ZoneOffset zoneOffset = ZoneOffset.UTC;

    /**
     * Default constructor. By default the lower and upper bound are calculated by the data.
     */
    public ZonedDateTimeAxis() {
        this.zoneId = ZoneId.systemDefault();
    }

    public ZonedDateTimeAxis(ZoneId zoneId) {
        this.zoneId = zoneId;
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
            minDate = maxDate = ZonedDateTime.now();
        }
        else if (list.size() == 1) {
            minDate = maxDate = list.get(0);
        }
        else if (list.size() > 1) {
            minDate = list.get(0);
            maxDate = list.get(list.size() - 1);
        }
    }

    @Override
    protected Object autoRange(double length) {
        if (isAutoRanging()) {
            return new Object[]{minDate, maxDate};
        }
        else {
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

        if (animating) {
//            final Timeline timeline = new Timeline();
//            timeline.setAutoReverse(false);
//            timeline.setCycleCount(1);
//            final AnimationTimer timer = new AnimationTimer() {
//                @Override
//                public void handle(long l) {
//                    requestAxisLayout();
//                }
//            };
//            timer.start();
//
//            timeline.setOnFinished(new EventHandler<ActionEvent>() {
//                @Override
//                public void handle(ActionEvent actionEvent) {
//                    timer.stop();
//                    requestAxisLayout();
//                }
//            });
//
//            KeyValue keyValue = new KeyValue(currentLowerBound, lower.getTime());
//            KeyValue keyValue2 = new KeyValue(currentUpperBound, upper.getTime());
//
//            timeline.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
//                    new KeyValue(currentLowerBound, oldLowerBound.getTime()),
//                    new KeyValue(currentUpperBound, oldUpperBound.getTime())),
//                    new KeyFrame(Duration.millis(3000), keyValue, keyValue2));
//            timeline.play();

            animator.stop(currentAnimationID);
            currentAnimationID = animator.animate(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, oldLowerBound.toInstant().toEpochMilli()),
                            new KeyValue(currentUpperBound, oldUpperBound.toInstant().toEpochMilli())
                    ),
                    new KeyFrame(Duration.millis(700),
                            new KeyValue(currentLowerBound, lower.toInstant().toEpochMilli()),
                            new KeyValue(currentUpperBound, upper.toInstant().toEpochMilli())
                    )
            );

        }
        else {
            currentLowerBound.set(getLowerBound().toInstant().toEpochMilli());
            currentUpperBound.set(getUpperBound().toInstant().toEpochMilli());
        }
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
        }
        else {
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
            // displayPosition = getHeight() - ((date - lowerBound) / diff) * range + getZero
            // date = displayPosition - getZero - getHeight())/range * diff + lowerBound
            long v = Math.round((displayPosition - getZeroPosition() - getHeight()) / -range * diff + currentLowerBound.get());
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), zoneId);
        }
        else {
            // displayPosition = ((date - lowerBound) / diff) * range + getZero
            // date = displayPosition - getZero)/range * diff + lowerBound
            long v = Math.round((displayPosition - getZeroPosition()) / range * diff + currentLowerBound.get());
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), zoneId);
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
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Math.round(v)), zoneId);
    }

    @Override
    protected List<ZonedDateTime> calculateTickValues(double v, Object range) {
        Object[] r = (Object[]) range;
        ZonedDateTime lower = (ZonedDateTime) r[0];
        ZonedDateTime upper = (ZonedDateTime) r[1];
        List<ZonedDateTime> dateList = new ArrayList<ZonedDateTime>();
        // The preferred gap which should be between two tick marks.
        double averageTickGap = 100;
        double averageTicks = v / averageTickGap;

        // Starting with the greatest unit, add one of each calendar unit.
        int i=0;
        while (i < Interval.values().length && dateList.size() <= averageTicks){
            Interval interval =  Interval.values()[i];
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
        // If gaps between dates are to small, remove one of them.
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
            if (upper.toInstant().toEpochMilli() - lastDate.toInstant().toEpochMilli() < (lastDate.toInstant().toEpochMilli() - previousLastDate.toInstant().toEpochMilli()) / 2) {
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
        if (actualInterval.unit == ChronoUnit.YEARS && date.getMonthValue() == 1 && date.getDayOfMonth() == 1) {
            formatter = DateTimeFormatter.ofPattern("yyyy");
        }
        else if (actualInterval.unit == ChronoUnit.MONTHS && date.getDayOfMonth() == 1) {
            formatter = DateTimeFormatter.ofPattern("MMM yy");
        }
        else {
            switch (actualInterval.unit) {
                case DAYS:
                case WEEKS:
                default:
                    formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                    break;
                case HOURS:
                case MINUTES:
                    formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
                    break;
                case SECONDS:
                    formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                    break;
                case MILLIS:
                    formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL);
                    break;
            }
        }
        return formatter.format(date);
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
            List<ZonedDateTime> evenDates = new ArrayList<ZonedDateTime>();

            // For each unit, modify the date slightly by a few millis, to make sure they are different days.
            // This is because Axis stores each value and won't update the tick labels, if the value is already known.
            // This happens if you display days and then add a date many years in the future the tick label will still be displayed as day.
            for (int i = 0; i < dates.size(); i++) {
                //  calendar.setTime(dates.get(i));

                ZonedDateTime date = dates.get(i);
                ZonedDateTime normalizedDate = date;
                boolean isFirstOrLast = i != 0 && i != dates.size() - 1;
                //  for(ZonedDateTime date : dates){
                switch (actualInterval.unit) {
                    case YEARS:
                        // If its not the first or last date (lower and upper bound), make the year begin with first month and let the months begin with first day.
//                        if (i != 0 && i != dates.size() - 1) {
//                            calendar.set(Calendar.MONTH, 0);
//                            calendar.set(Calendar.DATE, 1);
//                        }
//                        calendar.set(Calendar.HOUR_OF_DAY, 0);
//                        calendar.set(Calendar.MINUTE, 0);
//                        calendar.set(Calendar.SECOND, 0);
//                        calendar.set(Calendar.MILLISECOND, 6);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                isFirstOrLast ? 1 : date.getMonthValue(),
                                isFirstOrLast ? 1 : date.getDayOfMonth(),
                                0, 0, 0, 6, dates.get(i).getZone());
                        break;
                    case MONTHS:
                        // If its not the first or last date (lower and upper bound), make the months begin with first day.
//                        if (i != 0 && i != dates.size() - 1) {
//                            calendar.set(Calendar.DATE, 1);
//                        }
//                        calendar.set(Calendar.HOUR_OF_DAY, 0);
//                        calendar.set(Calendar.MINUTE, 0);
//                        calendar.set(Calendar.SECOND, 0);
//                        calendar.set(Calendar.MILLISECOND, 5);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                isFirstOrLast ? 1 : date.getDayOfMonth(),
                                0, 0, 0, 5, dates.get(i).getZone());
                        break;
                    case WEEKS:
                        // Make weeks begin with first day of week?
//                        calendar.set(Calendar.HOUR_OF_DAY, 0);
//                        calendar.set(Calendar.MINUTE, 0);
//                        calendar.set(Calendar.SECOND, 0);
//                        calendar.set(Calendar.MILLISECOND, 4);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                0, 0, 0, 4, dates.get(i).getZone());
                        break;
                    case DAYS:
//                        calendar.set(Calendar.HOUR_OF_DAY, 0);
//                        calendar.set(Calendar.MINUTE, 0);
//                        calendar.set(Calendar.SECOND, 0);
//                        calendar.set(Calendar.MILLISECOND, 3);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                0, 0, 0, 3, dates.get(i).getZone());
                        break;
                    case HOURS:
//                        if (i != 0 && i != dates.size() - 1) {
//                            calendar.set(Calendar.MINUTE, 0);
//                            calendar.set(Calendar.SECOND, 0);
//                        }
//                        calendar.set(Calendar.MILLISECOND, 2);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                date.getHour(),
                                isFirstOrLast ? 0 : date.getMinute(),
                                isFirstOrLast ? 0 : date.getSecond(),
                                2, dates.get(i).getZone());
                        break;
                    case MINUTES:
//                        if (i != 0 && i != dates.size() - 1) {
//                            calendar.set(Calendar.SECOND, 0);
//                        }
//                        calendar.set(Calendar.MILLISECOND, 1);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                date.getHour(),
                                date.getMinute(),
                                isFirstOrLast ? 0 : date.getSecond(),
                                1, dates.get(i).getZone());
                        break;
                    case SECONDS:
//                        calendar.set(Calendar.MILLISECOND, 0);
                        normalizedDate = ZonedDateTime.of(
                                date.getYear(),
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                date.getHour(),
                                date.getMinute(),
                                date.getSecond(),
                                1, dates.get(i).getZone());
                        break;

                }
                evenDates.add(normalizedDate);
            }

            return evenDates;
        }
        else {
            return dates;
        }
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The property.
     * @see #getLowerBound()
     */
    public final ObjectProperty<ZonedDateTime> lowerBoundProperty() {
        return lowerBound;
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The lower bound.
     * @see #lowerBoundProperty()
     */
    public final ZonedDateTime getLowerBound() {
        return lowerBound.get();
    }

    /**
     * Sets the lower bound of the axis.
     *
     * @param date The lower bound date.
     * @see #lowerBoundProperty()
     */
    public final void setLowerBound(ZonedDateTime date) {
        lowerBound.set(date);
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The property.
     * @see #getUpperBound() ()
     */
    public final ObjectProperty<ZonedDateTime> upperBoundProperty() {
        return upperBound;
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The upper bound.
     * @see #upperBoundProperty()
     */
    public final ZonedDateTime getUpperBound() {
        return upperBound.get();
    }

    /**
     * Sets the upper bound of the axis.
     *
     * @param date The upper bound date.
     * @see #upperBoundProperty() ()
     */
    public final void setUpperBound(ZonedDateTime date) {
        upperBound.set(date);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The converter.
     */
    public final StringConverter<ZonedDateTime> getTickLabelFormatter() {
        return tickLabelFormatter.getValue();
    }

    /**
     * Sets the tick label formatter for the ticks.
     *
     * @param value The converter.
     */
    public final void setTickLabelFormatter(StringConverter<ZonedDateTime> value) {
        tickLabelFormatter.setValue(value);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The property.
     */
    public final ObjectProperty<StringConverter<ZonedDateTime>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
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
        MILLISECOND(ChronoUnit.MILLIS, 1);

        private final int amount;

        private final ChronoUnit unit;

        private Interval(ChronoUnit interval, int amount) {
            this.unit = interval;
            this.amount = amount;
        }
    }
}