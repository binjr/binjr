package eu.binjr.common.time;

import java.time.temporal.*;
import java.util.Locale;


import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;

public enum ExtraChronoField implements TemporalField {

    MILLI_OF_DAY(
            "MilliOfDay",
            MILLIS,
            DAYS,
            SECOND_OF_DAY,
            ValueRange.of(0, 3_600_000L * 24)),
    MICRO_OF_DAY(
            "MicrosOfDay",
            MICROS,
            DAYS,
            SECOND_OF_DAY,
            ValueRange.of(0, 3_600_000_000L * 24)),
    NANO_OF_DAY(
            "NanoOfDay",
            NANOS,
            DAYS,
            SECOND_OF_DAY,
            ValueRange.of(0, 3_600_000_000_000L * 24)),
    INSTANT_MILLIS(
            "InstantMillis",
            MILLIS,
            FOREVER,
            INSTANT_SECONDS,
            ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)),
    INSTANT_MICROS(
            "InstantMicros",
            MICROS,
            FOREVER,
            INSTANT_SECONDS,
            ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)),
    INSTANT_NANOS(
            "InstantNanos",
            NANOS,
            FOREVER,
            INSTANT_SECONDS,
            ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE));

    private final String name;
    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;
    private final long distToSec;
    private final long distToNano;
    private final ChronoField secondMapping;

    ExtraChronoField(String name,
                     TemporalUnit baseUnit,
                     TemporalUnit rangeUnit,
                     ChronoField secondMapping,
                     ValueRange range) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
        this.distToNano = baseUnit.getDuration().toNanos();
        this.distToSec = 1_000_000_000L / this.distToNano;
        this.secondMapping = secondMapping;
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
        return t.getLong(secondMapping) * distToSec + (t.getLong(NANO_OF_SECOND) / distToNano);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        var nbSec = newValue / distToSec;
        var nbNanos = (newValue % distToSec) * distToNano;
        var res = temporal.with(secondMapping, nbSec);
        return (R) res.with(NANO_OF_SECOND, nbNanos);
    }

    @Override
    public String toString() {
        return name;
    }

}