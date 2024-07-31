/*
 *    Copyright 2020-2022 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser.capture;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Arrays;

@JsonAdapter(NamedCaptureGroup.JsonAdapter.class)
public interface NamedCaptureGroup {

    String name();

   class JsonAdapter extends TypeAdapter<NamedCaptureGroup> {
        @Override
        public void write(JsonWriter out, NamedCaptureGroup value) throws IOException {
            out.value(value.name());
        }

        @Override
        public NamedCaptureGroup read(final JsonReader jsonReader) throws IOException {
            var groupName = jsonReader.nextString();
            return Arrays.stream(TemporalCaptureGroup.values())
                    .filter(t -> t.name().equals(groupName))
                    .map(t -> (NamedCaptureGroup) t)
                    .findAny()
                    .orElse(CaptureGroup.of(groupName));
        }
    }
}

