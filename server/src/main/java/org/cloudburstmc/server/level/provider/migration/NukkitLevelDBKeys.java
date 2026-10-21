package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * Encodes Nukkit's dimension-qualified chunk keys for migration.
 */
@UtilityClass
public class NukkitLevelDBKeys {

    public static byte[] key(int x, int z, int dimension, int tag) {
        ByteBuffer key = ByteBuffer.allocate(dimension == 0 ? 9 : 13).order(ByteOrder.LITTLE_ENDIAN);
        key.putInt(x).putInt(z);
        if (dimension != 0) {
            key.putInt(dimension);
        }

        return key.put((byte) tag).array();
    }

    public static byte[] section(int x, int z, int dimension, int y) {
        if (y < Byte.MIN_VALUE || y > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Section Y cannot be encoded: " + y);
        }

        byte[] prefix = key(x, z, dimension, '/');
        return ByteBuffer.allocate(prefix.length + 1).put(prefix).put((byte) y).array();
    }

    public static Optional<ChunkAddress> chunk(byte[] key) {
        int tagOffset;
        if (key.length == 9 || key.length == 10) {
            tagOffset = 8;
        } else if (key.length == 13 || key.length == 14) {
            tagOffset = 12;
        } else {
            return Optional.empty();
        }

        int tag = key[tagOffset] & 0xff;
        boolean section = tag == '/' && key.length == tagOffset + 2;
        boolean record = key.length == tagOffset + 1 && (tag == ',' || tag == 'v' || tag == '0' || tag == '1' || tag == '2');
        if (!section && !record) {
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
