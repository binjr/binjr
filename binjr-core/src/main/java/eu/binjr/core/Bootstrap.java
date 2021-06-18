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

package eu.binjr.core;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.preferences.AppEnvironment;

import java.util.Arrays;


/**
 * Bootstrap class for binjr to workaround for JavaFX runtime presence  checks built into openJfx 11 which incorrectly
 * fails if app is started from classpath.
 *
 * @author Frederic Thevenet
 */
public final class Bootstrap {
    private static final Logger logger = Logger.create(Bootstrap.class);

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Binjr.main(args);
        } catch (Exception e) {
            logger.fatal("Failed to load " + AppEnvironment.APP_NAME, e);
            System.exit(1);
        }
    }
}
