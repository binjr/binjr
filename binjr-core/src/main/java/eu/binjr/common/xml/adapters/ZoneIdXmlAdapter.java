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
import java.time.ZoneId;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code ZoneId} and {@code ZoneOffset} to the time-zone ID string
 * <p>
 * Time-zone ID format details:
 * <ul>
 * <li>{@link ZoneId#of(String)}</li>
 * <li>{@link ZoneId#getId()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see ZoneId
 * @see java.time.ZoneOffset
 */
public class ZoneIdXmlAdapter extends XmlAdapter<String, ZoneId> {
    @Override
    public ZoneId unmarshal(String stringValue) {
        return stringValue != null ? ZoneId.of(stringValue) : null;
    }

    @Override
    public String marshal(ZoneId value) {
        return value != null ? value.getId() : null;
    }
}
