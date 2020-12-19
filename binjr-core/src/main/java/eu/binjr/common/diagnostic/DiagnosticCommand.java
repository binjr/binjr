/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.common.diagnostic;

import eu.binjr.core.preferences.AppEnvironment;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Defines methods for a diagnostic command.
 */
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

    static void dumpHeap(Path path) throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                HotSpotDiagnosticHelper.dumpHeap(path);
                break;
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }
    }

    static Path getHeapDumpPath() throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                return HotSpotDiagnosticHelper.getHeapDumpPath();
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }

    }

    static void setHeapDumpPath(Path path) throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                HotSpotDiagnosticHelper.setHeapDumpPath(path);
                break;
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }
    }

    static boolean getHeapDumpOnOutOfMemoryError() throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                return HotSpotDiagnosticHelper.getHeapDumpOnOutOfMemoryError();
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }
    }

    static void setHeapDumpOnOutOfMemoryError(boolean value) throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                HotSpotDiagnosticHelper.setHeapDumpOnOutOfMemoryError(value);
                break;
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }
    }

    static String dumpVmOptions() throws DiagnosticException {
        switch (AppEnvironment.getInstance().getRunningJvm()) {
            case HOTSPOT:
                return HotSpotDiagnosticHelper.dumpVmOptions();
            case OPENJ9:
            case UNSUPPORTED:
            default:
                throw new UnavailableDiagnosticFeatureException();
        }
    }




    String threadPrint(String... args);

    String help(String... args);

    String vmSystemProperties(String... args);

    String gcClassHistogram(String... args);

    String vmFlags(String... args);

    String vmCommandLine(String... args);
}