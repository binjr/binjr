/*
 *    Copyright 2017-2019 Frederic Thevenet
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

import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.OsFamily;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;


/**
 * Bootstrap class for binjr to workaround for JavaFX runtime presence  checks built into openJfx 11 which incorrectly
 * fails if app is started from classpath.
 *
 * @author Frederic Thevenet
 */
public final class Bootstrap {
    private static final Logger logger = LogManager.getLogger(Bootstrap.class);

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (AppEnvironment.getInstance().getOsFamily() == OsFamily.LINUX){
                // Force openJfx to fall back to gtk 2 to workaround issue with Wayland
                System.setProperty("jdk.gtk.version","2");
                args = appendToArray(args,"--dialogs.resizable=true");
            }
            Binjr.main(args);
        } catch (Exception e) {
            logger.fatal("Failed to load binjr", e);
            System.exit(1);
        }
    }

    @SafeVarargs
    private static <T> T[] appendToArray(T[] array, T... elements) {
        T[] newArray = Arrays.copyOf(array, array.length +  elements.length);
        System.arraycopy(elements, 0, newArray, array.length, elements.length);
        return newArray;
    }
}
