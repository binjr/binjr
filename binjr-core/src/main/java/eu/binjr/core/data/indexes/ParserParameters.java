/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.core.data.indexes;

import eu.binjr.common.logging.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ParserParameters {
    private static final Logger logger = Logger.create(ParserParameters.class);
    private final Pattern payloadPattern;
    private final Pattern timestampPattern;
    private final ZoneId zoneId;
    private final DateTimeFormatter dateTimeFormatter;


    public ParserParameters(String timestampPattern, String payloadPattern, String timeFormatPattern, ZoneId zoneId){
        this.payloadPattern = Pattern.compile(payloadPattern);
        this.timestampPattern = Pattern.compile(timestampPattern);
        this.zoneId = zoneId;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern).withZone(zoneId);
    }

    public Pattern getPayloadPattern() {
        return payloadPattern;
    }

    public Pattern getTimestampPattern() {
        return timestampPattern;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ParserParameters{");
        sb.append("payloadPattern=").append(payloadPattern);
        sb.append(", timestampPattern=").append(timestampPattern);
        sb.append(", zoneId=").append(zoneId);
        sb.append(", dateTimeFormatter=").append(dateTimeFormatter);
        sb.append('}');
        return sb.toString();
    }
}
