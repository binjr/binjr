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

package eu.binjr.sources.prometheus.api;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@JsonAdapter(StringSample.Adapter.class)
public record StringSample(ZonedDateTime timestamp, String value) {

    public static class Adapter extends TypeAdapter<StringSample> {
        @Override
        public void write(final JsonWriter jsonWriter, final StringSample sample) throws IOException {
            jsonWriter.beginArray();
            jsonWriter.value(String.format("%.3f", (double) sample.timestamp().toInstant().toEpochMilli() / 1000));
            jsonWriter.value(sample.value());
            jsonWriter.endArray();
        }

        @Override
        public StringSample read(final JsonReader jsonReader) throws IOException {
            jsonReader.beginArray();
            ZonedDateTime timestamp = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(Math.round(jsonReader.nextDouble() * 1000)),
                    ZoneId.systemDefault());
            String value = jsonReader.nextString();
            jsonReader.endArray();
            return new StringSample(timestamp, value);
        }
    }
}
