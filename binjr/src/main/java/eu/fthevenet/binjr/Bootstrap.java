/*
 *    Copyright 2017 Frederic Thevenet
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

package eu.fthevenet.binjr;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bootstrap class for binjr that checks for JavaFX runtime presence and fails with an explicit error if not.
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
        boolean forceStart = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--forcestart") || arg.equalsIgnoreCase("-f")) {
                forceStart = true;
            }
        }
        if (!checkForJavaVersion()) {
            logger.log(forceStart ? Level.WARN : Level.FATAL, "This version on binjr only supports Java 8 and will likely fail on the version you're currently running. Please check 'https://github.com/fthevenet/binjr' for a version that runs on more recent versions of Java.");
            if (!forceStart) {
                System.exit(1);
            }
        }
        if (!checkForJavaFX()) {
            logger.log(forceStart ? Level.WARN : Level.FATAL, "The JavaFX runtime must be present in order to run binjr. Please check with your Java vendor to see is JavaFX is available on your platform and how to install it.");
            if (!forceStart) {
                System.exit(1);
            }
        }
        try {
            Binjr.main(args);
        } catch (Exception e) {
            logger.fatal("Failed to load binjr", e);
            System.exit(1);
        }
    }

    private static boolean checkForJavaVersion() {
        try {
            if (System.getProperty("java.runtime.version").startsWith("1.8")) {
                return true;
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return false;
    }

    private static boolean checkForJavaFX() {
        try {
            logger.info("Class " + Class.forName("javafx.application.Application") + " found: JavaFX runtime is available.");
            return true;
        } catch (ClassNotFoundException | NullPointerException e) {
            logger.info("JavaFX is not available on the current runtime environment:" +
                    "\njava.version=" +
                    System.getProperty("java.version") +
                    "\njava.vendor=" +
                    System.getProperty("java.vendor") +
                    "\njava.vm.name=" +
                    System.getProperty("java.vm.name") +
                    "\njava.vm.version=" +
                    System.getProperty("java.vm.version") +
                    "\njava.home=" +
                    System.getProperty("java.home"));
            logger.debug("JavaFX not available", e);
            return false;
        }
    }
}
