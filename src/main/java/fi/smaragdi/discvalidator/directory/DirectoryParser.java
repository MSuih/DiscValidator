package fi.smaragdi.discvalidator.directory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectoryParser {
    private DirectoryParser() {
        throw new AssertionError("Creating instances is not allowed");
    }

    public static List<DirectoryItem> parse(Path rootDirectory, Set<FileVisitOption> options) {
        if (!Files.isDirectory(rootDirectory)) {
            throw new IllegalArgumentException(STR."Invalid directory \{rootDirectory}");
        }
        List<DirectoryItem> parseResults = Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1))) {
            FileVisitor<Path> visitor = createVisitor(rootDirectory, parseResults, executorService);
            Files.walkFileTree(rootDirectory, options, Integer.MAX_VALUE, visitor);
        } catch (IOException e) {
            throw new RuntimeException("Could not iterate through the game directory", e);
        }
        return parseResults;
    }

    private static FileVisitor<Path> createVisitor(Path root, List<DirectoryItem> list, ExecutorService executorService) {
        return new FileVisitor<>() {
            private String relativePath(Path location) {
                return "/" + root.relativize(location);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String s = relativePath(dir);
                list.add(DirectoryItem.forFolder(s));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String s = relativePath(file);
                executorService.submit(() -> {
                    try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                         DigestOutputStream outputStream = new DigestOutputStream(OutputStream.nullOutputStream(), MessageDigest.getInstance("MD5"))) {
                        inputStream.transferTo(outputStream);
                        list.add(DirectoryItem.forFile(s, outputStream.getMessageDigest().digest()));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("Your runtime does not provide MD5 algorithm", e);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read hash of file", e);
                    }
                });
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Should this just continue?
                throw new IllegalStateException("File visit failed on " + file.toAbsolutePath(), exc);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (exc != null) {
                    // Maybe this should continue?
                    throw new IllegalStateException("Did not successfully parse folder " + dir.toAbsolutePath(), exc);
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
