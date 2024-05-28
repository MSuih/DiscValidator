package fi.smaragdi.discvalidator.parsing.directory;

public record DirectoryItem(String path, boolean directory, byte[] hash) {
    private static final byte[] dirHash = new byte[0];

    public static DirectoryItem forFile(String path, byte[] hash) {
        return new DirectoryItem(path, false, hash);
    }

    public static DirectoryItem forFolder(String path) {
        return new DirectoryItem(path, true, dirHash);
    }
}
