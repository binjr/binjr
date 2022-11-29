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

import com.google.gson.Gson;
import eu.binjr.sources.prometheus.api.RangeResultResponse;
import eu.binjr.sources.prometheus.api.ScalarSample;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PrometheusApiTests {
    private static final Gson GSON = new Gson();

    @Test
    public void testSerializeScalarSample() throws Exception {
        var sample = new ScalarSample(ZonedDateTime.of(2022, 11, 29, 18, 5, 42, 123000000, ZoneId.of("Europe/Paris")), Double.NaN);
        String str =  GSON.toJson(sample);
        System.out.println("sample = " + str);
        Assertions.assertEquals(str, "[\"1669741542.123\",\"NaN\"]");

    }

    @Test
    public void testDeserializeRangeResultResponse() throws Exception {
        var payload = """
                {"status":"success",
                 "isPartial":false,
                 "data":{
                     "resultType":"matrix",
                     "result":[
                         {"metric":{
                             "__name__":"os.memory.usedMemoryKbytes",
                             "cluster":"cluster002",
                             "datacenter":"euw1",
                             "vmname":"host001"
                             }
                         ,"values":
                         [[1668418200,"32589322"],
                          [1668418800,"31958914"],
                          [1668419400,"34341140"],
                          [1668420000,"31895174"],
                          [1668420600,"31216586"],
                          [1668421200,"29785852"],
                          [1668421800,"29758120"],
                          [1668422400,"29756618"],
                          [1668423000,"29765854"],
                          [1668423600,"32612068"],
                          [1668424200,"32525560"]]
                     }]}}
                """;
        RangeResultResponse response = GSON.fromJson(payload, RangeResultResponse.class);

        System.out.println("res = " + response);
        Assertions.assertNotNull(response);

    }
}
