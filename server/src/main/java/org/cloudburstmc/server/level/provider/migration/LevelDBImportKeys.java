package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Encodes Bedrock chunk keys and actor storage references for import.
 * X and Z arguments are chunk coordinates, not block coordinates.
 * Coordinates and nonzero dimension IDs use little-endian integers. Dimension zero is omitted.
 */
@UtilityClass
public class LevelDBImportKeys {

    public static final int ACTOR_STORAGE_KEY_SIZE = 8;

    private static final byte[] ACTOR_INDEX_PREFIX = "digp".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ACTOR_PREFIX = "actorprefix".getBytes(StandardCharsets.US_ASCII);

    public static byte[] actorIndex(int x, int z, int dimension) {
        ByteBuffer key = ByteBuffer.allocate(dimension == 0 ? 12 : 16).order(ByteOrder.LITTLE_ENDIAN);
        key.put(ACTOR_INDEX_PREFIX).putInt(x).putInt(z);
        if (dimension != 0) {
            key.putInt(dimension);
        }

        return key.array();
    }

    /**
     * Prefixes an opaque eight-byte storage key without interpreting it as an entity ID.
     */
    public static byte[] actor(byte[] storageKey) {
        Objects.requireNonNull(storageKey, "storageKey");
        if (storageKey.length != ACTOR_STORAGE_KEY_SIZE) {
            throw new IllegalArgumentException("Actor storage keys must contain eight bytes");
        }
        return ByteBuffer.allocate(ACTOR_PREFIX.length + ACTOR_STORAGE_KEY_SIZE).put(ACTOR_PREFIX).put(storageKey).array();
    }

    public static byte[] key(int x, int z, int dimension, int tag) {
        ByteBuffer key = ByteBuffer.allocate(dimension == 0 ? 9 : 13).order(ByteOrder.LITTLE_ENDIAN);
        key.putInt(x).putInt(z);
        if (dimension != 0) {
            key.putInt(dimension);
        }
        return key.put((byte) tag).array();
    }

    /**
     * Encodes a sub-chunk key with a signed byte section Y coordinate.
     */
    public static byte[] section(int x, int z, int dimension, int y) {
        if (y < Byte.MIN_VALUE || y > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Section Y cannot be encoded: " + y);
        }

        byte[] prefix = key(x, z, dimension, '/');
        return ByteBuffer.allocate(prefix.length + 1).put(prefix).put((byte) y).array();
    }

    /**
     * Identifies stored chunks from current or legacy version keys without inspecting record values.
     */
    public static Optional<ChunkAddress> storedChunk(byte[] key) {
        if (key.length != 9 && key.length != 13) {
            return Optional.empty();
        }

        int tagOffset = key.length - 1;
        int tag = key[tagOffset] & 0xff;
        if (tag != ',' && tag != 'v') {
            return Optional.empty();
        }

        ByteBuffer data = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        int x = data.getInt();
        int z = data.getInt();
        int dimension = tagOffset == 12 ? data.getInt() : 0;
        return Optional.of(new ChunkAddress(x, z, dimension));
    }

    public record ChunkAddress(int x, int z, int dimension) {
    }
}
