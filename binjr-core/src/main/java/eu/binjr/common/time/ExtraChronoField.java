package eu.binjr.common.time;

import java.time.temporal.*;
import java.util.Locale;
import java.util.function.Function;


import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;

public enum ExtraChronoField implements TemporalField {

    SECONDS_OF_YEAR(
            "SecondsOfYear",
            SECONDS,
            YEARS,
            ValueRange.of(0, 3600 * 24 * 365),
            t -> t.getLong(SECOND_OF_DAY) + (t.getLong(DAY_OF_YEAR) - 1) * 3600L * 24),
    MILLIS_OF_YEAR(
            "MillisOfYear",
            MILLIS,
            YEARS,
            ValueRange.of(0, 3600_000L * 24 * 365),
            t -> t.getLong(MILLI_OF_DAY) + (t.getLong(DAY_OF_YEAR) - 1) * 3600_000L * 24),
    MICROS_OF_YEAR(
            "MicrosOfYear",
            MICROS,
            YEARS,
            ValueRange.of(0, 3600_000_000L * 24 * 365),
            t -> t.getLong(MICRO_OF_DAY) + (t.getLong(DAY_OF_YEAR) - 1) * 3600_000_000L * 24),
    NANOS_OF_YEAR(
            "NanosOfYear",
            NANOS,
            YEARS,
            ValueRange.of(0, 3600_000_000_000L * 24 * 365),
            t -> t.getLong(NANO_OF_DAY) + (t.getLong(DAY_OF_YEAR) - 1) * 3600_000_000_000L * 24);

      private final String name;
    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;
    private final Function<TemporalAccessor, Long> toLong;

    ExtraChronoField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range, Function<TemporalAccessor, Long> toLong) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
        this.toLong = toLong;
    }

    @Override
    public String getDisplayName(Locale locale) {
        return name;
    }

    @Override
    public TemporalUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public TemporalUnit getRangeUnit() {
        return rangeUnit;
    }


    @Override
    public ValueRange range() {
        return range;
    }


    @Override
    public boolean isDateBased() {
        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }

    @Override
    public boolean isTimeBased() {
        return ordinal() < DAY_OF_WEEK.ordinal();
    }


    public long checkValidValue(long value) {
        return range().checkValidValue(value, this);
    }


    public int checkValidIntValue(long value) {
        return range().checkValidIntValue(value, this);
    }

    @Override
    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        return temporal.range(this);
    }

    @Override
    public long getFrom(TemporalAccessor t) {
        return toLong.apply(t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        return (R) temporal.with(this, newValue);
    }

    @Override
    public String toString() {
        return name;
    }

}