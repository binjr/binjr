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

package eu.binjr.common.io;

import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.logging.Logger;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Defines methods to help list entries in a file system, and obtain input streams on files it contains.
 * Roots for a {@link FileSystemBrowser} can be either folders in the default file system or zipfs compatible files
 * (typically .zip and .jar files).
 */
public class FileSystemBrowser implements Closeable {
    private static final Logger logger = Logger.create(FileSystemBrowser.class);
    private final List<Path> fsRoots;
    private final FileSystem fs;

    /**
     * Return a new {@link FileSystemBrowser} instance for the provided path.
     * Valid path can be either a folder of the default file system or a zip.jar file.
     *
     * @param path the path to browse.
     * @return a new {@link FileSystemBrowser} instance for the provided  path
     * @throws IOException if an error occurs while initializing the browser.
     */
    public static FileSystemBrowser of(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return new FileSystemBrowser(path);
        }
        List<Path> roots = new ArrayList<>();
        var fs = FileSystems.newFileSystem(path, (ClassLoader) null);
        fs.getRootDirectories()
                .iterator()
                .forEachRemaining(roots::add);

        return new FileSystemBrowser(fs, roots);
    }

    /**
     * Return a new {@link FileSystemBrowser} instance for the provided URI.
     * Valid URI can point to a zip or a jar file.
     *
     * @param uri the URI to browse.
     * @return a new {@link FileSystemBrowser} instance for the provided URI
     * @throws IOException if an error occurs while initializing the browser.
     */
    public static FileSystemBrowser of(URI uri) throws IOException {
        List<Path> roots = new ArrayList<>();
        var fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        fs.getRootDirectories().iterator().forEachRemaining(roots::add);
        return new FileSystemBrowser(fs, roots);
    }

    /**
     * Return a new {@link FileSystemBrowser} instance for the provided URI and a root path inside the target file system
     * Valid URI can point to a zip or a jar file.
     *
     * @param uri   the URI to browse the fs to browse
     * @param first the path string or initial part of the path string to browse
     * @param more  additional strings to be joined to form the path string to browse
     * @return a new {@link FileSystemBrowser} instance
     * @throws IOException if an error occurs while initializing the browser.
     */
    public static FileSystemBrowser of(URI uri, String first, String... more) throws IOException {
        return new FileSystemBrowser(FileSystems.newFileSystem(uri, Collections.emptyMap()).getPath(first, more));
    }

    /**
     * Initialize a new browser from a single root
     *
     * @param fsRoot the root path for the browser
     * @throws IOException if an error occurs while initializing the browser.
     */
    private FileSystemBrowser(Path fsRoot) throws IOException {
        this.fs = fsRoot.getFileSystem();
        this.fsRoots = List.of(fsRoot);
    }

    /**
     * Initialize a new browser from a list of roots and a {@link FileSystem}
     * NB: All roots must belong to the same FileSystem
     *
     * @param fs      the FileSystem for the browser
     * @param fsRoots a list of root directories to browse
     * @throws IOException if an error occurs while initializing the browser.
     */
    private FileSystemBrowser(FileSystem fs, List<Path> fsRoots) throws IOException {
        if (fsRoots.isEmpty()) {
            throw new IOException("Cannot create a FileSystemBrowser with no root");
        }
        this.fs = fs;
        this.fsRoots = fsRoots;
    }

    /**
     * Returns an {@link InputStream} for the file system entry identified by the provided path.
     *
     * @param path the path of the file system entry to get a stream for.
     * @return an {@link InputStream} for the file system entry identified by the provided path.
     * @throws IOException If no entry could be identified in the underlying file system for the provided path.
     * @apiNote It is the caller's responsibility to close the returned stream when no longer needed.
     */
    public InputStream getData(String path) throws IOException {
        return getData(p -> p.equals(toInternalPath(path)))
                .stream()
                .findAny()
                .orElseThrow(CheckedLambdas.wrap(() -> new FileNotFoundException("Could not find file system entry " + path)));
    }

    /**
     * Returns a collection of {@link InputStream} for all file system entries matching the provided path predicate.
     *
     * @param filter a predicate that filters the file system entries to return a stream for.
     * @return a collection of {@link InputStream} for all file system entries matching the provided path predicate.
     * @throws IOException If no entry could be identified in the underlying file system for the provided path.
     * @apiNote It is the caller's responsibility to close the returned streams when no longer needed.
     */
    public Collection<InputStream> getData(Predicate<Path> filter) throws IOException {
        return getRootDirectories().stream().flatMap(CheckedLambdas.wrap(root -> {
            return Files.walk(root).map(root::relativize)
                    .filter(filter)
                    .map(CheckedLambdas.wrap((CheckedFunction<Path, InputStream, IOException>)
                            path -> Files.newInputStream(root.resolve(path), StandardOpenOption.READ)));
        })).collect(Collectors.toList());
    }

    /**
     * Returns a collection of {@link FileSystemEntry} for all file system entries matching the provided path predicate.
     *
     * @param filter a predicate that filters the file system entries to return a {@link FileSystemEntry} for.
     * @return a collection of {@link FileSystemEntry} for all file system entries matching the provided path predicate.gradle
     * @throws IOException If an error occurs while listing file system entries.
     */
    public Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException {
        return getRootDirectories().stream().flatMap(CheckedLambdas.wrap(root -> {
            return Files.walk(root) .map(root::relativize)
                    .filter(filter)
                    .map(CheckedLambdas.wrap(p -> {
                        return new FileSystemEntry(Files.isDirectory(p), p, Files.size(root.resolve(p)));
                    })).sorted(FileSystemEntry::compareTo);
        })).collect(Collectors.toList());
    }

    /**
     * Returns the file system root directories for this {@link FileSystemBrowser} instance
     *
     * @return the file system root directories for this {@link FileSystemBrowser} instance
     */
    public List<Path> getRootDirectories() {
        return fsRoots;
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form a path string, to a {@link Path} valid
     * inside of the browser's {@link FileSystem}
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting {@code Path}
     * @throws InvalidPathException If the path string cannot be converted
     */
    public Path toInternalPath(String first, String... more) {
        return this.fs.getPath(first, more);
    }

    @Override
    public void close() throws IOException {
        if (!this.fs.equals(FileSystems.getDefault())) {
            this.fs.close();
        }
    }

    /**
     * A data class to carry selected properties describing file system entries browsed by {@link FileSystemBrowser}
     */
    public static class FileSystemEntry implements Comparable<FileSystemEntry> {
        private final boolean directory;
        private final Path path;
        private final Long size;

        public FileSystemEntry(boolean directory, Path path, Long size) {
            this.directory = directory;
            this.path = path;
            this.size = size;
        }

        public boolean isDirectory() {
            return directory;
        }

        public Path getPath() {
            return path;
        }

        public Long getSize() {
            return size;
        }

        @Override
        public int compareTo(FileSystemEntry o) {
            var dirComp = Boolean.compare(this.directory, o.directory);
            if (dirComp != 0) {
                return dirComp;
            }

            var rootComp = Boolean.compare(isRoot(this.path), isRoot(o.path));
            if (rootComp != 0) {
                return rootComp;
            }

            var pathComp = this.path.toString().compareToIgnoreCase(o.path.toString());
            if (pathComp != 0) {
                return pathComp;
            }
            var depthComp = pathDepthFromRoot(o.path) - pathDepthFromRoot(this.path);
            if (depthComp != 0) {
                return depthComp;
            }
            return Long.compare(this.size, o.size);
        }

        private boolean isRoot(Path path) {
            return path.getParent() == null || path.getParent().toString().equals("/");
        }

        private int pathDepthFromRoot(Path path) {
            int depth = 0;
            Path parent = path.getParent();
            while (parent != null) {
                depth++;
                parent = parent.getParent();
            }
            return depth;
        }

        @Override
        public int hashCode() {
            return path.hashCode() + Long.hashCode(size) + Boolean.hashCode(directory);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FileSystemEntry fse = (FileSystemEntry) obj;
            return path.equals(fse.path) &&
                    size.equals(fse.size) &&
                    directory == fse.directory;
        }
    }
}
