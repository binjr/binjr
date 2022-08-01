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

package eu.binjr.common.json.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;

public class LocaleJsonAdapter extends TypeAdapter<Locale> {
    @Override
    public void write(JsonWriter jsonWriter, Locale locale) throws IOException {
        jsonWriter.value(locale.toLanguageTag());
    }

    @Override
    public Locale read(JsonReader jsonReader) throws IOException {
        return Locale.forLanguageTag(jsonReader.nextString());
    }
}
