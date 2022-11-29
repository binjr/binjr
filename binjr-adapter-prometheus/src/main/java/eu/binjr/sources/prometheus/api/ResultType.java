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

@JsonAdapter(ResultType.Adapter.class)
public enum ResultType {
    MATRIX("matrix"),
    VECTOR("vector"),
    SCALAR("scalar"),
    STRING("string");
    private final String value;

    ResultType(String value) {
        this.value = value;

    }

    public String getValue() {
        return value;
    }

    public static ResultType fromValue(String value) {
        for (ResultType b : ResultType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<ResultType> {
        @Override
        public void write(final JsonWriter jsonWriter, final ResultType enumeration) throws IOException {
            jsonWriter.value(enumeration.getValue());
        }

        @Override
        public ResultType read(final JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();
            return ResultType.fromValue(value);
        }
    }
}
