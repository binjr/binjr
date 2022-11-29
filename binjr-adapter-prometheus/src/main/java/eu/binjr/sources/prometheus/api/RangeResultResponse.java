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

public class RangeResultResponse extends ApiResponse {
    @SerializedName("data")
    private RangeData data;

    public static class RangeData {
        @SerializedName("resultType")
        private ResultType resultType;

        @SerializedName("result")
        private ResultVector[] result;

        public ResultType getResultType() {
            return resultType;
        }

        public ResultVector[] getResult() {
            return result;
        }
    }

    public RangeData getData() {
        return data;
    }
}
