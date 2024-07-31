/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.sources.rrd4j.adapters;

/**
 * RRD4J backend types.
 */
public enum Rrd4jBackendType {
    /**
     *  Backend based on the java.io.* package. RRD data is stored in files on the disk
     */
    FILE,
    /**
     *  Backend based on the java.io.* package. RRD data is stored in files on the disk.
     *  This backend is "safe". Being safe means that RRD files can be safely shared between several JVM's.
     */
    SAFE,
    /**
     * Backend based on the java.nio.* package. RRD data is stored in files on the disk
     */
    NIO,
    /**
     *  Memory-oriented backend. RRD data is stored in memory, it gets lost as soon as JVM exits.
     */
    MEMORY
}
