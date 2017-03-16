package eu.fthevenet.binjr.io;

import java.io.*;
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

//    public static String readToString(InputStream in) throws IOException{
//        BufferedReader reader = new BufferedReader( new InputStreamReader(in));
//        String inputLine;
//        StringBuilder response = new StringBuilder();
//        while ((inputLine = reader.readLine()) != null) {
//            response.append(inputLine);
//        }
//        return response.toString();
//    }

    public static long consumeStream(InputStream input) throws IOException{
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
