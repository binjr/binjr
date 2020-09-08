/*
 *    Copyright 2019 Frederic Thevenet
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

package eu.binjr.common.javafx.controls;

import javafx.scene.input.DataFormat;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class TimeRange {
    public static final DataFormat TIME_RANGE_DATA_FORMAT = new DataFormat(TimeRange.class.getCanonicalName());
    private static final String DELIMITER = "\n";
    private final ZonedDateTime beginning;
    private final ZonedDateTime end;
    private final Duration duration;
    private final ZoneId zoneId;

    public static TimeRange of(TimeRange range) {
        return new TimeRange(range.getBeginning(), range.getEnd());
    }

    public static TimeRange of(ZonedDateTime beginning, ZonedDateTime end) {
        return new TimeRange(beginning, end);
    }

    public static TimeRange last24Hours(){
        var end = ZonedDateTime.now();
        return new TimeRange(end.minusHours(24), end);
    }

    TimeRange(ZonedDateTime beginning, ZonedDateTime end) {
        Objects.requireNonNull(beginning, "Parameter 'beginning' must not be null");
        Objects.requireNonNull(end, "Parameter 'end' must not be null");
        this.zoneId = beginning.getZone();
        this.beginning = beginning;
        this.end = end.withZoneSameInstant(zoneId);
        this.duration = Duration.between(beginning, end);
    }

    public ZonedDateTime getBeginning() {
        return beginning;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public Duration getDuration() {
        return duration;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public boolean isNegative() {
        return duration.isNegative();
    }

    public String serialize() {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(beginning) + DELIMITER + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(end);
    }

    public static TimeRange deSerialize(String xmlString) {
        String[] s = xmlString.split(DELIMITER);
        if (s.length != 2) {
            throw new IllegalArgumentException("Could not parse provided string as a TimeRange");
        }
        return TimeRange.of(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[0], ZonedDateTime::from), DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[1], ZonedDateTime::from));
    }
}
