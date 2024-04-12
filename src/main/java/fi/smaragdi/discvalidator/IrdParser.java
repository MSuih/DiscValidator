package fi.smaragdi.discvalidator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

class IrdParser {
    private final Path irdFile;
    private InputStream input;

    public IrdParser(Path file) {
        this.irdFile = file;
    }

    private static final byte[] IRD_MAGIC = "3IRD".getBytes(StandardCharsets.UTF_8);

    public synchronized Ird parse() throws IOException {
        try (CheckedInputStream input = new CheckedInputStream(open(irdFile), new CRC32())) {
            this.input = input;

            byte[] magic = readBytes(4);
            if (Arrays.mismatch(IRD_MAGIC, magic) != -1) {
                HexFormat hexFormat = HexFormat.of().withPrefix("0x");
                throw new IllegalArgumentException(STR."Not valid ird file, magic \{hexFormat.formatHex(magic)}");
            }

            byte version = readByte();
            /*if (version < 6 || version > 9) {
                throw new IllegalArgumentException("Version " + version + " is not supported");
            }*/

            String serial = readFixedLengthString(9);
            String title = readString();

            String fwVersion = readFixedLengthString(4);
            String mainVersion = readFixedLengthString(5);
            String appVersion = readFixedLengthString(5);

            if (version == 7) {
                // File ID, discard it
                readInt();
            }

            int headerSize = readInt();
            byte[] header = decompress(readBytes(headerSize));
            int footerSize = readInt();
            byte[] footer = decompress(readBytes(footerSize));

            byte regions = readByte();
            byte[][] regionHashes = new byte[regions][];
            for (int region = 0; region < regions; region++) {
                byte[] hash = getHash();
                regionHashes[region] = hash;
            }

            int files = readInt();
            Ird.FileChecksum[] fileHashes = new Ird.FileChecksum[files];
            for (int file = 0; file < files; file++) {
                long sector = readLong();
                byte[] hash = getHash();
                fileHashes[file] = new Ird.FileChecksum(sector, hash);
            }

            int padding = readInt();
            if (padding != 0) {
                throw new IllegalArgumentException(STR."Padding contained \{padding}");
            }
            byte[] pic = null;
            if (version == 9) {
                pic = readBytes(115);
            }
            byte[] data1 = readBytes(16);
            byte[] data2 = readBytes(16);
            if (version < 9) {
                pic = readBytes(115);
            }
            int uid = readInt();

            // check crc
            long crc = input.getChecksum().getValue();
            long expected = Integer.toUnsignedLong(readInt());
            if (crc != expected) {
                throw new IllegalArgumentException("CRC is invalid! Expected %s, actual %s".formatted(expected, crc));
            }

            return new Ird(serial, title, fwVersion, mainVersion, appVersion, header, footer, regionHashes, fileHashes, pic, data1, data2, uid);
        }
    }

    private static InputStream open(Path file) throws IOException {
        try {
            return new BufferedInputStream(new GZIPInputStream(new FileInputStream(file.toFile())));
        } catch (ZipException e) {
            // Not compressed, try to read as uncompressed data
            return new BufferedInputStream(new FileInputStream(file.toFile()));
        }
    }

    private byte[] decompress(byte[] gzipped) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            return inputStream.readAllBytes();
        }
    }

    private byte[] getHash() throws IOException {
        return readBytes(16);
    }

    private String readFixedLengthString(int len) throws IOException {
        return new String(readBytes(len), StandardCharsets.UTF_8);
    }

    private static void check(int size, int expected) throws EOFException {
        check(size);
        if (size != expected) {
            throw new EOFException();
        }
    }

    private static int check(int value) throws EOFException {
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    private String readString() throws IOException {
        byte size = readByte();
        return new String(readBytes(size), StandardCharsets.UTF_8);
    }

    private int readInt() throws IOException {
        byte[] bytes = readBytes(4);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private byte readByte() throws IOException {
        int check = check(input.read());
        return (byte) check;
    }

    private long readLong() throws IOException {
        byte[] bytes = readBytes(8);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private byte[] readBytes(int count) throws IOException {
        byte[] bytes = new byte[count];
        check(input.read(bytes), count);
        return bytes;
    }
}
