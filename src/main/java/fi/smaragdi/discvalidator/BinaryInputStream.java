package fi.smaragdi.discvalidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;

public class BinaryInputStream implements AutoCloseable {
    private InputStream input;
    private final ByteOrder byteOrder;

    public BinaryInputStream(InputStream input, ByteOrder byteOrder) {
        this.input = input;
        this.byteOrder = byteOrder;
    }

    public byte[] decompress(byte[] gzipped) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            return inputStream.readAllBytes();
        }
    }

    public byte[] getHash() throws IOException {
        return readBytes(16);
    }

    public String readFixedLengthString(int len) throws IOException {
        return new String(readBytes(len), StandardCharsets.UTF_8);
    }

    public static int check(int value) throws EOFException {
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    public String readNullTerminatedString() throws IOException {
        try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream()) {
            while (true) {
                byte b = readByte();
                if (b == '\0') {
                    return new String(byteArray.toByteArray(), StandardCharsets.UTF_8);
                }
                byteArray.write(b);
            }
        }
    }

    public String readString() throws IOException {
        byte size = readByte();
        return new String(readBytes(size), StandardCharsets.UTF_8);
    }

    public int readInt() throws IOException {
        byte[] bytes = readBytes(4);
        return ByteBuffer.wrap(bytes).order(byteOrder).getInt();
    }

    public short readShort() throws IOException {
        byte[] bytes = readBytes(2);
        return ByteBuffer.wrap(bytes).order(byteOrder).getShort();
    }

    public byte readByte() throws IOException {
        int check = check(input.read());
        return (byte) check;
    }

    public long readLong() throws IOException {
        byte[] bytes = readBytes(8);
        return ByteBuffer.wrap(bytes).order(byteOrder).getLong();
    }

    public byte[] readBytes(int size) throws IOException {
        int current = 0;
        byte[] bytes = new byte[size];
        do {
            int read = input.read(bytes, current, size - current);
            if (read < 0) {
                throw new EOFException();
            }
            current += read;
        } while (current < size);
        return bytes;
    }

    public static class CheckedBinaryInputStream extends BinaryInputStream {
        public CheckedBinaryInputStream(CheckedInputStream input, ByteOrder byteOrder) {
            super(input, byteOrder);
        }

        public long getChecksum() {
            return ((CheckedInputStream) getUnderlyingStream()).getChecksum().getValue();
        }
    }

    public static class SeekableBinaryInputStream extends BinaryInputStream {

        public SeekableBinaryInputStream(FileInputStream inputStream, ByteOrder byteOrder) {
            super(inputStream, byteOrder);
        }

        public void seek(long position) throws IOException {
            ((FileInputStream) getUnderlyingStream()).getChannel().position(position);
        }

        public long getPosition() throws IOException {
            return ((FileInputStream) getUnderlyingStream()).getChannel().position();
        }
    }

    /*package-private*/ InputStream getUnderlyingStream() {
        return input;
    }

    public void skip(long bytes) throws IOException {
        long skip = input.skip(bytes);
        if (skip >= bytes) {
            throw new EOFException("Tried to seek " + bytes + ", ended up skipping " + skip + " instead");
        }
    }

    @Override
    public void close() throws IOException {
        this.input.close();
        this.input = null;
    }
}
