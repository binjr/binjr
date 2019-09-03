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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * The implementation of {@link PlatformUpdater} for unsupported platforms.
 *
 * @author Frederic Thevenet
 */
public class UnsupportedUpdater implements PlatformUpdater {
    private static final Logger logger = LogManager.getLogger(UnsupportedUpdater.class);

    @Override
    public boolean isInAppUpdateSupported() {
        return false;
    }

    @Override
    public void launchUpdater(Path updatePackage, Version updateVersion, boolean restartRequested) throws Exception {
        logger.debug(() -> "UnsupportedUpdater launchUpdater invoked with " +
                "updatePackage=" + updatePackage +
                "updateVersion=" + updateVersion +
                "restartRequested=" + restartRequested);
    }
}
