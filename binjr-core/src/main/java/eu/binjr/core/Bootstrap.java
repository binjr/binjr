/*
 *    Copyright 2017-2024 Frederic Thevenet
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
import eu.binjr.core.preferences.ScalingFactor;
import eu.binjr.core.preferences.UserPreferences;


/**
 * Bootstrap class for binjr to initialize system properties prior to JavaFX Application initialization.
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
            // Property "glass.gtk.uiScale" needs to be set before JavaFX is initialized
            var uiScale = UserPreferences.getInstance().uiScalingFactor.get();
            if (uiScale != ScalingFactor.AUTO) {
                System.setProperty("glass.win.uiScale", uiScale.getLabel());
                System.setProperty("glass.gtk.uiScale", uiScale.getLabel());
            }
            Binjr.main(args);
        } catch (Exception e) {
            logger.fatal("Failed to load " + AppEnvironment.APP_NAME, e);
            System.exit(1);
        }
    }
}
