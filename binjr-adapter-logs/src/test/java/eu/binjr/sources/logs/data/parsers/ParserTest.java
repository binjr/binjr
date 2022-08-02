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

package eu.binjr.sources.logs.data.parsers;


import eu.binjr.core.data.indexes.parser.LogEventFormat;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTest {

    @Test
    public void parseBinjrLogsFull() {
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020/07/21-02:56:42.460] [info] Hello World!"));
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020-11-13 19:59:22.627] [INFO ] Hello world!"));
    }

    @Test
    public void parseBinjrLogsNoFraction() {
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020/07/21-02:56:42] [info] Hello World!"));
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020-11-13 19:59:22] [INFO ] Hello world!"));
    }

    @Test
    public void parseBinjrLogsUnknownSeverity() {
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020-11-13 19:59:22.627] [FOO  ] Hello world!"));
    }

    @Test
    public void parseBinjrLogsNoSeverity() {
        assertTrue(parsingTest(BuiltInParsingProfile.ISO, "[2020-11-13 19:59:22.627] Hello world!"));
    }

    @Test
    public void parseBinjrStrictFull() {
        assertFalse(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020/07/21-02:56:42.460] [info] Hello World!"));
        assertTrue(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020-11-13 19:59:22.627] [INFO ] Hello world!"));
    }

    @Test
    public void parseBinjrStrictNoFraction() {
        assertFalse(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020/07/21-02:56:42] [info] Hello World!"));
        assertFalse(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020-11-13 19:59:22] [INFO ] Hello world!"));
    }

    @Test
    public void parseBinjrStrictUnknownSeverity() {
        assertFalse(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020-11-13 19:59:22.627] [FOO  ] Hello world!"));
    }

    @Test
    public void parseBinjrStrictNoSeverity() {
        assertFalse(parsingTest(BuiltInParsingProfile.BINJR_STRICT, "[2020-11-13 19:59:22.627] Hello world!"));
    }


    @Test
    public void parseGc() {
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.144s][info][gc,phases      ] GC(59) Phase 4: Compact heap 11.810ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,heap        ] GC(59) Eden regions: 9->0(130)"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,heap        ] GC(59) Survivor regions: 0->0(12)"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,heap        ] GC(59) Old regions: 139->137"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,heap        ] GC(59) Archive regions: 0->0"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,heap        ] GC(59) Humongous regions: 10->10"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc,metaspace   ] GC(59) Metaspace: 48468K(49200K)->48468K(49200K) NonClass: 41947K(42416K)->41947K(42416K) Class: 6521K(6784K)->6521K(6784K)"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.151s][info][gc             ] GC(59) Pause Full (System.gc()) 153M->143M(477M) 94.613ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[54.152s][info][gc,cpu         ] GC(59) User=0.66s Sys=0.00s Real=0.10s"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[65.250s][info][gc,heap,exit   ] Heap"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[65.250s][info][gc,heap,exit   ]  garbage-first heap   total 488448K, used 163531K [0x0000000080000000, 0x0000000100000000)"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[65.250s][info][gc,heap,exit   ]   region size 1024K, 17 young (17408K), 0 survivors (0K)"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[65.250s][info][gc,heap,exit   ]  Metaspace       used 48659K, capacity 49344K, committed 49456K, reserved 1091584K"));
        assertTrue(parsingTest(BuiltInParsingProfile.GC, "[65.250s][info][gc,heap,exit   ]   class space    used 6554K, capacity 6778K, committed 6784K, reserved 1048576K"));
    }

    private boolean parsingTest(ParsingProfile profile, String text) {
        var p = new LogEventFormat(profile, ZoneId.systemDefault(),  StandardCharsets.UTF_8);
        var res = p.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        System.out.println(res);
        return res.iterator().next() != null;
    }


}
