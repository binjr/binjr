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
    private final Path cvdiagPath;

    public static FileSystemBrowser of(Path cvdiagPath) throws IOException {
        if (Files.isDirectory(cvdiagPath)) {
            return new FolderBrowser(cvdiagPath);
        } else {
            return new ZipBrowser(cvdiagPath);
        }
    }

    protected FileSystemBrowser(Path cvdiagPath) throws IOException {
        this.cvdiagPath = cvdiagPath;
    }

    public InputStream getData(String path) throws IOException {
        return getData(p -> p.equals(toPath(path)))
                .stream()
                .findAny()
                .orElseThrow(CheckedLambdas.wrap(() -> new FileNotFoundException("Could not find file system entry " + path)));
    }

    public abstract Collection<InputStream> getData(Predicate<Path> filter) throws IOException;

    public abstract Collection<Path> listEntries(Predicate<Path> filter) throws IOException;

    public Path getPath() {
        return cvdiagPath;
    }

    public abstract Path toPath(String path);


    private static class FolderBrowser extends FileSystemBrowser {
        public FolderBrowser(Path cvdiagPath) throws IOException {
            super(cvdiagPath);
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
        public Collection<Path> listEntries(Predicate<Path> filter) throws IOException {
            try (Stream<Path> paths = Files.walk(getPath())) {
                return paths.map(p -> getPath().relativize(p))
                        .filter(filter)
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

        public ZipBrowser(Path cvdiagPath) throws IOException {
            super(cvdiagPath);
            this.zipFile = new ZipFile(cvdiagPath.toFile());
            this.zipRootPath = FileSystems.newFileSystem(cvdiagPath, (ClassLoader) null).getRootDirectories().iterator().next();
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
        public Collection<Path> listEntries(Predicate<Path> filter) throws IOException {
            List<Path> paths = new ArrayList<>();
            try (Profiler ignored = Profiler.start("Listing path from zip " + getPath().getFileName(), logger::perf)) {
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry zipEntry = entries.nextElement();
                    if (!zipEntry.isDirectory()) {
                        Path entryPath = zipRootPath.resolve(zipEntry.getName());
                        if (filter.test(entryPath)) {
                            paths.add(entryPath);
                        }
                    }
                }
                return paths;
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
}
