package org.cloudburstmc.server.persistence;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Replaces stored files without exposing a partially written replacement.
 */
@UtilityClass
public class AtomicFiles {

    public static void write(Path path, String content) throws IOException {
        write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void write(Path path, byte[] content) throws IOException {
        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());

        Path temporary = Files.createTempFile(target.getParent(), "save-", ".tmp");
        try {
            Files.write(temporary, content);

            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
