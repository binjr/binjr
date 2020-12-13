/*
 *    Copyright 2017-2018 Frederic Thevenet
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

import eu.binjr.common.javafx.controls.TimeRange;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javafx.scene.paint.Color;


import java.sql.Time;

/**
 * An {@link XmlAdapter} for {@link TimeRange} objects
 *
 * @author Frederic Thevenet
 */
public class TimeRangeXmlAdapter extends XmlAdapter<String, TimeRange> {
    /**
     * Initializes a new instance of the {@link TimeRangeXmlAdapter} class
     */
    public TimeRangeXmlAdapter() {
    }

    @Override
    public TimeRange unmarshal(String stringValue) {
        return stringValue != null ? TimeRange.deSerialize(stringValue) : null;
    }

    @Override
    public String marshal(TimeRange value) {
        return value != null ? value.serialize() : null;
    }
}