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

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ResultVector {
    @SerializedName("metric")
    private Map<String, String> metric;

    @SerializedName("values")
    private ScalarSample[] values;

    public Map<String, String> getMetric() {
        return metric;
    }

    public ScalarSample[] getValues() {
        return values;
    }
}
