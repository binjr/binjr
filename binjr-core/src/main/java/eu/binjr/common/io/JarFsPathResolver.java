/*
 * Copyright 2025 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.io;

import eu.binjr.common.logging.Logger;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JarFsPathResolver {
    private static final Logger logger = Logger.create(JarFsPathResolver.class);
    private static Path root;

    static {
        Path jarPath = null;
        try {
            URI uri = JarFsPathResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            jarPath = Paths.get(uri);
            root = FileSystems.newFileSystem(jarPath, (ClassLoader) null).getRootDirectories().iterator().next();
        } catch (Exception e) {
            logger.warn("Failed to create FileSystem from " + (jarPath != null ? jarPath : "null") + "; default file system will be used as an anchor to resolve path.");
            logger.debug(() -> "Exception stack", e);
            root = FileSystems.getDefault().getRootDirectories().iterator().next();
        }
        logger.debug(() -> String.format("Path provider: [%s] [%s]", root.getFileSystem().provider().getScheme(), root.getFileSystem().toString()));
    }

    public static Path get(String path) {
        return root.resolve(path);
    }
}
