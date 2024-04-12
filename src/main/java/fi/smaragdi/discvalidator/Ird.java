package fi.smaragdi.discvalidator;

import java.io.*;
import java.nio.file.Path;

public record Ird(
        String serial,
        String title,
        String fwVersion,
        String mainVersion,
        String appVersion,
        byte[] header,
        byte[] footer,
        byte[][] regions,
        FileChecksum[] fileChecksums,
        byte[] pic,
        byte[] data1,
        byte[] data2,
        int uid) {

    public record FileChecksum(long key, byte[] hash) {}

    public static Ird parse(Path file) throws IOException {
        IrdParser parser = new IrdParser(file);
        return parser.parse();
    }
}
