package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Direction;

/**
 * Packs block positions into longs: 26 bits of x, 26 bits of z and 12 bits of y.
 *
 * <p>Valid ranges are x and z within +-2^25 and y within +-2^11; packing outside
 * these ranges wraps.
 */
@UtilityClass
public class LightPositions {

    /**
     * Packs a block position into a long.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @return the packed position
     */
    public static long pack(int x, int y, int z) {
        return ((long) x << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    /**
     * Returns the block x of a packed position.
     *
     * @param packed the packed position
     * @return the block x
     */
    public static int x(long packed) {
        return (int) (packed >> 38);
    }

    /**
     * Returns the block y of a packed position.
     *
     * @param packed the packed position
     * @return the block y
     */
    public static int y(long packed) {
        return (int) (packed << 52 >> 52);
    }

    /**
     * Returns the block z of a packed position.
     *
     * @param packed the packed position
     * @return the block z
     */
    public static int z(long packed) {
        return (int) (packed << 26 >> 38);
    }

    /**
     * Returns the first column of a seeding column band.
     *
     * @param band  the band from {@code 0} to {@code bands - 1}
     * @param bands the number of bands; must be positive
     * @return the first local x of the band
     */
    static int bandFromX(int band, int bands) {
        return band * 16 / bands;
    }

    /**
     * Returns one past the last column of a seeding column band.
     *
     * @param band  the band from {@code 0} to {@code bands - 1}
     * @param bands the number of bands; must be positive
     * @return one past the last local x of the band
     */
    static int bandToX(int band, int bands) {
        return (band + 1) * 16 / bands;
    }

    /**
     * Visits the ring of cells just outside the chunk between two heights;
     * {@code intoChunk} points from each outside cell to its inside neighbour.
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     * @param fromY  the lowest visited y
     * @param toY    one past the highest visited y
     * @param border the visitor
     */
    static void forNeighbourBorder(int chunkX, int chunkZ, int fromY, int toY, NeighbourBorder border) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int y = fromY; y < toY; y++) {
            for (int i = 0; i < 16; i++) {
                border.outside(minX - 1, y, minZ + i, Direction.EAST);
                border.outside(minX + 16, y, minZ + i, Direction.WEST);
                border.outside(minX + i, y, minZ - 1, Direction.SOUTH);
                border.outside(minX + i, y, minZ + 16, Direction.NORTH);
            }
        }
    }

    @FunctionalInterface
    interface NeighbourBorder {
        void outside(int x, int y, int z, Direction intoChunk);
    }
}
