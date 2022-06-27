/*
 *    Copyright 2020-2022 Frederic Thevenet
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

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

public enum TemporalCaptureGroup implements NamedCaptureGroup {
    YEAR(ChronoField.YEAR_OF_ERA),
    MONTH(ChronoField.MONTH_OF_YEAR),
    DAY(ChronoField.DAY_OF_MONTH),
    HOUR(ChronoField.HOUR_OF_DAY),
    MINUTE(ChronoField.MINUTE_OF_HOUR),
    SECOND(ChronoField.SECOND_OF_MINUTE),
    EPOCH(ChronoField.INSTANT_SECONDS),
    MILLI(ChronoField.MILLI_OF_SECOND),
    NANO(ChronoField.NANO_OF_SECOND),
    FRACTION(ChronoField.MILLI_OF_SECOND);

    private final TemporalField temporalMapping;

    TemporalCaptureGroup(TemporalField temporalMapping) {
        this.temporalMapping = temporalMapping;
    }

    public TemporalField getMapping() {
        return temporalMapping;
    }

}
