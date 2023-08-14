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
        // Logs with uptime
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.144s][info][gc,phases      ] GC(59) Phase 4: Compact heap 11.810ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,heap        ] GC(59) Eden regions: 9->0(130)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,heap        ] GC(59) Survivor regions: 0->0(12)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,heap        ] GC(59) Old regions: 139->137"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,heap        ] GC(59) Archive regions: 0->0"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,heap        ] GC(59) Humongous regions: 10->10"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc,metaspace   ] GC(59) Metaspace: 48468K(49200K)->48468K(49200K) NonClass: 41947K(42416K)->41947K(42416K) Class: 6521K(6784K)->6521K(6784K)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.151s][info][gc             ] GC(59) Pause Full (System.gc()) 153M->143M(477M) 94.613ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[54.152s][info][gc,cpu         ] GC(59) User=0.66s Sys=0.00s Real=0.10s"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[65.250s][info][gc,heap,exit   ] Heap"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[65.250s][info][gc,heap,exit   ]  garbage-first heap   total 488448K, used 163531K [0x0000000080000000, 0x0000000100000000)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[65.250s][info][gc,heap,exit   ]   region size 1024K, 17 young (17408K), 0 survivors (0K)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[65.250s][info][gc,heap,exit   ]  Metaspace       used 48659K, capacity 49344K, committed 49456K, reserved 1091584K"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[65.250s][info][gc,heap,exit   ]   class space    used 6554K, capacity 6778K, committed 6784K, reserved 1048576K"));

        //Logs with time (ISO)
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.093+0200][info ][safepoint     ] Application time: 0.4142997 seconds"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.094+0200][info ][safepoint     ] Entering safepoint region: G1CollectForAllocation"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.097+0200][info ][gc,start      ] GC(846) Pause Young (Normal) (GCLocker Initiated GC)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.097+0200][info ][gc,task       ] GC(846) Using 143 workers of 143 for evacuation"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,phases     ] GC(846)   Pre Evacuate Collection Set: 32.2ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,phases     ] GC(846)   Evacuate Collection Set: 43.3ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,phases     ] GC(846)   Post Evacuate Collection Set: 9.2ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,phases     ] GC(846)   Other: 8.9ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,heap       ] GC(846) Eden regions: 14->0(949)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,heap       ] GC(846) Survivor regions: 10->11(960)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,heap       ] GC(846) Old regions: 7420->7420"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,heap       ] GC(846) Humongous regions: 11634->11632"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc,metaspace  ] GC(846) Metaspace: 231059K(255744K)->231059K(255744K)"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.190+0200][info ][gc            ] GC(846) Pause Young (Normal) (GCLocker Initiated GC) 610464M->609992M(614400M) 92.765ms"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.191+0200][info ][gc,cpu        ] GC(846) User=4.18s Sys=0.03s Real=0.09s"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.192+0200][info ][safepoint     ] Leaving safepoint region"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.192+0200][info ][safepoint     ] Total time for which application threads were stopped: 0.0987139 seconds, Stopping threads took: 0.0008103 seconds"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.224+0200][info ][safepoint     ] Application time: 0.0321887 seconds"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.286+0200][info ][safepoint     ] Entering safepoint region: G1CollectForAllocation"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.292+0200][debug][gc,jni        ] Setting _needs_gc. Thread `\"VM Thread\" 62 locked."));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.292+0200][info ][safepoint     ] Leaving safepoint region"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.292+0200][info ][safepoint     ] Total time for which application threads were stopped: 0.0685306 seconds, Stopping threads took: 0.0622806 seconds"));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.299+0200][debug][gc,jni        ] Allocation failed. Thread stalled by JNI critical section. Thread \"ForkJoinPool-280-worker-45_CommitThrottle#commit: top level tasks for transactions\" 48 locked."));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.299+0200][debug][gc,jni        ] Allocation failed. Thread stalled by JNI critical section. Thread \"ForkJoinPool-280-worker-111_CommitThrottle#commit: top level tasks for transactions\" 46 locked."));
        assertTrue(parsingTest(BuiltInParsingProfile.JVM, "[2023-02-22T06:46:55.305+0200][debug][gc,jni        ] Allocation failed. Thread stalled by JNI critical section. Thread \"ForkJoinPool-280-worker-25_CommitThrottle#commit: top level tasks for transactions\" 46 locked."));
    }

    private boolean parsingTest(ParsingProfile profile, String text) {
        var p = new LogEventFormat(profile, ZoneId.systemDefault(),  StandardCharsets.UTF_8);
        var res = p.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        System.out.println(res);
        return res.iterator().next() != null;
    }


}
