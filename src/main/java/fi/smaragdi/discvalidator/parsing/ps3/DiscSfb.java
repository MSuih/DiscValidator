package fi.smaragdi.discvalidator.parsing.ps3;

import fi.smaragdi.discvalidator.parsing.BinaryInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
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
                    case '\0' -> null;
                    //FIXME: Log and return unknown
                    default -> throw new IllegalArgumentException("Unexpected value: " + b);
                };
                if (f != null) {
                    set.add(f);
                }
            }
            return set;
        }
    }

    public static DiscSfb parse(Path file) throws IOException {
        try (BinaryInputStream.SeekableBinaryInputStream bis = new BinaryInputStream.SeekableBinaryInputStream(new FileInputStream(file.toFile()), ByteOrder.BIG_ENDIAN)) {
            String magic = bis.readFixedLengthString(4);
            if (!magic.equals(".SFB")) {
                throw new IllegalArgumentException("Incorrect magic");
            }
            bis.readInt(); // SFB version
            bis.readBytes(0x18); // padding
            String flagType = bis.readFixedLengthString(0x10);
            if (!flagType.startsWith("HYBRID_FLAG")) {
                throw new IllegalArgumentException("Unexpected flag type " + flagType);
            }
            int contentOffset = bis.readInt();
            int contentLen = bis.readInt();
            bis.readLong(); // padding
            String titleName = bis.readFixedLengthString(8);
            if (!Objects.equals(titleName, "TITLE_ID")) {
                throw new IllegalArgumentException(titleName);
            }
            bis.readLong(); // padding
            //bis.readLong(); // multi game version?
            bis.readLong(); // padding
            //int versionOffset = bis.readInt();
            //int versionLen = bis.readInt();

            bis.seek(contentOffset);
            Set<ContentFlag> hybridFlags = ContentFlag.fromSymbols(bis.readBytes(20));
            String discSerial = bis.readFixedLengthString(10);
            String discVersion = bis.readFixedLengthString(contentLen);
            return new DiscSfb(discSerial, hybridFlags);
        }
    }
}
