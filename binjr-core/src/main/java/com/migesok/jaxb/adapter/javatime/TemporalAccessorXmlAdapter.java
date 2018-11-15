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


import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import static java.util.Objects.requireNonNull;

/**
 * {@code XmlAdapter} mapping any JSR-310 {@code TemporalAccessor} to string using provided {@code DateTimeFormatter}
 * <p>
 * Example:
 * <pre>
 * {@code
 *  public class DottedDateXmlAdapter extends TemporalAccessorXmlAdapter<LocalDate> {
 *      public DottedDateXmlAdapter() {
 *          super(DateTimeFormatter.ofPattern("dd.MM.yyyy"), LocalDate::from);
 *      }
 *  }
 * }
 * </pre>
 *
 * @param <T> mapped temporal type
 * @see XmlAdapter
 * @see TemporalAccessor
 * @see DateTimeFormatter
 */
public class TemporalAccessorXmlAdapter<T extends TemporalAccessor> extends XmlAdapter<String, T> {
    private final DateTimeFormatter formatter;
    private final TemporalQuery<? extends T> temporalQuery;

    /**
     * @param formatter     the formatter for printing and parsing, not null
     * @param temporalQuery the query defining the type to parse to, not null
     */
    public TemporalAccessorXmlAdapter(DateTimeFormatter formatter,
                                      TemporalQuery<? extends T> temporalQuery) {
        this.formatter = requireNonNull(formatter, "formatter must not be null");
        this.temporalQuery = requireNonNull(temporalQuery, "temporal query must not be null");
    }

    @Override
    public T unmarshal(String stringValue) {
        return stringValue != null ? formatter.parse(stringValue, temporalQuery) : null;
    }

    @Override
    public String marshal(T value) {
        return value != null ? formatter.format(value) : null;
    }
}
