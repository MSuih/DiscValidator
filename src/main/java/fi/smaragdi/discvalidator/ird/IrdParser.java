package fi.smaragdi.discvalidator.ird;

import fi.smaragdi.discvalidator.BinaryInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

public class IrdParser {
    private IrdParser() {
        throw new AssertionError();
    }
    private static final byte[] IRD_MAGIC = "3IRD".getBytes(StandardCharsets.UTF_8);

    public static synchronized Ird parse(Path irdFile) throws IOException {
        try (BinaryInputStream.CheckedBinaryInputStream input = new BinaryInputStream.CheckedBinaryInputStream(new CheckedInputStream(open(irdFile), new CRC32()), ByteOrder.LITTLE_ENDIAN)) {

            byte[] magic = input.readBytes(4);
            if (Arrays.mismatch(IRD_MAGIC, magic) != -1) {
                HexFormat hexFormat = HexFormat.of().withPrefix("0x");
                throw new IllegalArgumentException(STR."Not valid ird file, magic \{hexFormat.formatHex(magic)}");
            }

            byte version = input.readByte();
            /*if (version < 6 || version > 9) {
                throw new IllegalArgumentException("Version " + version + " is not supported");
            }*/

            String serial = input.readFixedLengthString(9);
            String title = input.readString();

            String fwVersion = input.readFixedLengthString(4);
            String mainVersion = input.readFixedLengthString(5);
            String appVersion = input.readFixedLengthString(5);

            if (version == 7) {
                // File ID, discard it
                input.readInt();
            }

            int headerSize = input.readInt();
            byte[] header = input.decompress(input.readBytes(headerSize));
            int footerSize = input.readInt();
            byte[] footer = input.decompress(input.readBytes(footerSize));

            byte regions = input.readByte();
            byte[][] regionHashes = new byte[regions][];
            for (int region = 0; region < regions; region++) {
                byte[] hash = input.getHash();
                regionHashes[region] = hash;
            }

            int files = input.readInt();
            Ird.FileChecksum[] fileHashes = new Ird.FileChecksum[files];
            for (int file = 0; file < files; file++) {
                long sector = input.readLong();
                byte[] hash = input.getHash();
                fileHashes[file] = new Ird.FileChecksum(sector, hash);
            }

            int padding = input.readInt();
            if (padding != 0) {
                throw new IllegalArgumentException(STR."Padding contained \{padding}");
            }
            byte[] pic = null;
            if (version == 9) {
                pic = input.readBytes(115);
            }
            byte[] data1 = input.readBytes(16);
            byte[] data2 = input.readBytes(16);
            if (version < 9) {
                pic = input.readBytes(115);
            }
            int uid = input.readInt();

            // check crc
            long crc = input.getChecksum();
            long expected = Integer.toUnsignedLong(input.readInt());
            if (crc != expected) {
                throw new IllegalArgumentException("CRC is invalid! Expected %s, actual %s".formatted(expected, crc));
            }

            return new Ird(serial, title, fwVersion, mainVersion, appVersion, header, footer, regionHashes, fileHashes, pic, data1, data2, uid);
        }
    }

    private static InputStream open(Path file) throws IOException {
        try {
            return new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file)));
        } catch (ZipException e) {
            // Not compressed, try to read as uncompressed data
            return new BufferedInputStream(Files.newInputStream(file));
        }
    }
}
