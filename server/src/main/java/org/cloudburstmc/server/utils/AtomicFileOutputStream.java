package org.cloudburstmc.server.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFileOutputStream extends OutputStream {
    private final OutputStream output;
    private final Path temporaryPath;
    private final Path targetPath;
    private final Path backupPath;
    private boolean closed;
    private boolean failed;

    public AtomicFileOutputStream(Path targetPath) throws IOException {
        this.targetPath = targetPath;
        this.temporaryPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        this.backupPath = targetPath.resolveSibling(targetPath.getFileName() + "_old");

        Files.createDirectories(targetPath.getParent());
        this.output = Files.newOutputStream(this.temporaryPath);
    }

    @Override
    public void write(int value) throws IOException {
        try {
            this.output.write(value);
        } catch (IOException e) {
            this.failed = true;
            throw e;
        }
    }

    @Override
    public void write(byte @NotNull [] bytes) throws IOException {
        try {
            this.output.write(bytes);
        } catch (IOException e) {
            this.failed = true;
            throw e;
        }
    }

    @Override
    public void write(byte @NotNull [] bytes, int offset, int length) throws IOException {
        try {
            this.output.write(bytes, offset, length);
        } catch (IOException e) {
            this.failed = true;
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            this.output.flush();
        } catch (IOException e) {
            this.failed = true;
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }

        this.closed = true;

        try {
            this.output.close();
        } catch (IOException e) {
            this.failed = true;
            this.deleteTemporaryFile();
            throw e;
        }

        if (this.failed) {
            this.deleteTemporaryFile();
            return;
        }

        if (Files.exists(this.targetPath)) {
            Files.copy(this.targetPath, this.backupPath, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Files.move(this.temporaryPath,
                    this.targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(this.temporaryPath, this.targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteTemporaryFile() throws IOException {
        Files.deleteIfExists(this.temporaryPath);
    }
}
