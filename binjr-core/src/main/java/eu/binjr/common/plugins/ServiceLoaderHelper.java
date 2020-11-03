/*
 *    Copyright 2019-2020 Frederic Thevenet
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

package eu.binjr.common.plugins;

import eu.binjr.common.logging.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * A collection of helper methods to work with {@link ServiceLoader}
 *
 * @author Frederic Thevenet
 */
public final class ServiceLoaderHelper {
    private static final Logger logger = Logger.create(ServiceLoaderHelper.class);

    /**
     * A helper method to load and return service implementations from the classpath and external jars
     *
     * @param clazz the type of service to load and return
     * @param <T>   the type of service to load and return
     * @return A {@link Set} of loaded service implementations
     */
    public static <T> Set<T> loadFromClasspath(Class<T> clazz) {
        Set<T> loadServices = new HashSet<>();
        // Load plugins from classpath
        loadFromServiceLoader(ServiceLoader.load(clazz), loadServices);
        return loadServices;
    }

    /**
     * A helper method to load and return service implementations from the classpath and/or external jars
     *
     * @param clazz             the type of service to load and return
     * @param externalLocations file system paths specifying where to look for external jar to load services from
     * @param <T>               the type of service to load and return
     * @return A {@link Set} of loaded service implementations
     */
    public static <T> Set<T> loadFromPaths(Class<T> clazz, Path... externalLocations) {
        Objects.requireNonNull(externalLocations);
        Set<T> loadServices = new HashSet<>();
        //Load plugin from external folder
        List<URL> urls = new ArrayList<>();
        for (var externalLocation : externalLocations) {
            if (externalLocation != null && Files.exists(externalLocation)) {
                logger.info(() -> "Looking for services of type " + clazz.getName() + " in " + externalLocation);
                PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");
                try {
                    Files.walkFileTree(externalLocation,
                            EnumSet.noneOf(FileVisitOption.class),
                            1,
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    if (jarMatcher.matches(file)) {
                                        logger.debug(() -> "Inspecting " + file.getFileName() +
                                                " for " + clazz.getName() + " service implementations");
                                        urls.add(file.toUri().toURL());
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                } catch (IOException e) {
                    logger.error("Error while scanning for services: " + e.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Stack trace", e);
                    }
                }
            } else {
                logger.warn("External location " + externalLocation + " does not exist.");
            }
        }
        loadFromServiceLoader(ServiceLoader.load(clazz, new URLClassLoader(urls.toArray(URL[]::new))), loadServices);
        return loadServices;
    }

    private static <T> void loadFromServiceLoader(ServiceLoader<T> sl, Set<T> registeredResources) {
        for (Iterator<T> iterator = sl.iterator(); iterator.hasNext(); ) {
            try {
                T res = iterator.next();
                registeredResources.add(res);
                logger.debug(() -> "Successfully registered resource " + res.toString() + " from external JAR.");
            } catch (ServiceConfigurationError sce) {
                logger.error("Failed to load resource", sce);
            } catch (Exception e) {
                logger.error("Unexpected error while loading resource", e);
            }
        }
    }

}
