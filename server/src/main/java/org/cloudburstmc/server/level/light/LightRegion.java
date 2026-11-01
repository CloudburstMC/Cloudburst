package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongFunction;

/**
 * Runs one light pass over locked chunks: collects the loaded 3x3 around each
 * affected chunk, locks it in sorted {@link CloudChunk} order, runs both
 * channels, and releases in reverse. Locks taken before a failure are released
 * before it propagates.
 *
 * <p>{@link LevelLightEngine} is only thin synchronous overloads over it.
 */
final class LightRegion {

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> loadedChunks;

    /**
     * @param loadedChunks returns the chunk for a chunk key, or null when absent
     */
    LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
            LongFunction<CloudChunk> loadedChunks) {
        this.registry = registry;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.skyLight = skyLight;
        this.loadedChunks = loadedChunks;
    }

    /**
     * Lights a chunk from scratch with its loaded neighbours.
     *
     * @return true when propagation drained
     */
    boolean initialize(int chunkX, int chunkZ) {
        LockedChunk[] locked = lockSorted(surrounding(chunkX, chunkZ, true));
        try {
            Pass pass = new Pass(this.registry, this.minHeight, this.maxHeight, views(locked));
            BlockLightEngine.initialize(pass.blockAccess, chunkX, chunkZ, pass.block);
            boolean drained = pass.block.run(pass.blockAccess, Integer.MAX_VALUE);
            if (this.skyLight) {
                SkyLightEngine.initialize(pass.skyAccess, chunkX, chunkZ, pass.sky);
                drained &= pass.sky.run(pass.skyAccess, Integer.MAX_VALUE);
            }
            return drained;
        } finally {
            releaseReversed(locked);
        }
    }

    /**
     * Refreshes the ring around an unloaded chunk.
     *
     * @return true when propagation drained
     */
    boolean unloaded(int chunkX, int chunkZ) {
        List<CloudChunk> ring = surrounding(chunkX, chunkZ, false);
        if (ring.isEmpty()) {
            return true;
        }
        LockedChunk[] locked = lockSorted(ring);
        try {
            Pass pass = new Pass(this.registry, this.minHeight, this.maxHeight, views(locked));
            forNeighbourBorder(chunkX, chunkZ, this.minHeight, this.maxHeight, (x, y, z, intoChunk) -> {
                invalidateIfHorizontallyLit(pass.blockAccess, pass.block, x, y, z);
                if (this.skyLight) {
                    invalidateIfHorizontallyLit(pass.skyAccess, pass.sky, x, y, z);
                }
            });
            boolean drained = pass.block.run(pass.blockAccess, Integer.MAX_VALUE);
            if (this.skyLight) {
                drained &= pass.sky.run(pass.skyAccess, Integer.MAX_VALUE);
            }
            return drained;
        } finally {
            releaseReversed(locked);
        }
    }

    /**
     * Repropagates both channels from the changed packed positions.
     *
     * @return true when propagation drained
     */
    boolean update(LongCollection positions) {
        if (positions.isEmpty()) {
            return true;
        }
        Set<CloudChunk> surrounding = new TreeSet<>();
        for (long packed : positions) {
            surrounding.addAll(surrounding(LightPositions.x(packed) >> 4, LightPositions.z(packed) >> 4, true));
        }
        if (surrounding.isEmpty()) {
            return true;
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            Pass pass = new Pass(this.registry, this.minHeight, this.maxHeight, views(locked));
            for (long packed : positions) {
                pass.block.invalidate(pass.blockAccess, packed);
                if (this.skyLight) {
                    pass.sky.invalidate(pass.skyAccess, packed);
                }
            }
            boolean drained = pass.block.run(pass.blockAccess, Integer.MAX_VALUE);
            if (this.skyLight) {
                drained &= pass.sky.run(pass.skyAccess, Integer.MAX_VALUE);
            }
            return drained;
        } finally {
            releaseReversed(locked);
        }
    }

    /**
     * Visits the ring of cells just outside the chunk; {@code intoChunk} points
     * from each outside cell to its inside neighbour.
     */
    static void forNeighbourBorder(int chunkX, int chunkZ, int minHeight, int maxHeight, NeighbourBorder border) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int y = minHeight; y < maxHeight; y++) {
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

    private List<CloudChunk> surrounding(int chunkX, int chunkZ, boolean includeCenter) {
        List<CloudChunk> surrounding = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    if (includeCenter) {
                        CloudChunk center = this.loadedChunks.apply(CloudChunk.key(chunkX, chunkZ));
                        if (center != null) {
                            surrounding.add(center);
                        }
                    }
                    continue;
                }
                CloudChunk neighbour = this.loadedChunks.apply(CloudChunk.key(chunkX + dx, chunkZ + dz));
                if (neighbour != null) {
                    surrounding.add(neighbour);
                }
            }
        }
        return surrounding;
    }

    private static LockedChunk[] lockSorted(Collection<CloudChunk> chunks) {
        List<CloudChunk> sorted = chunks.stream().sorted().toList();
        LockedChunk[] locked = new LockedChunk[sorted.size()];
        int taken = 0;
        boolean success = false;
        try {
            for (CloudChunk chunk : sorted) {
                locked[taken++] = chunk.lockForWrite();
            }
            success = true;
        } finally {
            if (!success) {
                for (int i = taken - 1; i >= 0; i--) {
                    locked[i].close();
                }
            }
        }
        return locked;
    }

    private static void releaseReversed(LockedChunk[] locked) {
        for (int i = locked.length - 1; i >= 0; i--) {
            locked[i].close();
        }
    }

    private static LongFunction<LockedChunk> views(LockedChunk[] locked) {
        Long2ObjectOpenHashMap<LockedChunk> byKey = new Long2ObjectOpenHashMap<>(locked.length);
        for (LockedChunk view : locked) {
            byKey.put(CloudChunk.key(view.getX(), view.getZ()), view);
        }
        return byKey::get;
    }

    /**
     * Re-queues a border cell whose light may have arrived horizontally through
     * the removed neighbour. Light 15 is either an emitter's own light or
     * direct sky from above, so it never depends on a horizontal neighbour
     * and needs no update.
     */
    private static void invalidateIfHorizontallyLit(ChunkLightAccess access, LightPropagation propagation,
            int x, int y, int z) {
        if (access.isLoaded(x, z)) {
            int light = access.light(x, y, z);
            if (light >= 1 && light <= 14) {
                propagation.invalidate(access, LightPositions.pack(x, y, z));
            }
        }
    }

    private static final class Pass {
        final LightPropagation block = new LightPropagation(LightChannel.BLOCK);
        final ChunkLightAccess blockAccess;
        final LightPropagation sky = new LightPropagation(LightChannel.SKY);
        final ChunkLightAccess skyAccess;

        Pass(BlockRegistry registry, int minHeight, int maxHeight, LongFunction<LockedChunk> views) {
            LightStateCache states = new LightStateCache(registry);
            this.blockAccess = new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views);
            this.skyAccess = new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views);
        }
    }
}
