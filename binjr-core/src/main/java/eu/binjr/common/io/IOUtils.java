/*
 *    Copyright 2017-2018 Frederic Thevenet
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Utility methods to read, write and copy data from and across streams
 *
 * @author Frederic Thevenet
 */
public class IOUtils {
    private static final int DEFAULT_COPY_BUFFER_SIZE = 32 * 1024;
    private static final int EOF = -1;

    public static long copyChannels(ReadableByteChannel input, WritableByteChannel output) throws IOException {
        return copyChannels(input, output, DEFAULT_COPY_BUFFER_SIZE);
    }

    public static long copyChannels(ReadableByteChannel input, WritableByteChannel output, int bufferSize) throws IOException {
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
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            long count = copyStreams(input, baos);
            return baos.toByteArray();
        }
    }

    public static long consumeStream(InputStream input) throws IOException {
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
}
