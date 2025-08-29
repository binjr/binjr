/*
 *    Copyright 2022-2025 Frederic Thevenet
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

package eu.binjr.core.preferences;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * An enumeration of commonly used date and time representation formats.
 */
public enum DateFormat {
    ISO(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS] Z")),
    RFC_1123(DateTimeFormatter.ofPattern("eee, dd MMM yyyy HH:mm:ss[.SSS] Z")),
    DURATION(new DateTimeFormatterBuilder()
            .appendValue(ChronoField.EPOCH_DAY)
            .appendLiteral("d ")
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral("h ")
            .appendValue(ChronoField.MINUTE_OF_HOUR)
            .appendLiteral("min ")
            .appendValue(ChronoField.SECOND_OF_MINUTE)
            .appendLiteral("s ")
            .appendValue(ChronoField.MILLI_OF_SECOND)
            .appendLiteral("ms")
            .toFormatter());

    private final DateTimeFormatter dateTimeFormatter;

    DateFormat(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    /**
     * Returns an instance of {@link DateTimeFormatter} for this {@link DateFormat}
     *
     * @return an instance of {@link DateTimeFormatter} for this {@link DateFormat}
     */
    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }


}
