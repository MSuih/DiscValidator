package fi.smaragdi.discvalidator.ird;

import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileEntry;
import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileSystem;
import com.github.stephenc.javaisotools.loopfs.spi.SeekableInput;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

public class DiscHeaderParser {
    private DiscHeaderParser() {
        throw new AssertionError();
    }

    public record HeaderFile(
            long location,
            String name,
            String path,
            long size,
            boolean directory
    ) {}

    private static class SeekableByteArray implements SeekableInput {
        private final byte[] array;
        private long pos;
        private boolean closed;

        private SeekableByteArray(byte[] array) {
            this.array = array;
        }

        @Override
        public void seek(long l) throws IOException {
            if (closed) {
                throw new ClosedChannelException();
            }
            pos = l;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (closed) {
                throw new ClosedChannelException();
            }
            if (pos >= array.length) {
                return -1;
            }
            long toRead;
            if (pos + len > array.length) {
                toRead = array.length - pos;
            } else {
                toRead = len;
            }
            System.arraycopy(array, (int) pos, bytes, off, (int) toRead);
            return (int) toRead;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    public static List<HeaderFile> parse(byte[] bytes) {
        try (Iso9660FileSystem fileSystem = new Iso9660FileSystem(new SeekableByteArray(bytes), true)) {
            List<HeaderFile> files = new ArrayList<>();
            for (Iso9660FileEntry file : fileSystem) {
                files.add(new HeaderFile(file.getStartBlock(), file.getName(), file.getPath(), file.getSize(), file.isDirectory()));
            }
            return files;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
