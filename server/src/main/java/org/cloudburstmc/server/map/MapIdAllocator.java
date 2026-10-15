package org.cloudburstmc.server.map;

import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MapIdAllocator {

    private final Path counterFile;
    private long nextId;

    public MapIdAllocator(Path directory, long minimumNextId) throws IOException {
        if (minimumNextId < 0) {
            throw new IllegalArgumentException("Minimum map ID must not be negative");
        }

        this.counterFile = directory.resolve("idcounts.dat");
        this.nextId = minimumNextId;

        if (Files.exists(this.counterFile)) {
            try (NBTInputStream input = NbtUtils.createGZIPReader(Files.newInputStream(this.counterFile))) {
                if (!(input.readTag() instanceof NbtMap data) || !(data.get("nextId") instanceof Long next) || next < 0) {
                    throw new IOException("Invalid map ID counter");
                }

                this.nextId = Math.max(this.nextId, next);
            }
        }
    }

    public boolean isReserved(long id) {
        return id >= 0 && id < this.nextId;
    }

    public long[] allocate(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Map ID count must not be negative");
        }

        long[] ids = new long[count];
        long end = Math.addExact(this.nextId, count);
        for (int i = 0; i < count; i++) {
            ids[i] = this.nextId + i;
        }

        if (count == 0) {
            return ids;
        }

        Path temporary = this.counterFile.resolveSibling("idcounts.tmp");
        try {
            try (NBTOutputStream output = NbtUtils.createGZIPWriter(Files.newOutputStream(temporary))) {
                output.writeTag(NbtMap.builder().putLong("nextId", end).build());
            }

            try {
                Files.move(temporary, this.counterFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, this.counterFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not reserve map IDs", e);
        }

        this.nextId = end;
        return ids;
    }
}
