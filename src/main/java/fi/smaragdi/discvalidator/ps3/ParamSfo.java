package fi.smaragdi.discvalidator.ps3;

import fi.smaragdi.discvalidator.BinaryInputStream;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public void parse(Path file) throws IOException {
        try (BinaryInputStream stream = new BinaryInputStream(Files.newInputStream(file), ByteOrder.LITTLE_ENDIAN)) {
            String magic = stream.readFixedLengthString(4);
            String version = stream.readFixedLengthString(4);
            int keyTableStart = stream.readInt();
            int dataTableStart = stream.readInt();
            int tableEntries = stream.readInt();

            // Indexes
            for (int i = 0; i < tableEntries; i++) {
                int keyOffset = stream.readShort();
                DataType type = DataType.fromShort(stream.readShort());
                int dataUsed = stream.readInt();
                int dataLen = stream.readInt();
                int dataOffset = stream.readInt();
            }
            // Keys
            for (int i = 0; i < tableEntries; i++) {

            }
            // Values
        }
    }
}
