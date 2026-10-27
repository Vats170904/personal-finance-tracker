package com.finance.repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a file so that it is never left half-written.
 *
 * <p>The content is first written to a temporary file in the same directory, which
 * is then moved over the target. If the program crashes or the disk is full while
 * writing, the old file is still intact.
 */
final class AtomicFileWriter {

    @FunctionalInterface
    interface ContentWriter {
        void writeTo(BufferedWriter writer) throws IOException;
    }

    private AtomicFileWriter() {
        // utility class
    }

    static void write(Path target, ContentWriter content) throws IOException {
        Path absoluteTarget = target.toAbsolutePath();
        Path directory = absoluteTarget.getParent();
        Files.createDirectories(directory);

        Path temp = Files.createTempFile(directory, absoluteTarget.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                content.writeTo(writer);
            }
            moveReplacing(temp, absoluteTarget);
        } finally {
            // Only does something if writing or moving failed.
            Files.deleteIfExists(temp);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Some file systems cannot move atomically; a plain replace is the best fallback.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
