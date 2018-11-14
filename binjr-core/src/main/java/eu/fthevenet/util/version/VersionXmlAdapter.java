/*
 *    Copyright 2018 Frederic Thevenet
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
 *
 */

package eu.fthevenet.util.version;

import javax.xml.bind.annotation.adapters.XmlAdapter;


public class VersionXmlAdapter extends XmlAdapter<String, Version> {
    public VersionXmlAdapter() {
    }

    public Version unmarshal(String versionString) {
        return versionString != null ? new Version(versionString) : null;
    }

    public String marshal(Version version) {
        return version != null ? version.toString() : null;
    }
}