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

import com.axibase.date.DatetimeProcessor;
import com.axibase.date.OnMissingDateComponentAction;
import com.axibase.date.PatternResolver;
import eu.binjr.common.logging.Logger;

import java.time.ZoneId;
import java.util.regex.Pattern;

public class ParserParameters {
    private static final Logger logger = Logger.create(ParserParameters.class);
    private final Pattern payloadPattern;
    private final Pattern timestampSyntax;
    private final boolean normalizeSeparators;
    private final String separatorsPattern;
    private final String separatorsReplacement;
    private final ZoneId zoneId;
    private final DatetimeProcessor datetimeProcessor;
    //  private final DateTimeFormatter dateTimeFormatter;


    private ParserParameters(String timestampSyntax,
                             String payloadPattern,
                             boolean normalizeSeparators,
                             String separatorsPattern,
                             String separatorsReplacement,
                             String timeFormatPattern,
                             ZoneId zoneId) {
        this.payloadPattern = Pattern.compile(payloadPattern);
        this.timestampSyntax = Pattern.compile(timestampSyntax);
        this.normalizeSeparators = normalizeSeparators;
        this.separatorsPattern = separatorsPattern;
        this.separatorsReplacement = separatorsReplacement;
        this.zoneId = zoneId;
        this.datetimeProcessor = PatternResolver.createNewFormatter(timeFormatPattern, zoneId, OnMissingDateComponentAction.SET_ZERO);
        // this.dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern).withZone(zoneId);
    }

    public Pattern getPayloadPattern() {
        return payloadPattern;
    }

    public Pattern getTimestampSyntax() {
        return timestampSyntax;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

//    public DateTimeFormatter getDateTimeFormatter() {
//        return dateTimeFormatter;
//    }

    public DatetimeProcessor getDatetimeProcessor() {
        return datetimeProcessor;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ParserParameters{");
        sb.append("payloadPattern=").append(payloadPattern);
        sb.append(", timestampPattern=").append(timestampSyntax);
        sb.append(", zoneId=").append(zoneId);
        sb.append(", dateTimeFormatter=").append(datetimeProcessor);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        public Builder setTimestampSyntax(String timestampSyntax) {
            this.timestampSyntax = timestampSyntax;
            return this;
        }

        public Builder setPayloadPattern(String payloadPattern) {
            this.payloadPattern = payloadPattern;
            return this;
        }

        public Builder setNormalizeSeparators(boolean normalizeSeparators) {
            this.normalizeSeparators = normalizeSeparators;
            return this;
        }

        public Builder setSeparatorsPattern(String separatorsPattern) {
            this.separatorsPattern = separatorsPattern;
            return this;
        }

        public Builder setSeparatorsReplacement(String separatorsReplacement) {
            this.separatorsReplacement = separatorsReplacement;
            return this;
        }

        public Builder setTimeFormatPattern(String timeFormatPattern) {
            this.timeFormatPattern = timeFormatPattern;
            return this;
        }

        public Builder setZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        private String timestampSyntax = "";
        private String payloadPattern = "";
        private boolean normalizeSeparators = false;
        private String separatorsPattern = "";
        private String separatorsReplacement = "";
        private String timeFormatPattern = "";
        private ZoneId zoneId = ZoneId.systemDefault();

        public Builder() {
        }

        public ParserParameters build() {
            return new ParserParameters(timestampSyntax,
                    payloadPattern,
                    normalizeSeparators,
                    separatorsPattern,
                    separatorsReplacement,
                    timeFormatPattern,
                    zoneId);
        }
    }
}
