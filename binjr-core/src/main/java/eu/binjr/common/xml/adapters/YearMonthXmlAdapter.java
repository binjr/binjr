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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.YearMonth;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code YearMonth} to a string such as 2007-12
 * <p>
 * String format details:
 * <ul>
 * <li>{@link YearMonth#parse(CharSequence)}</li>
 * <li>{@link YearMonth#toString()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see YearMonth
 */
public class YearMonthXmlAdapter extends XmlAdapter<String, YearMonth> {
    @Override
    public YearMonth unmarshal(String stringValue) {
        return stringValue != null ? YearMonth.parse(stringValue) : null;
    }

    @Override
    public String marshal(YearMonth value) {
        return value != null ? value.toString() : null;
    }
}
