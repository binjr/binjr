package eu.binjr.common.io;


import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class FileSystemBrowser implements Closeable {
    private static Logger logger = Logger.create(FileSystemBrowser.class);
    private final Path filePath;

    public static FileSystemBrowser of(Path filePath) throws IOException {
        if (Files.isDirectory(filePath)) {
            return new FolderBrowser(filePath);
        } else {
            return new ZipBrowser(filePath);
        }
    }

    protected FileSystemBrowser(Path filePath) throws IOException {
        this.filePath = filePath;
    }

    public InputStream getData(String path) throws IOException {
        return getData(p -> p.equals(toPath(path)))
                .stream()
                .findAny()
                .orElseThrow(CheckedLambdas.wrap(() -> new FileNotFoundException("Could not find file system entry " + path)));
    }

    public abstract Collection<InputStream> getData(Predicate<Path> filter) throws IOException;

    public abstract Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException;

    public Path getPath() {
        return filePath;
    }

    public abstract Path toPath(String path);

    private static class FolderBrowser extends FileSystemBrowser {
        public FolderBrowser(Path filePath) throws IOException {
            super(filePath);
        }

        @Override
        public Collection<InputStream> getData(Predicate<Path> filter) throws IOException {
            try (Stream<Path> paths = Files.walk(getPath())) {
                return paths.map(p -> getPath().relativize(p))
                        .filter(filter)
                        .map(CheckedLambdas.wrap((CheckedFunction<Path, InputStream, IOException>)
                                path -> Files.newInputStream(getPath().resolve(path), StandardOpenOption.READ)))
                        .collect(Collectors.toList());
            }
        }

        @Override
        public Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException {
            try (Stream<Path> paths = Files.walk(getPath())) {
                return paths.map(p -> getPath().relativize(p))
                        .filter(filter)
                        .map(CheckedLambdas.wrap(p -> {
                            return new FileSystemEntry(Files.isDirectory(p), p, Files.size(getPath().resolve(p)));
                        }))
                        .sorted(FileSystemEntry::compareTo)
                        .collect(Collectors.toList());
            }
        }

        @Override
        public Path toPath(String path) {
            return Path.of(path);
        }


        @Override
        public void close() throws IOException {

        }
    }

    private static class ZipBrowser extends FileSystemBrowser {
        private final ZipFile zipFile;
        private final Path zipRootPath;

        public ZipBrowser(Path filePath) throws IOException {
            super(filePath);
            this.zipFile = new ZipFile(filePath.toFile());
            this.zipRootPath = FileSystems.newFileSystem(filePath, (ClassLoader) null).getRootDirectories().iterator().next();
        }

        @Override
        public Collection<InputStream> getData(Predicate<Path> filter) throws IOException {
            List<InputStream> inputStreams = new ArrayList<>();
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory() && filter.test(zipRootPath.resolve(zipEntry.getName()))) {
                    inputStreams.add(zipFile.getInputStream(zipEntry));
                }
            }
            return inputStreams;
        }

        @Override
        public Collection<FileSystemEntry> listEntries(Predicate<Path> filter) throws IOException {
            List<FileSystemEntry> fsEntries = new ArrayList<>();
            try (Profiler ignored = Profiler.start("Listing path from zip " + getPath().getFileName(), logger::perf)) {
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry zipEntry = entries.nextElement();
                    Path entryPath = zipRootPath.resolve(zipEntry.getName());
                    if (filter.test(entryPath)) {
                        fsEntries.add(new FileSystemEntry(zipEntry.isDirectory(), entryPath, zipEntry.getSize()));
                    }
                }
                fsEntries.sort(FileSystemEntry::compareTo);
                return fsEntries;
            }
        }

        @Override
        public Path toPath(String path) {
            return zipRootPath.resolve(path);
        }

        @Override
        public void close() throws IOException {
            if (this.zipFile != null) {
                this.zipFile.close();
            }
        }
    }

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
                    size == fse.size &&
                    directory == fse.directory;
        }
    }
}
