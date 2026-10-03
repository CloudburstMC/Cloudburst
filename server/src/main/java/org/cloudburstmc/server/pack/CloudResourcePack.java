package org.cloudburstmc.server.pack;

import lombok.Getter;
import lombok.ToString;
import org.cloudburstmc.api.pack.PackType;
import org.cloudburstmc.api.pack.ResourcePack;
import org.cloudburstmc.api.util.SemVersion;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

@Getter
@ToString(onlyExplicitlyIncluded = true)
public class CloudResourcePack implements ResourcePack, Closeable {

    @ToString.Include
    private final UUID id;
    @ToString.Include
    private final String name;
    @ToString.Include
    private final SemVersion version;
    private final long size;
    @Getter(lombok.AccessLevel.NONE)
    private final byte[] hash;
    @Getter(lombok.AccessLevel.NONE)
    private final Path archive;
    @Getter(lombok.AccessLevel.NONE)
    private final FileChannel channel;
    private final List<CloudPackDependency> dependencies;

    public CloudResourcePack(CloudPackManifest manifest, Path archive, long size, byte[] hash) throws IOException {
        this.id = manifest.getHeader().getUuid();
        this.name = manifest.getHeader().getName();
        this.version = manifest.getHeader().getVersion();
        this.dependencies = manifest.getDependencies().stream()
                .map(dependency -> new CloudPackDependency(dependency.getUuid(), dependency.getVersion()))
                .toList();
        this.archive = archive;
        this.size = size;
        this.hash = hash.clone();
        this.channel = FileChannel.open(archive, StandardOpenOption.READ);
    }

    @Override
    public PackType getType() {
        return PackType.RESOURCES;
    }

    @Override
    public byte[] getHash() {
        return this.hash.clone();
    }

    /**
     * Reads a range using an independent position so concurrent transfers do not share a cursor.
     * An offset at the end returns an empty array.
     *
     * @throws IllegalArgumentException if the range starts outside the archive or the length is not positive
     * @throws IOException              if the archive cannot be read completely
     */
    public byte[] readChunk(long offset, int length) throws IOException {
        if (offset < 0 || offset > this.size || length <= 0) {
            throw new IllegalArgumentException("Invalid pack byte range");
        }

        byte[] chunk = new byte[(int) Math.min(length, this.size - offset)];
        ByteBuffer buffer = ByteBuffer.wrap(chunk);
        while (buffer.hasRemaining()) {
            int count = this.channel.read(buffer, offset + buffer.position());
            if (count < 0) {
                throw new EOFException("Pack ended before the requested range was read");
            }
        }

        return chunk;
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.close();
        } finally {
            Files.deleteIfExists(this.archive);
        }
    }
}
