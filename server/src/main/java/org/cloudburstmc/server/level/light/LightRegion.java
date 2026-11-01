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
 * <p>Chunk initialization seeds in column bands spread over one hold each, so
 * no single hold carries the whole-chunk scan; every other operation seeds at
 * once. Each operation then runs propagation in slices: it locks the 3x3,
 * runs up to {@code sliceSteps} propagation steps combined across both
 * channels, releases, records the hold, and re-locks with a freshly read 3x3
 * until drained. Chunks that unloaded in between read as absent. One
 * {@link LightStateCache} is shared by the whole operation.
 *
 * <p>{@link LightScheduler} queues background work over it.
 */
final class LightRegion {

    /**
     * The column bands initialization seeds across, one band per hold.
     */
    private static final int SEED_BANDS = 16;

    /**
     * The column bands the sky spread check runs across, one band per hold.
     */
    private static final int SPREAD_BANDS = 8;

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> loadedChunks;
    private final int sliceSteps;
    private final @Nullable LongConsumer holdNanos;
    private volatile @Nullable Runnable afterSeedBand;
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
     * @param sliceSteps   the combined propagation step budget per lock hold;
     *                     must be positive
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
     * Runs a hook once after the next seed band, without holding chunk locks.
     *
     * @param afterSeedBand the hook, or null to clear it
     */
    void afterSeedBandForTest(@Nullable Runnable afterSeedBand) {
        this.afterSeedBand = afterSeedBand;
    }

    /**
     * Runs a hook once after the next propagation slice, without holding chunk
     * locks.
     *
     * @param afterSlice the hook, or null to clear it
     */
    void afterSliceForTest(@Nullable Runnable afterSlice) {
        this.afterSlice = afterSlice;
    }

    /**
     * Lights a chunk from scratch with its loaded neighbours.
     *
     * <p>Skipped when the chunk itself is no longer loaded. Seeding runs in
     * column bands over one hold each, so no single hold carries the
     * whole-chunk scan.
     */
    void initialize(int chunkX, int chunkZ) {
        if (this.loadedChunks.apply(CloudChunk.key(chunkX, chunkZ)) == null) {
            return;
        }
        runSlices(() -> surrounding(chunkX, chunkZ, true), new Seeder() {
            @Override
            public int bands() {
                return SEED_BANDS + (LightRegion.this.skyLight ? SPREAD_BANDS : 0);
            }

            @Override
            public void seed(Pass pass, LightPropagation block, LightPropagation sky, int band) {
                if (band < SEED_BANDS) {
                    BlockLightEngine.initializeBand(pass.blockAccess, chunkX, chunkZ, band, SEED_BANDS, block);
                    if (LightRegion.this.skyLight) {
                        SkyLightEngine.initializeBand(pass.skyAccess, chunkX, chunkZ, band, SEED_BANDS, sky);
                    }
                } else {
                    int spread = band - SEED_BANDS;
                    SkyLightEngine.spreadBand(pass.skyAccess, chunkX, chunkZ, spread, SPREAD_BANDS, sky);
                }
            }
        });
    }

    /**
     * Refreshes the ring around an unloaded chunk.
     */
    void unloaded(int chunkX, int chunkZ) {
        if (surrounding(chunkX, chunkZ, false).isEmpty()) {
            return;
        }
        runSlices(() -> surrounding(chunkX, chunkZ, false), new Seeder() {
            @Override
            public int bands() {
                return 1;
            }

            @Override
            public void seed(Pass pass, LightPropagation block, LightPropagation sky, int band) {
                forNeighbourBorder(chunkX, chunkZ, LightRegion.this.minHeight, LightRegion.this.maxHeight,
                        (x, y, z, intoChunk) -> {
                            invalidateIfHorizontallyLit(pass.blockAccess, block, x, y, z);
                            if (LightRegion.this.skyLight) {
                                invalidateIfHorizontallyLit(pass.skyAccess, sky, x, y, z);
                            }
                        });
            }
        });
    }

    /**
     * Repropagates both channels from the changed packed positions.
     */
    void update(LongCollection positions) {
        if (positions.isEmpty() || neighbourhood(positions).isEmpty()) {
            return;
        }
        runSlices(() -> neighbourhood(positions), new Seeder() {
            @Override
            public int bands() {
                return 1;
            }

            @Override
            public void seed(Pass pass, LightPropagation block, LightPropagation sky, int band) {
                for (long packed : positions) {
                    block.invalidate(pass.blockAccess, packed);
                    if (LightRegion.this.skyLight) {
                        sky.invalidate(pass.skyAccess, packed);
                    }
                }
            }
        });
    }

    /**
     * Seeds in bands over one hold each, then locks, propagates and releases
     * until both channels drain. The scope is re-read before every hold, so
     * unloaded chunks drop out.
     */
    private void runSlices(Supplier<? extends Collection<CloudChunk>> scope, Seeder seeder) {
        LightStateCache states = new LightStateCache(this.registry);
        LightPropagation block = new LightPropagation(LightChannel.BLOCK);
        LightPropagation sky = new LightPropagation(LightChannel.SKY);
        int bands = seeder.bands();
        for (int band = 0; band < bands; band++) {
            LockedChunk[] locked = lockSorted(scope.get());
            long start = System.nanoTime();
            try {
                seeder.seed(new Pass(states, this.minHeight, this.maxHeight, views(locked)), block, sky, band);
            } finally {
                releaseReversed(locked);
                notifyHold(System.nanoTime() - start);
            }
            notifyAfterSeedBand();
        }
        while (true) {
            LockedChunk[] locked = lockSorted(scope.get());
            long start = System.nanoTime();
            boolean blockDrained = false;
            boolean skyDrained = false;
            try {
                Pass pass = new Pass(states, this.minHeight, this.maxHeight, views(locked));
                int[] budget = {this.sliceSteps};
                blockDrained = block.run(pass.blockAccess, budget);
                skyDrained = !this.skyLight || sky.run(pass.skyAccess, budget);
            } finally {
                releaseReversed(locked);
                notifyHold(System.nanoTime() - start);
            }
            notifyAfterSlice();
            if (blockDrained && skyDrained) {
                return;
            }
        }
    }

    private void notifyHold(long holdNanos) {
        if (this.holdNanos != null) {
            this.holdNanos.accept(holdNanos);
        }
    }

    private void notifyAfterSeedBand() {
        Runnable hook = this.afterSeedBand;
        if (hook != null) {
            this.afterSeedBand = null;
            hook.run();
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

    private interface Seeder {
        int bands();

        void seed(Pass pass, LightPropagation block, LightPropagation sky, int band);
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
