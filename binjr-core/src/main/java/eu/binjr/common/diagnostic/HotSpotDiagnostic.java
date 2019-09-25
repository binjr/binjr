/*
 *    Copyright 2019 Frederic Thevenet
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

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public interface HotSpotDiagnostic {
    HotSpotDiagnosticMXBean HOTSPOT_DIAGNOSTIC = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);

    static List<VMOption> listOption() throws DiagnosticException {
        try {
            return HOTSPOT_DIAGNOSTIC.getDiagnosticOptions();
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to list VMOptions", t);
        }
    }

    static void dumpHeap(Path path) throws DiagnosticException {
        try {
           HOTSPOT_DIAGNOSTIC.dumpHeap(path.toString(), false);
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to get VMOption HeapDumpPath", t);
        }
    }

    static Path getHeapDumpPath() throws DiagnosticException {
        try {
            return Path.of(HOTSPOT_DIAGNOSTIC.getVMOption("HeapDumpPath").getValue());
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to get VMOption HeapDumpPath", t);
        }
    }

    static void setHeapDumpPath(Path path) throws DiagnosticException {
        try {
            HOTSPOT_DIAGNOSTIC.setVMOption("HeapDumpPath", path.toString());
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to set VMOption HeapDumpPath", t);
        }
    }

    static boolean getHeapDumpOnOutOfMemoryError() throws DiagnosticException {
        try {
            return Boolean.parseBoolean(HOTSPOT_DIAGNOSTIC.getVMOption("HeapDumpOnOutOfMemoryError").getValue());
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to get VMOption HeapDumpOnOutOfMemoryError", t);
        }
    }

    static void setHeapDumpOnOutOfMemoryError(boolean value) throws DiagnosticException {
        try {
            HOTSPOT_DIAGNOSTIC.setVMOption("HeapDumpOnOutOfMemoryError", Boolean.toString(value));
        } catch (Throwable t) {
            throw new DiagnosticException("Failed to set VMOption HeapDumpOnOutOfMemoryError", t);
        }
    }

}