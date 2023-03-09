/*
 *    Copyright 2020-2023 Frederic Thevenet
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

import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.logging.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;

/**
 * Defines methods to help list entries in a file system, and obtain input streams on files it contains.
 * Roots for a {@link FileSystemBrowser} can be either folders in the default file system or zipfs compatible files
 * (typically .zip and .jar files).
 */
public class FileSystemBrowser implements Closeable {
    private static final Logger logger = Logger.create(FileSystemBrowser.class);
    private final Path fsRoot;
    private final FileSystem fs;

    /**
     * Initialize a new browser from a single root
     *
     * @param fsRoot the root path for the browser
     * @throws IOException if an error occurs while initializing the browser.
     */
    private FileSystemBrowser(Path fsRoot) throws IOException {
        this(Objects.requireNonNull(fsRoot).getFileSystem(), fsRoot);
    }

    /**
     * Initialize a new browser from a list of roots and a {@link FileSystem}
     * NB: All roots must belong to the same FileSystem
     *
     * @param fs     the FileSystem for the browser
     * @param fsRoot a list of root directories to browse
     * @throws IOException if an error occurs while initializing the browser.
     */
    private FileSystemBrowser(FileSystem fs, Path fsRoot) throws IOException {
        Objects.requireNonNull(fsRoot);
        this.fs = fs;
        this.fsRoot = fsRoot;
    }

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
        if (path.toString().toLowerCase(Locale.ROOT).endsWith(".zip") ||
                (path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))) {
            var fs = FileSystems.newFileSystem(path, (ClassLoader) null);
            for (var root : fs.getRootDirectories()) {
                // we only care about the first root in a zip file
                return new FileSystemBrowser(fs, root);
            }
        } else {
            // Return an instance that only lists the single file that was passed as root.
            return new FileSystemBrowser(path.getParent()){
                @Override
                public Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException {
                    return List.of(new FileSystemEntry(Files.isDirectory(path), path.getFileName(), Files.size(path)));
                }
            };
        }
        throw new UnsupportedOperationException("Unsupported operation: path is not a folder nor a zip file");
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
     * Returns an {@link InputStream} for the file system entry identified by the provided path.
     * <p>
     * <b>NOTE:</b> It is the caller's responsibility to close the returned stream when no longer needed.
     * </p>
     *
     * @param path the path of the file system entry to get a stream for.
     * @return an {@link InputStream} for the file system entry identified by the provided path.
     * @throws IOException If no entry could be identified in the underlying file system for the provided path.
     */
    public InputStream getData(String path) throws IOException {
        return Files.newInputStream(getRootDirectory().resolve(path), StandardOpenOption.READ);
    }

    /**
     * Returns a collection of {@link InputStream} for all file system entries matching the provided path predicate.
     * <p>
     * <b>NOTE:</b> It is the caller's responsibility to close the returned stream when no longer needed.
     * </p>
     *
     * @param filter a predicate that filters the file system entries to return a stream for.
     * @return a collection of {@link InputStream} for all file system entries matching the provided path predicate.
     * @throws IOException If no entry could be identified in the underlying file system for the provided path.
     */
    public Collection<InputStream> getData(Predicate<Path> filter) throws IOException {
        var streams = new ArrayList<InputStream>();
        Files.walkFileTree(fsRoot, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                var p = fsRoot.relativize(path);
                if (filter.test(p)) {
                    streams.add(Files.newInputStream(fsRoot.resolve(p), StandardOpenOption.READ));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.warn(() -> "An error occurred while accessing file " + file + ": " + exc.getMessage());
                logger.debug(() -> "Full stack", exc);
                return FileVisitResult.CONTINUE;
            }
        });
        return streams;
    }

    /**
     * Returns a collection of {@link FileSystemEntry} for all paths specified in the target file system
     *
     * @param paths list of paths to return a {@link FileSystemEntry} for.
     * @return a collection of {@link FileSystemEntry}.
     * @throws IOException If an error occurs while listing file system entries.
     */
    public Collection<FileSystemEntry> listEntries(Collection<Path> paths) throws IOException {
        return paths.stream()
                .map(fsRoot::resolve)
                .filter(Files::exists)
                .map(CheckedLambdas.wrap(p -> {
                    return new FileSystemEntry(Files.isDirectory(p), fsRoot.relativize(p), Files.size(fsRoot.resolve(p)));
                }))
                .sorted(FileSystemEntry::compareTo)
                .toList();
    }

    /**
     * * Returns a {@link FileSystemEntry}  instance for the path specified in the target file system
     *
     * @param path path to return a {@link FileSystemEntry} for.
     * @return a {@link FileSystemEntry}  instance for the path specified
     * @throws IOException If an error occurs while listing file system entries.
     */
    public FileSystemEntry getEntry(String path) throws IOException {
        var p = getRootDirectory().resolve(path);
        return new FileSystemEntry(Files.isDirectory(p), p, Files.size(p));
    }

    /**
     * Returns a collection of {@link FileSystemEntry} for all file system entries matching the provided path predicate.
     *
     * @param filter a predicate that filters the file system entries to return a {@link FileSystemEntry} for.
     * @return a collection of {@link FileSystemEntry} for all file system entries matching the provided path predicate.
     * @throws IOException If an error occurs while listing file system entries.
     */
    public Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException {
        var subEntries = new ArrayList<FileSystemEntry>();
        Files.walkFileTree(fsRoot, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                var p = fsRoot.relativize(path);
                if (filter.test(p)) {
                    subEntries.add(new FileSystemEntry(Files.isDirectory(p), p, Files.size(fsRoot.resolve(p))));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.warn(() -> "An error occurred while accessing file " + file + ": " + exc.getMessage());
                logger.debug(() -> "Full stack", exc);
                return FileVisitResult.CONTINUE;
            }
        });
        return subEntries.stream().sorted(FileSystemEntry::compareTo).toList();
    }

    /**
     * Returns a collection of {@link FileSystemEntry} for all file system entries matching the provided
     * folder and files glob patterns.
     *
     * @param folderFilters         a list of names of folders to inspect for content.
     * @param fileExtensionsFilters a list of file extensions patterns to inspect for content.
     * @return a collection of {@link FileSystemEntry} for all file system entries matching the provided path predicate.
     * @throws IOException If an error occurs while listing file system entries.
     */
    public Collection<FileSystemEntry> listEntries(String[] folderFilters, String[] fileExtensionsFilters) throws IOException {
        return listEntries(path -> path.getFileName() != null &&
                Arrays.stream(folderFilters)
                        .map(folder -> folder.equalsIgnoreCase("*") || path.startsWith(this.toInternalPath(folder)))
                        .reduce(Boolean::logicalOr).orElse(false) &&
                Arrays.stream(fileExtensionsFilters)
                        .map(f -> f.equalsIgnoreCase("*") ||
                                f.equalsIgnoreCase("*.*") ||
                                path.getFileName().toString().matches(("\\Q" + f + "\\E").replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q")))
                        .reduce(Boolean::logicalOr).orElse(false));
    }

    /**
     * Returns the file system root directory for this {@link FileSystemBrowser} instance
     *
     * @return the file system root directory for this {@link FileSystemBrowser} instance
     */
    public Path getRootDirectory() {
        return fsRoot;
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form a path string, to a {@link Path} valid
     * inside the browser's {@link FileSystem}
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
