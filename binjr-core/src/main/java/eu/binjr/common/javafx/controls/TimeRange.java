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

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import javafx.scene.input.DataFormat;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@JsonAdapter(TimeRange.Adapter.class)
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TimeRange {
    public static final DataFormat TIME_RANGE_DATA_FORMAT = new DataFormat(TimeRange.class.getCanonicalName());
    private static final String DELIMITER = " ";
    private final ZonedDateTime beginning;
    private final ZonedDateTime end;
    private final ZoneId zoneId;

    private TimeRange(ZonedDateTime beginning, ZonedDateTime end) {
        Objects.requireNonNull(beginning, "Parameter 'beginning' must not be null");
        Objects.requireNonNull(end, "Parameter 'end' must not be null");
        this.zoneId = beginning.getZone();
        this.beginning = beginning;
        this.end = end.withZoneSameInstant(zoneId);
    }

    public static TimeRange of(TimeRange range) {
        return new TimeRange(range.getBeginning(), range.getEnd());
    }

    public static TimeRange of(ZonedDateTime beginning, ZonedDateTime end) {
        return new TimeRange(beginning, end);
    }

    public static TimeRange last24Hours() {
        var end = ZonedDateTime.now();
        return new TimeRange(end.minusHours(24), end);
    }

    public static TimeRange deSerialize(String valueStr) {
        String[] s = valueStr.split(DELIMITER);
        if (s.length != 2) {
            throw new IllegalArgumentException("Could not parse provided string as a TimeRange");
        }
        return TimeRange.of(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[0], ZonedDateTime::from), DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[1], ZonedDateTime::from));
    }

    public ZonedDateTime getBeginning() {
        return beginning;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public Duration getDuration() {
        return Duration.between(beginning, end);
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public boolean isNegative() {
        return getDuration().isNegative();
    }

    public String serialize() {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(beginning) + DELIMITER + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(end);
    }

    @Override
    public int hashCode() {
        return beginning.hashCode() + end.hashCode() + zoneId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var other = (TimeRange) obj;
        return beginning.equals(other.beginning) &&
                end.equals(other.end) &&
                zoneId.equals(other.zoneId);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TimeRange{");
        sb.append("beginning=").append(beginning);
        sb.append(", end=").append(end);
        sb.append(", zoneId=").append(zoneId);
        sb.append('}');
        return sb.toString();
    }

    public static class Adapter extends TypeAdapter<TimeRange> {
        @Override
        public void write(final JsonWriter jsonWriter, final TimeRange value) throws IOException {
            jsonWriter.value(value.serialize());
        }

        @Override
        public TimeRange read(final JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();
            return TimeRange.deSerialize(value);
        }
    }
}
