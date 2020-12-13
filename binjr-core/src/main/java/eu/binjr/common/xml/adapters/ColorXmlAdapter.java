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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javafx.scene.paint.Color;


/**
 * An {@link XmlAdapter} for {@link Color} objects
 *
 * @author Frederic Thevenet
 */
public class ColorXmlAdapter extends XmlAdapter<String, Color> {
    /**
     * Initializes a new instance of the {@link ColorXmlAdapter} class
     */
    public ColorXmlAdapter() {
    }

    @Override
    public Color unmarshal(String stringValue) {
        return stringValue != null ? Color.valueOf(stringValue) : null;
    }

    @Override
    public String marshal(Color value) {
        return value != null ? value.toString() : null;
    }
}