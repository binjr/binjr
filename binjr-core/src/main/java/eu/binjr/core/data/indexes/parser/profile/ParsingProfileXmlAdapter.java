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

import com.google.gson.Gson;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;


/**
 * An {@link XmlAdapter} for {@link ParsingProfile} objects
 *
 * @author Frederic Thevenet
 */
public class ParsingProfileXmlAdapter extends XmlAdapter<String, ParsingProfile> {
    private static final Gson GSON = new Gson();

    /**
     * Initializes a new instance of the CustomParsingProfile class
     */
    public ParsingProfileXmlAdapter() {
    }

    @Override
    public ParsingProfile unmarshal(String stringValue) {
        if (stringValue == null || stringValue.isBlank()){
            return  BuiltInParsingProfile.ALL;
        }
        if (stringValue.startsWith(BuiltInParsingProfile.class.getTypeName())){
            return BuiltInParsingProfile.valueOf(stringValue.replace(BuiltInParsingProfile.class.getTypeName() + ":", ""));
        }
        return  GSON.fromJson(stringValue, CustomParsingProfile.class);
    }

    @Override
    public String marshal(ParsingProfile value) {
        if (value instanceof BuiltInParsingProfile builtIn) {
            return builtIn.getClass().getTypeName() + ":" + builtIn.name();
        }

        return value != null ? GSON.toJson(CustomParsingProfile.of(value)) : null;
    }
}