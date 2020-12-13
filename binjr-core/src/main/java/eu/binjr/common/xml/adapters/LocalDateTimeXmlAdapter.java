/*
 *    Copyright (c) 2015 Mikhail Sokolov
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

package eu.binjr.common.xml.adapters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code LocalDateTime} to ISO-8601 string
 * <p>
 * String format details: {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
 *

 * @see LocalDateTime
 */
public class LocalDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<LocalDateTime> {
    public LocalDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from);
    }
}
