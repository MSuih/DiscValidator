package fi.smaragdi.discvalidator.ird;

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
}
