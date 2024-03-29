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

package eu.binjr.core.preferences;

/**
 * An enumeration of supported families of operating systems.
 *
 * @author Frederic Thevenet
 */
public enum OsFamily {
    LINUX("linux-amd64"),
    OSX("mac-x86_64"),
    UNSUPPORTED("unsupported"),
    WINDOWS("windows-amd64");

    private final String platformClassifier;

    OsFamily(String platformClassifier){
        this.platformClassifier = platformClassifier;
    }

    public String getPlatformClassifier() {
        return platformClassifier;
    }
}

