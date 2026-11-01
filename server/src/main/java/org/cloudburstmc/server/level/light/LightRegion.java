package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Runs one light pass over locked chunks: collects the loaded 3x3 around each
 * affected chunk, locks it in sorted {@link CloudChunk} order, runs both
 * channels, and releases in reverse. Locks taken before a failure are released
 * before it propagates.
 *
 * <p>Each operation runs propagation in slices: it locks the 3x3, seeds and
 * runs up to {@code sliceSteps} propagation steps, releases, records the hold,
 * and re-locks with a freshly read 3x3 until drained. Chunks that unloaded in
 * between read as absent. One {@link LightStateCache} is shared by the whole
 * operation.
 *
 * <p>{@link LevelLightEngine} is only thin synchronous overloads over it.
 */
final class LightRegion {

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> loadedChunks;
    private final int sliceSteps;
    private final @Nullable LongConsumer holdNanos;
    private volatile @Nullable Runnable afterSlice;

    /**
     * @param loadedChunks returns the chunk for a chunk key, or null when absent
     */
    LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
            LongFunction<CloudChunk> loadedChunks) {
        this(registry, minHeight, maxHeight, skyLight, loadedChunks, Integer.MAX_VALUE, null);
    }

    /**
     * @param loadedChunks returns the chunk for a chunk key, or null when absent
     * @param sliceSteps   the propagation steps per lock hold; must be positive
     * @param holdNanos    receives each lock hold duration in nanoseconds, or null
     */
    LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
            LongFunction<CloudChunk> loadedChunks, int sliceSteps, @Nullable LongConsumer holdNanos) {
        Preconditions.checkArgument(sliceSteps > 0, "sliceSteps must be positive");
        this.registry = registry;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.skyLight = skyLight;
        this.loadedChunks = loadedChunks;
        this.sliceSteps = sliceSteps;
        this.holdNanos = holdNanos;
    }

    /**
     * Runs a hook once after the next slice, without holding chunk locks.
     *
     * @param afterSlice the hook, or null to clear it
     */
    void afterSliceForTest(@Nullable Runnable afterSlice) {
        this.afterSlice = afterSlice;
    }

    /**
     * Lights a chunk from scratch with its loaded neighbours.
     *
     * <p>Skipped when the chunk itself is no longer loaded.
     *
     * @return true when propagation drained
     */
    boolean initialize(int chunkX, int chunkZ) {
        if (this.loadedChunks.apply(CloudChunk.key(chunkX, chunkZ)) == null) {
            return true;
        }
        return runSlices(() -> surrounding(chunkX, chunkZ, true), (pass, block, sky) -> {
            BlockLightEngine.initialize(pass.blockAccess, chunkX, chunkZ, block);
            if (this.skyLight) {
                SkyLightEngine.initialize(pass.skyAccess, chunkX, chunkZ, sky);
            }
        });
    }

    /**
     * Refreshes the ring around an unloaded chunk.
     *
     * @return true when propagation drained
     */
    boolean unloaded(int chunkX, int chunkZ) {
        if (surrounding(chunkX, chunkZ, false).isEmpty()) {
            return true;
        }
        return runSlices(() -> surrounding(chunkX, chunkZ, false), (pass, block, sky) -> {
            forNeighbourBorder(chunkX, chunkZ, this.minHeight, this.maxHeight, (x, y, z, intoChunk) -> {
                invalidateIfHorizontallyLit(pass.blockAccess, block, x, y, z);
                if (this.skyLight) {
                    invalidateIfHorizontallyLit(pass.skyAccess, sky, x, y, z);
                }
            });
        });
    }

    /**
     * Repropagates both channels from the changed packed positions.
     *
     * @return true when propagation drained
     */
    boolean update(LongCollection positions) {
        if (positions.isEmpty() || neighbourhood(positions).isEmpty()) {
            return true;
        }
        return runSlices(() -> neighbourhood(positions), (pass, block, sky) -> {
            for (long packed : positions) {
                block.invalidate(pass.blockAccess, packed);
                if (this.skyLight) {
                    sky.invalidate(pass.skyAccess, packed);
                }
            }
        });
    }

    /**
     * Seeds once, then locks, propagates and releases until both channels drain.
     * The scope is re-read before every slice, so unloaded chunks drop out.
     *
     * @return true when propagation drained
     */
    private boolean runSlices(Supplier<? extends Collection<CloudChunk>> scope, Seeder seeder) {
        LightStateCache states = new LightStateCache(this.registry);
        LightPropagation block = new LightPropagation(LightChannel.BLOCK);
        LightPropagation sky = new LightPropagation(LightChannel.SKY);
        boolean seeded = false;
        while (true) {
            LockedChunk[] locked = lockSorted(scope.get());
            long start = System.nanoTime();
            boolean blockDrained = false;
            boolean skyDrained = false;
            try {
                Pass pass = new Pass(states, this.minHeight, this.maxHeight, views(locked));
                if (!seeded) {
                    seeder.seed(pass, block, sky);
                    seeded = true;
                }
                blockDrained = block.run(pass.blockAccess, this.sliceSteps);
                skyDrained = !this.skyLight || sky.run(pass.skyAccess, this.sliceSteps);
            } finally {
                releaseReversed(locked);
                notifyHold(System.nanoTime() - start);
            }
            notifyAfterSlice();
            if (blockDrained && skyDrained) {
                return true;
            }
        }
    }

    private void notifyHold(long holdNanos) {
        if (this.holdNanos != null) {
            this.holdNanos.accept(holdNanos);
        }
    }

    private void notifyAfterSlice() {
        Runnable hook = this.afterSlice;
        if (hook != null) {
            this.afterSlice = null;
            hook.run();
        }
    }

    private Set<CloudChunk> neighbourhood(LongCollection positions) {
        Set<CloudChunk> neighbourhood = new TreeSet<>();
        for (long packed : positions) {
            neighbourhood.addAll(surrounding(LightPositions.x(packed) >> 4, LightPositions.z(packed) >> 4, true));
        }
        return neighbourhood;
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

    @FunctionalInterface
    private interface Seeder {
        void seed(Pass pass, LightPropagation block, LightPropagation sky);
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
        final ChunkLightAccess blockAccess;
        final ChunkLightAccess skyAccess;

        Pass(LightStateCache states, int minHeight, int maxHeight, LongFunction<LockedChunk> views) {
            this.blockAccess = new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views);
            this.skyAccess = new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views);
        }
    }
}
