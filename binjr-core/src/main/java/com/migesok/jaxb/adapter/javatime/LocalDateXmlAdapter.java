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

package com.migesok.jaxb.adapter.javatime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code LocalDate} to ISO-8601 string
 * <p>
 * It uses {@link DateTimeFormatter#ISO_DATE} for parsing and serializing,
 * time-zone information ignored.
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see LocalDate
 */
public class LocalDateXmlAdapter extends TemporalAccessorXmlAdapter<LocalDate> {
    public LocalDateXmlAdapter() {
        super(DateTimeFormatter.ISO_DATE, LocalDate::from);
    }
}
