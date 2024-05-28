package fi.smaragdi.discvalidator.parsing.ps3;

import fi.smaragdi.discvalidator.parsing.BinaryInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParamSfo {
    private enum DataType {
        UTF8_NOT_TERMINATED,
        UTF8,
        INT;

        public static DataType fromShort(short s) {
            return switch (s) {
                case 0x04 -> UTF8_NOT_TERMINATED;
                case 0x204 -> UTF8;
                case 0x404 -> INT;
                default -> throw new IllegalArgumentException("Unexpected value: " + s);
            };
        }
    }

    private record SfoIndex(
            int keyOffset,
            DataType type,
            int dataUsed,
            int dataLength,
            int dataOffset
    ) {}

    public static LinkedHashMap<String, String> parse(Path file) throws IOException {
        try (BinaryInputStream.SeekableBinaryInputStream stream = new BinaryInputStream.SeekableBinaryInputStream(new FileInputStream(file.toFile()), ByteOrder.LITTLE_ENDIAN)) {
            byte[] magic = stream.readBytes(4);
            if (Arrays.mismatch(magic, "\0PSF".getBytes(StandardCharsets.UTF_8)) >= 0) {
                throw new IllegalArgumentException("Invalid magic " + Arrays.toString(magic));
            }
            byte[] version = stream.readBytes(4); // should be \1\1\0\0, but not guaranteed?
            int keyTableStart = stream.readInt();
            int dataTableStart = stream.readInt();
            int tableEntries = stream.readInt();

            List<SfoIndex> indexes = new ArrayList<>(tableEntries);
            // Indexes
            for (int i = 0; i < tableEntries; i++) {
                int keyOffset = stream.readShort();
                DataType type = DataType.fromShort(stream.readShort());
                int dataUsed = stream.readInt();
                int dataLen = stream.readInt();
                int dataOffset = stream.readInt();
                indexes.add(new SfoIndex(keyOffset, type, dataUsed, dataLen, dataOffset));
            }
            if (indexes.size() != tableEntries) {
                throw new IllegalStateException(indexes.size() + " != " + tableEntries);
            }
            stream.seek(keyTableStart);
            LinkedHashMap<String, String> contents = new LinkedHashMap<>();
            long keyStart = stream.getPosition();
            // Keys
            for (SfoIndex sfoIndex : indexes) {
                int offset = sfoIndex.keyOffset();
                if (stream.getPosition() != keyStart + offset) {
                    stream.seek(keyStart + offset);
                }
                contents.putLast(stream.readNullTerminatedString(), null);
            }
            stream.seek(dataTableStart);
            long dataStart = stream.getPosition();
            Iterator<Map.Entry<String, String>> iterator = contents.entrySet().iterator();
            // Values
            for (SfoIndex sfoIndex : indexes) {
                Map.Entry<String, String> next = iterator.next();
                int offset = sfoIndex.dataOffset();
                if (stream.getPosition() != dataStart + offset) {
                    stream.seek(dataStart + offset);
                }
                next.setValue(
                        switch (sfoIndex.type) {
                            case UTF8_NOT_TERMINATED -> stream.readFixedLengthString(sfoIndex.dataLength);
                            case UTF8 -> stream.readNullTerminatedString();
                            case INT -> String.valueOf(stream.readInt());
                        }
                );
            }
            return contents;
        }
    }
}
