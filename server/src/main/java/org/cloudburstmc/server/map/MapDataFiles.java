package org.cloudburstmc.server.map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.persistence.AtomicFiles;
import org.cloudburstmc.server.persistence.NbtFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapDataFiles {

    private static final int DATA_VERSION = 2;

    public static NbtMap read(Path path, long id) throws IOException {
        NbtMap data = NbtFiles.readCompressed(path);
        if (data.getInt("version") != DATA_VERSION) {
            throw new IOException("Unsupported map data format: " + path);
        }

        validate(data, id);
        return data;
    }

    public static void write(Path path, long id, NbtMap data) throws IOException {
        validate(data, id);
        NbtMap stored = data.toBuilder().putInt("version", DATA_VERSION).build();
        AtomicFiles.write(path, NbtFiles.encodeCompressed(stored));
    }

    private static void validate(NbtMap data, long id) throws IOException {
        int scale = data.getInt("scale");
        if (data.getIntArray("pixels").length != 128 * 128 || scale < 0 || scale >= MapScale.values().length) {
            throw new IOException("Invalid map dimensions or scale");
        }

        long[] ids = data.getLongArray("mapIds");
        if (ids.length != MapScale.values().length - scale || ids[0] != id
                || Arrays.stream(ids).anyMatch(value -> value < 0)
                || Arrays.stream(ids).distinct().count() != ids.length) {
            throw new IOException("Invalid map scale hierarchy");
        }
    }
}
