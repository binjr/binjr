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
import java.time.Period;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code Period} to ISO-8601 string
 * <p>
 * String format details:
 * <ul>
 * <li>{@link Period#parse(CharSequence)}</li>
 * <li>{@link Period#toString()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see Period
 */
public class PeriodXmlAdapter extends XmlAdapter<String, Period> {
    @Override
    public Period unmarshal(String stringValue) {
        return stringValue != null ? Period.parse(stringValue) : null;
    }

    @Override
    public String marshal(Period value) {
        return value != null ? value.toString() : null;
    }
}
