/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Utility methods to read, write and copy data from and across streams
 *
 * @author Frederic Thevenet
 */
public class IOUtils {
    private static final int DEFAULT_COPY_BUFFER_SIZE = 32 * 1024;
    private static final int EOF = -1;
    private static final Logger logger = Logger.create(IOUtils.class);

    public static long copyChannels(ReadableByteChannel input, WritableByteChannel output) throws IOException {
        return copyChannels(input, output, DEFAULT_COPY_BUFFER_SIZE);
    }

    public static long copyChannels(ReadableByteChannel input, WritableByteChannel output, int bufferSize) throws IOException {
        Objects.requireNonNull(input, "Argument input must not be null");
        Objects.requireNonNull(output, "Argument output must not be null");
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        long count = 0;
        while (input.read(buffer) != -1) {
            buffer.flip();
            count += output.write(buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            count += output.write(buffer);
        }
        return count;
    }

    public static long copyStreams(InputStream input, OutputStream output) throws IOException {
        return copyStreams(input, output, DEFAULT_COPY_BUFFER_SIZE);
    }

    public static long copyStreams(InputStream input, OutputStream output, int bufferSize) throws IOException {
        Objects.requireNonNull(input, "Argument input must not be null");
        Objects.requireNonNull(output, "Argument output must not be null");
        byte[] buffer = new byte[bufferSize];
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static byte[] readToBuffer(InputStream input) throws IOException {
        Objects.requireNonNull(input, "Argument input must not be null");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            long count = copyStreams(input, baos);
            return baos.toByteArray();
        }
    }

    public static long consumeStream(InputStream input) throws IOException {
        Objects.requireNonNull(input, "Argument input must not be null");
        byte[] buffer = new byte[DEFAULT_COPY_BUFFER_SIZE];
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            count += n;
        }
        return count;
    }

    public static String readToString(InputStream in) throws IOException {
        return new String(readToBuffer(in));
    }

    public static String readToString(InputStream in, String charset) throws IOException {
        return new String(readToBuffer(in), charset);
    }

    public static <T extends AutoCloseable> void closeAll(Collection<T> collection) {
        closeAll(collection.stream());
    }

    public static <T extends AutoCloseable> void closeAll(Collection<T> collection, BiConsumer<T, Exception> onError) {
        closeAll(collection.stream(), onError);
    }

    public static <T extends AutoCloseable> void closeAll(Stream<T> stream) {
        closeAll(stream, (c, e) -> {
            logger.error("An error occurred while closing element" + c + ": " + e.getMessage());
            logger.debug(e);
        });
    }

    public static <T extends AutoCloseable> void closeAll(Stream<T> stream, BiConsumer<T, Exception> onError) {
        Objects.requireNonNull(stream, "Argument collection must not be null");
        stream.forEach(closeable -> close(closeable, onError));
    }

    public static  <T extends AutoCloseable> void close(T closeable){
        close(closeable, (t, e) -> {
            logger.error("An error occurred while closing " + t + ": " + e.getMessage());
            logger.debug(e);
        });
    }

    public static  <T extends AutoCloseable> void close(T closeable, BiConsumer<T, Exception> onError) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                onError.accept(closeable, e);
            }
        }
    }

}
