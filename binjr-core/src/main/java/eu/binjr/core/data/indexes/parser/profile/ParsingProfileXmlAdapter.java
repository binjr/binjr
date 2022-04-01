/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser.profile;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;


/**
 * An {@link XmlAdapter} for {@link ParsingProfile} objects
 *
 * @author Frederic Thevenet
 */
public class ParsingProfileXmlAdapter extends XmlAdapter<String, ParsingProfile> {
    /**
     * Initializes a new instance of the CustomParsingProfile class
     */
    public ParsingProfileXmlAdapter() {
    }

    @Override
    public ParsingProfile unmarshal(String stringValue) {
        return (stringValue != null && !stringValue.isBlank()) ? CustomParsingProfile.fromJson(stringValue) : null;
    }

    @Override
    public String marshal(ParsingProfile value) {
        return value != null ? CustomParsingProfile.of(value).toJson() : null;
    }
}