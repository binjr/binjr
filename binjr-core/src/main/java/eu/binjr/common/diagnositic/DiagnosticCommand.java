/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.binjr.common.diagnositic;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

public interface DiagnosticCommand {
    DiagnosticCommand local = ((Supplier<DiagnosticCommand>) () -> {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.sun.management", "type", "DiagnosticCommand");
            return JMX.newMBeanProxy(server, name, DiagnosticCommand.class);
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e);
        }
    }).get();

    static String dumpVmSystemProperties() throws DiagnosticException {
        try {
            return local.vmSystemProperties();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command VM.System_properties", t);
        }
    }

    static String dumpThreadStacks() throws DiagnosticException {
        try {
            return local.threadPrint("-l");
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command Thread.print", t);
        }
    }

    static String dumpClassHistogram() throws DiagnosticException {
        try {
            return local.gcClassHistogram();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command GC.class_histogram", t);
        }
    }

    static String dumpVmFlags() throws DiagnosticException {
        try {
            return local.vmFlags();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command VM.flags", t);
        }
    }

    static String dumpVmCommandLine() throws DiagnosticException {
        try {
            return local.vmCommandLine();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command VM.command_line", t);
        }
    }

    static String getHelp() throws DiagnosticException {
        try {
            return local.help();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to invoke diagnostic command help", t);
        }
    }

    String threadPrint(String... args);

    String help(String... args);

    String vmSystemProperties(String... args);

    String gcClassHistogram(String... args);

    String vmFlags(String... args);

    String vmCommandLine(String... args);
}