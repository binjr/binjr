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

package eu.binjr.core.update;

import eu.binjr.common.version.Version;

import java.nio.file.Path;

/**
 * Describes the platform specific behaviour of an update
 *
 * @author Frederic Thevenet
 */
public interface PlatformUpdater {

    /**
     * Returns true if "in-app" update is supported on the current platform, false otherwise.
     *
     * @return true if "in-app" update is supported on the current platform, false otherwise.
     */
    boolean isInAppUpdateSupported();

    /**
     * Starts the update process
     *
     * @param updatePackage    the path to the update package
     * @param updateVersion    the update version
     * @param restartRequested true if the app should be restarted once the update is complete, false otherwise.
     * @throws Exception if an error occurs during the update.
     */
    void launchUpdater(Path updatePackage, Version updateVersion, boolean restartRequested) throws Exception;

}
