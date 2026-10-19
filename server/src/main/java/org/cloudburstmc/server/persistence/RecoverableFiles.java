package org.cloudburstmc.server.persistence;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates recovery candidates and keeps corrupt data separate from usable backups.
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecoverableFiles {

    public static <T> Optional<T> read(Path target, Decoder<T> decoder) throws IOException {
        Path backup = backup(target);
        if (Files.notExists(target) && Files.notExists(backup)) {
            return Optional.empty();
        }

        IOException primaryFailure;
        try {
            return Optional.of(decode(target, decoder));
        } catch (IOException failure) {
            primaryFailure = failure;
        }

        T recovered;
        try {
            recovered = decode(backup, decoder);
        } catch (IOException failure) {
            primaryFailure.addSuppressed(failure);
            throw new IOException("Neither primary nor backup data is usable: " + target, primaryFailure);
        }

        if (Files.exists(target)) {
            preserveCorrupt(target);
        }

        AtomicFiles.write(target, Files.readAllBytes(backup));
        log.warn("Recovered {} from {}", target, backup);
        return Optional.of(recovered);
    }

    public static void write(Path target, byte[] content, Decoder<?> decoder) throws IOException {
        if (Files.exists(target)) {
            boolean valid;
            try {
                decode(target, decoder);
                valid = true;
            } catch (IOException failure) {
                valid = false;
                preserveCorrupt(target);
            }

            if (valid) {
                AtomicFiles.write(backup(target), Files.readAllBytes(target));
            }
        }

        AtomicFiles.write(target, content);
    }

    private static Path backup(Path target) {
        return target.resolveSibling(target.getFileName() + "_old");
    }

    private static void preserveCorrupt(Path target) throws IOException {
        Path preserved = target.resolveSibling(target.getFileName() + ".corrupt-" + UUID.randomUUID());
        Files.copy(target, preserved);
        log.warn("Preserved corrupt data from {} as {}", target, preserved);
    }

    private static <T> T decode(Path path, Decoder<T> decoder) throws IOException {
        try {
            return Objects.requireNonNull(decoder.read(path), "decoded data");
        } catch (RuntimeException failure) {
            throw new IOException("Invalid stored data: " + path, failure);
        }
    }

    @FunctionalInterface
    public interface Decoder<T> {
        T read(Path path) throws IOException;
    }
}
