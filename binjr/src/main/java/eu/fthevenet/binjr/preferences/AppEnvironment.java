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

package eu.fthevenet.binjr.preferences;

import eu.fthevenet.binjr.controllers.ConsoleStage;
import eu.fthevenet.util.version.Version;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Provides access to the application's environmental properties
 */
public class AppEnvironment {
    public static final String HTTP_GITHUB_REPO = "https://github.com/fthevenet/binjr";
    public static final String HTTP_WWW_BINJR_EU = "http://www.binjr.eu";
    public static final String HTTP_BINJR_WIKI = "https://github.com/fthevenet/binjr/wiki";
    public static final String COPYRIGHT_NOTICE = "Copyright (c) 2017-2018 Frederic Thevenet";
    public static final String LICENSE = "Apache-2.0";

    private final Level configuredRootLevel = LogManager.getRootLogger().getLevel();
    private final BooleanProperty debugMode = new SimpleBooleanProperty();
    private final BooleanProperty consoleVisible = new SimpleBooleanProperty();
    private final Property<Level> logLevel = new SimpleObjectProperty<>();

    private static final Logger logger = LogManager.getLogger(AppEnvironment.class);
    private final Manifest manifest;
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    private static class EnvironmentHolder {
        private final static AppEnvironment instance = new AppEnvironment();
    }

    private AppEnvironment() {
        this.manifest = getManifest();
        consoleVisible.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ConsoleStage.show();
            }
            else {
                ConsoleStage.hide();
            }
        });

        logLevel.setValue(LogManager.getRootLogger().getLevel());
        logLevel.addListener((observable, oldLevel, newLevel) -> {
            Configurator.setRootLevel(newLevel);
            logger.info("Root logger level set to " + newLevel);
        });

        debugMode.addListener((observable, oldValue, newValue) -> {
            Level newLevel = configuredRootLevel;
            if (newValue) {
                if (newLevel.compareTo(Level.DEBUG) < 0) {
                    newLevel = Level.DEBUG;
                }
            }
            logLevel.setValue(newLevel);
            consoleVisible.setValue(newValue);
        });
        debugMode.setValue(LogManager.getRootLogger().isDebugEnabled());
    }

    /**
     * Get the singleton instance for the {@link UpdateManager} class.
     *
     * @return the singleton instance for the {@link UpdateManager} class.
     */
    public static AppEnvironment getInstance() {
        return EnvironmentHolder.instance;
    }

    /**
     * Returns the version information held in the containing jar's manifest
     *
     * @return the version information held in the containing jar's manifest
     */
    public Version getVersion() {
        return getVersion(this.manifest);
    }

    /**
     * Returns a version number extracted from the specified manifest.
     * <p>The version number is extracted according to the following rules:
     * <ul>
     * <li>If the key {@code Specification-Version} is present and can be formatted as valid {@link Version} instance then it is returned, else</li>
     * <li>If the key {@code Implementation-Version} is present and can be formatted as valid {@link Version} instance then it is returned, else</li>
     * <li> {@code Version.emptyVersion} is returned</li>
     * </ul>
     * </p>
     *
     * @param manifest the {@link Manifest} from which a version number should be extracted.
     * @return a version number extracted from the specified manifest.
     */
    public Version getVersion(Manifest manifest) {
        if (manifest != null) {
            String[] keys = new String[]{"Specification-Version", "Implementation-Version"};
            for (String key : keys) {
                String value = manifest.getMainAttributes().getValue(key);
                if (value != null) {
                    try {
                        return new Version(value);
                    } catch (IllegalArgumentException e) {
                        logger.error("Could not decode version number: " + value + ": " + e.getMessage());
                        logger.debug(() -> "Full stack", e);
                    }
                }
            }
        }
        return Version.emptyVersion;
    }

    public Version getVersion(Class aClass) {
        return this.getVersion(this.getManifest(aClass));
    }

    public Manifest getManifest() {
        return getManifest(this.getClass());
    }

    /**
     * Returns the manifest for the JAR which packages the specified class.
     *
     * @param aClass
     * @return the manifest for the JAR which packages the specified class.
     */
    public Manifest getManifest(Class<?> aClass) {
        String className = aClass.getSimpleName() + ".class";
        String classPath = aClass.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) + "/META-INF/MANIFEST.MF";
            try {
                return new Manifest(new URL(manifestPath).openStream());
            } catch (IOException e) {
                logger.error("Error extracting manifest from jar: " + e.getMessage());
                logger.debug(() -> "Full stack", e);
            }
        }
        logger.warn("Could not extract MANIFEST from jar!");
        return null;
    }

    /**
     * Returns a short description for the application.
     *
     * @return a short description for the application.
     */
    public String getAppDescription() {
        return "binjr v" + getVersion() + " (build #" + getBuildNumber() + ")";
    }

    /**
     * Returns the build number from the manifest
     *
     * @return the build number from the manifest
     */
    public Long getBuildNumber() {
        if (manifest != null) {
            String value = manifest.getMainAttributes().getValue("Build-Number");
            if (value != null) {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException e) {
                    logger.error("Could not decode build number: " + value + ": " + e.getMessage());
                    logger.debug(() -> "Full stack", e);
                }
            }
        }
        return 0L;
    }

    /**
     * Returns a list of system properties
     *
     * @return a list of system properties
     */
    public List<SysInfoProperty> getSysInfoProperties() {
        Runtime rt = Runtime.getRuntime();
        double usedMB = ((double) rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        double percentUsage = (((double) rt.totalMemory() - rt.freeMemory()) / rt.totalMemory()) * 100;

        List<SysInfoProperty> sysInfo = new ArrayList<>();
        sysInfo.add(new SysInfoProperty("Version", getVersion().toString() + " (build #" + getBuildNumber().toString() + ")"));
        sysInfo.add(new SysInfoProperty("Java Version", System.getProperty("java.version")));
        sysInfo.add(new SysInfoProperty("Java Vendor", System.getProperty("java.vendor")));
        sysInfo.add(new SysInfoProperty("Java VM name", System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")"));
        sysInfo.add(new SysInfoProperty("Java Home", System.getProperty("java.home")));
        sysInfo.add(new SysInfoProperty("Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")"));
        sysInfo.add(new SysInfoProperty("System Architecture", System.getProperty("os.arch")));
        sysInfo.add(new SysInfoProperty("JVM Heap Max size", String.format("%.0f MB", (double) rt.maxMemory() / 1024 / 1024)));
        sysInfo.add(new SysInfoProperty("JVM Heap Usage", String.format("%.2f%% (%.0f/%.0f MB)", percentUsage, usedMB, (double) rt.totalMemory() / 1024 / 1024)));
        return sysInfo;
    }

    /**
     * Returns the family of the currently running OS
     *
     * @return the family of the currently running OS
     */
    public OsFamily getOsFamily() {
        if (OS_NAME.startsWith("windows")) {
            return OsFamily.WINDOWS;
        }
        if (OS_NAME.startsWith("mac")) {
            return OsFamily.OSX;
        }
        if (OS_NAME.startsWith("linux")) {
            return OsFamily.LINUX;
        }
        else {
            return OsFamily.UNSUPPORTED;
        }
    }

    public void setDebugMode(boolean value) {
        debugMode.setValue(value);
    }

    public boolean isDebugMode() {
        return debugMode.get();
    }

    public BooleanProperty debugModeProperty() {
        return debugMode;
    }

    public boolean isConsoleVisible() {
        return consoleVisible.get();
    }

    public BooleanProperty consoleVisibleProperty() {
        return consoleVisible;
    }

    public void setConsoleVisible(boolean consoleVisible) {
        this.consoleVisible.set(consoleVisible);
    }

    public Level getLogLevel() {
        return logLevel.getValue();
    }

    public Property<Level> logLevelProperty() {
        return logLevel;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel.setValue(logLevel);
    }
}
