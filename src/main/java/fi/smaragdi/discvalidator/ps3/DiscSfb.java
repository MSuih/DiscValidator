package fi.smaragdi.discvalidator.ps3;

import fi.smaragdi.discvalidator.BinaryInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

public record DiscSfb(String serial, Set<ContentFlag> flags) {

    public enum ContentFlag {
        BENEFITS,
        THEMES,
        VIDEO,
        FRIENDS,
        GAME,
        MUSIC,
        PHOTO,
        FW_UPDATE,
        MOVIE,
        SETTINGS;

        public static Set<ContentFlag> fromSymbols(byte[] symbols) {
            Set<ContentFlag> set = EnumSet.noneOf(ContentFlag.class);
            for (byte b : symbols) {
                ContentFlag f = switch (b) {
                    case 'S' -> BENEFITS;
                    case 'T' -> THEMES;
                    case 'V' -> VIDEO;
                    case 'g' -> GAME;
                    case 'u' -> FW_UPDATE;
                    case 'v' -> MOVIE;
                    // These are guesses
                    case 'f' -> FRIENDS;
                    case 'm' -> MUSIC;
                    case 'p' -> PHOTO;
                    case 's' -> SETTINGS;
                    //FIXME: Log and return unknown
                    default -> throw new IllegalArgumentException("Unexpected value: " + b);
                };
                set.add(f);
            }
            return set;
        }
    }

    public static DiscSfb parse(Path file) throws IOException {
        try (BinaryInputStream bis = new BinaryInputStream(new FileInputStream(file.toFile()), ByteOrder.BIG_ENDIAN)) {
            String magic = bis.readFixedLengthString(4);
            if (!magic.equals(".SFB")) {
                throw new IllegalArgumentException("Incorrect magic");
            }
            bis.readInt(); // SFB version
            bis.readBytes(18); // padding
            String flagType = bis.readFixedLengthString(10);
            if (!flagType.startsWith("HYBRID_FLAG")) {
                throw new IllegalArgumentException("Unexpected flag type " + flagType);
            }
            int contentOffset = bis.readInt();
            int contentLen = bis.readInt();
            bis.readLong(); // padding
            String serial = bis.readFixedLengthString(8);
            bis.readLong(); // padding
            bis.readLong(); // multi game version?
            bis.readLong(); // padding
            int versionOffset = bis.readInt();
            int versionLen = bis.readInt();
            //TODO: Could replace this with seek or something like that
            bis.skip(contentOffset - 0x78); // more padding
            Set<ContentFlag> hybridFlags = ContentFlag.fromSymbols(bis.readBytes(20));
            String discSerial = bis.readFixedLengthString(10);
            String discVersion = bis.readFixedLengthString(10);
            return new DiscSfb(serial, hybridFlags);
        }
    }
}
