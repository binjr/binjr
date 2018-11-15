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
import java.time.Year;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code Year} to ISO proleptic year number
 * <p>
 * Year number interpretation details:
 * <ul>
 * <li>{@link Year#of(int)}</li>
 * <li>{@link Year#getValue()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see Year
 */
public class YearXmlAdapter extends XmlAdapter<Integer, Year> {
    @Override
    public Year unmarshal(Integer isoYearInt) {
        return isoYearInt != null ? Year.of(isoYearInt) : null;
    }

    @Override
    public Integer marshal(Year year) {
        return year != null ? year.getValue() : null;
    }
}
