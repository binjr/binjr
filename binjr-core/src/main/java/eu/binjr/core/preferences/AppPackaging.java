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

package eu.binjr.core.preferences;

public enum AppPackaging {
    UNKNOWN("Unknown", "unknown"),
    WIN_MSI("Windows MSI", "msi"),
    WIN_ZIP("Windows ZIP", "zip"),
    LINUX_TAR("Linux Tarball", "tar.gz"),
    LINUX_DEB("Linux DEB", "deb"),
    LINUX_RPM("Linux DEB", "deb"),
    MAC_DMG("macOS DMG", "dmg"),
    MAC_TAR("macOS Tarball", "tar.gz");

    private final String name;
    private final String bundleExtension;

    AppPackaging(String name, String bundleExtension) {
        this.bundleExtension = bundleExtension;
        this.name = name;
    }

    public String getBundleExtension() {
        return bundleExtension;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
