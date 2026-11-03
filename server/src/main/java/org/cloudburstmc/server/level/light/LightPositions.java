package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * Packs block coordinates for lighting and maps chunk-local changes to absolute positions.
 * Horizontal coordinates use signed 26-bit values and y uses a signed 12-bit value.
 * Coordinates outside those ranges wrap.
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
     * Converts queued chunk-local block keys into packed light positions.
     * Both block layers address the same light position.
     *
     * @param queue     block keys grouped by chunk key
     * @param minHeight the level's lowest valid y
     * @return the packed light positions
     */
    public static LongArrayList collectChanges(Long2ObjectMap<IntSet> queue, int minHeight) {
        LongArrayList changed = new LongArrayList();

        for (Long2ObjectMap.Entry<IntSet> entry : queue.long2ObjectEntrySet()) {
            long chunkKey = entry.getLongKey();

            for (int blockKey : entry.getValue()) {
                changed.add(CloudChunk.lightPosition(chunkKey, blockKey, minHeight));
            }
        }

        return changed;
    }

    /**
     * Returns the first column of a seeding column band.
     *
     * @param band  the band from {@code 0} to {@code bands - 1}
     * @param bands the positive number of bands
     * @return the first local x of the band
     */
    public static int bandFromX(int band, int bands) {
        return band * 16 / bands;
    }

    /**
     * Returns one past the last column of a seeding column band.
     *
     * @param band  the band from {@code 0} to {@code bands - 1}
     * @param bands the positive number of bands
     * @return one past the last local x of the band
     */
    public static int bandToX(int band, int bands) {
        return (band + 1) * 16 / bands;
    }

    /**
     * Visits the four outer chunk borders over {@code [fromY, toY)}.
     * The supplied direction points from each visited cell into the chunk.
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     * @param fromY  the lowest visited y
     * @param toY    one past the highest visited y
     * @param border the visitor
     */
    public static void forNeighbourBorder(int chunkX, int chunkZ, int fromY, int toY, NeighbourBorder border) {
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

    /**
     * Visits a border cell using absolute coordinates and its inward direction.
     */
    @FunctionalInterface
    public interface NeighbourBorder {
        void outside(int x, int y, int z, Direction intoChunk);
    }
}
