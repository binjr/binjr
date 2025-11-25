/*
 *    Copyright 2020-2025 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser.capture;

import eu.binjr.common.time.ExtraChronoField;

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Locale;
import java.util.function.Function;

public enum TemporalCaptureGroup implements NamedCaptureGroup {
    YEAR(ChronoField.YEAR_OF_ERA),
    MONTH(ChronoField.MONTH_OF_YEAR, s -> {
        if (s == null) {
            return null;
        }
        if (s.trim().length() < 3) {
            return Long.parseLong(s);
        }
        return switch (s.toLowerCase(Locale.ROOT).trim().substring(0, 3)) {
            case "jan" -> 1L;
            case "feb", "fev" -> 2L;
            case "mar" -> 3L;
            case "apr", "avr" -> 4L;
            case "may", "mai" -> 5L;
            case "jun", "jui" -> 6L;
            case "jul" -> 7L;
            case "aug", "aou" -> 8L;
            case "sep" -> 9L;
            case "oct" -> 10L;
            case "nov" -> 11L;
            case "dec" -> 12L;
            default -> Long.parseLong(s);
        };
    }),
    DAY(ChronoField.DAY_OF_MONTH),
    HOUR(ChronoField.HOUR_OF_DAY),
    MINUTE(ChronoField.MINUTE_OF_HOUR),
    SECOND(ChronoField.SECOND_OF_MINUTE),
    MILLI(ChronoField.MILLI_OF_SECOND),
    NANO(ChronoField.NANO_OF_SECOND),
    FRACTION(ChronoField.MILLI_OF_SECOND),
    ELAPSEDSECONDS(ChronoField.SECOND_OF_DAY),
    OFFSET(ChronoField.OFFSET_SECONDS, s -> {
        if (s.equals("Z")) {
            return 0;
        }
        if (s.matches("[+-]\\d{2}")) {
            // Assume offset format +-HH
            return Long.parseLong(s) * 3600;
        }
        if (s.matches("[+-]\\d{2}:?\\d{2}")) {
            // Assume offset format +-HHMM or +-HH:MM
            var num = Long.parseLong(s.replace(":", ""));
            var hours = num / 100;
            var minutes = num - (hours * 100);
            return hours * 3600 + minutes * 60;
        }
        // Assume offset in seconds
        return Long.parseLong(s);
    }),
    ELAPSEDMILLIS(ExtraChronoField.MILLI_OF_DAY),
    ELAPSEDMICROS(ExtraChronoField.INSTANT_MICROS),
    ELAPSEDNANOS(ExtraChronoField.INSTANT_NANOS),
    EPOCHSECONDS(ChronoField.INSTANT_SECONDS),
    EPOCHMILLIS(ExtraChronoField.INSTANT_MILLIS),
    EPOCHMICROS(ExtraChronoField.INSTANT_MICROS),
    EPOCHNANOS(ExtraChronoField.INSTANT_NANOS);

    private final TemporalField temporalMapping;
    private final Function<String, Number> tranformer;

    TemporalCaptureGroup(TemporalField temporalMapping, Function<String, Number> translator) {
        this.temporalMapping = temporalMapping;
        this.tranformer = translator;
    }

    TemporalCaptureGroup(TemporalField temporalMapping) {
        this(temporalMapping, Long::parseLong);
    }

    public TemporalField getMapping() {
        return temporalMapping;
    }

    public long parseLong(String input) {
        return tranformer.apply(input).longValue();
    }

    public int parseInt(String input) {
        return tranformer.apply(input).intValue();
    }
}
