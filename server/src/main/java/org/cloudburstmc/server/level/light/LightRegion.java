package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Runs one light pass over locked chunks: collects the loaded 3x3 around each
 * affected chunk, locks it in sorted {@link CloudChunk} order, runs both
 * channels, and releases in reverse. Locks taken before a failure are released
 * before it propagates.
 *
 * <p>Every operation seeds in bands over one hold each, so no single hold
 * carries a whole-chunk scan or a large edit: initialization in column bands,
 * block changes in bands of {@value #UPDATE_BAND_POSITIONS} positions,
 * unloads in bands of {@value #UNLOAD_BAND_HEIGHTS} heights. Each operation
 * then runs propagation in slices: it locks the 3x3, runs up to
 * {@code sliceSteps} propagation steps combined across both channels,
 * releases, records the hold, and re-locks with a freshly read 3x3 until
 * drained. Chunks that unloaded in between read as absent. One
 * {@link LightStateCache} is shared by the whole operation.
 *
 * <p>A hold waits behind any population or finishing task holding a promoted
 * neighbour in chunk-lock order; the main thread can then briefly wait on a
 * chunk the lane already holds. This cannot deadlock: every path locks in the
 * same sorted {@link CloudChunk} order.
 *
 * <p>{@link LightScheduler} queues background work over it.
 *
 * <p>Each operation covers only the chunk's loaded 3x3 neighbourhood. While
 * some loaded chunks still have their own initialization queued, intermediate
 * light can differ from lighting every chunk at promotion, as the previous
 * engine did; once every loaded chunk's queued work has drained, the results
 * match.
 */
final class LightRegion {

    /**
     * The column bands initialization seeds across, one band per hold.
     */
    static final int SEED_BANDS = 16;

    /**
     * The column bands the sky spread check runs across, one band per hold.
     */
    static final int SPREAD_BANDS = 8;

    /**
     * The changed positions one block-change seed band holds at most.
     */
    static final int UPDATE_BAND_POSITIONS = 128;

    /**
     * The heights one unload seed band covers at most.
     */
    static final int UNLOAD_BAND_HEIGHTS = 32;

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> loadedChunks;
    private final int sliceSteps;
    private final @Nullable LongConsumer holdNanos;
    private final @Nullable IntConsumer holdSteps;
    private volatile @Nullable Runnable afterSeedBand;
    private volatile @Nullable Runnable afterSlice;

    /**
     * @param loadedChunks returns the chunk for a chunk key, or null when absent
     * @param sliceSteps   the combined propagation step budget per lock hold;
     *                     must be positive
     * @param holdNanos    receives each lock hold duration in nanoseconds, or null
     * @param holdSteps    receives the propagation steps each lock hold ran
     *                     (0 for seed holds), or null. Each hold notifies the
     *                     duration before its steps.
     */
    LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
            LongFunction<CloudChunk> loadedChunks, int sliceSteps, @Nullable LongConsumer holdNanos,
            @Nullable IntConsumer holdSteps) {
        Preconditions.checkArgument(sliceSteps > 0, "sliceSteps must be positive");
        this.registry = registry;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.skyLight = skyLight;
        this.loadedChunks = loadedChunks;
        this.sliceSteps = sliceSteps;
        this.holdNanos = holdNanos;
        this.holdSteps = holdSteps;
    }

    /**
     * @param loadedChunks returns the chunk for a chunk key, or null when absent
     * @param sliceSteps   the combined propagation step budget per lock hold;
     *                     must be positive
     * @param holdNanos    receives each lock hold duration in nanoseconds, or null
     */
    LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
            LongFunction<CloudChunk> loadedChunks, int sliceSteps, @Nullable LongConsumer holdNanos) {
        this(registry, minHeight, maxHeight, skyLight, loadedChunks, sliceSteps, holdNanos, null);
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
     * whole-chunk scan. The scope is the chunk's loaded 3x3; light matches
     * lighting every chunk at promotion, as the previous engine did, only once
     * every loaded chunk's queued initialization has drained.
     */
    void initialize(int chunkX, int chunkZ) {
        if (this.loadedChunks.apply(CloudChunk.key(chunkX, chunkZ)) == null) {
            return;
        }
        runSlices(new Seeder() {
            @Override
            public int bands() {
                return SEED_BANDS + (LightRegion.this.skyLight ? SPREAD_BANDS : 0);
            }

            @Override
            public Collection<CloudChunk> seedScope(int band) {
                return surrounding(chunkX, chunkZ, true);
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
        }, () -> surrounding(chunkX, chunkZ, true));
    }

    /**
     * Refreshes the ring around an unloaded chunk.
     *
     * <p>Seeding runs in bands of {@value #UNLOAD_BAND_HEIGHTS} heights over
     * one hold each, so one unload cannot hold the ring's write locks while
     * scanning the whole column.
     */
    void unloaded(int chunkX, int chunkZ) {
        if (surrounding(chunkX, chunkZ, false).isEmpty()) {
            return;
        }
        int bands = unloadBands(this.minHeight, this.maxHeight);
        runSlices(new Seeder() {
            @Override
            public int bands() {
                return bands;
            }

            @Override
            public Collection<CloudChunk> seedScope(int band) {
                return surrounding(chunkX, chunkZ, false);
            }

            @Override
            public void seed(Pass pass, LightPropagation block, LightPropagation sky, int band) {
                int fromY = unloadBandFromY(LightRegion.this.minHeight, band);
                int toY = unloadBandToY(LightRegion.this.minHeight, LightRegion.this.maxHeight, band);
                LightPositions.forNeighbourBorder(chunkX, chunkZ, fromY, toY,
                        (x, y, z, intoChunk) -> {
                            invalidateIfHorizontallyLit(pass.blockAccess, block, x, y, z);
                            if (LightRegion.this.skyLight) {
                                invalidateIfHorizontallyLit(pass.skyAccess, sky, x, y, z);
                            }
                        });
            }
        }, () -> surrounding(chunkX, chunkZ, false));
    }

    /**
     * Repropagates both channels from the changed packed positions.
     *
     * <p>Seeding runs in bands of {@value #UPDATE_BAND_POSITIONS} positions
     * over one hold each, each locking only its own band's neighbourhood, so a
     * large edit cannot hold the write locks while invalidating every change
     * at once.
     */
    void update(LongCollection positions) {
        if (positions.isEmpty() || neighbourhood(positions).isEmpty()) {
            return;
        }
        long[] all = positions.toLongArray();
        int bands = updateBands(all.length);
        runSlices(new Seeder() {
            @Override
            public int bands() {
                return bands;
            }

            @Override
            public Collection<CloudChunk> seedScope(int band) {
                return neighbourhood(all, updateBandFrom(all.length, band), updateBandTo(all.length, band));
            }

            @Override
            public void seed(Pass pass, LightPropagation block, LightPropagation sky, int band) {
                int from = updateBandFrom(all.length, band);
                int to = updateBandTo(all.length, band);
                for (int i = from; i < to; i++) {
                    block.invalidate(pass.blockAccess, all[i]);
                    if (LightRegion.this.skyLight) {
                        sky.invalidate(pass.skyAccess, all[i]);
                    }
                }
            }
        }, () -> neighbourhood(all, 0, all.length));
    }

    /**
     * Returns the seed bands an update of {@code positions} changed positions
     * needs.
     *
     * @param positions the number of changed positions
     * @return the band count, at least 1
     */
    static int updateBands(int positions) {
        return Math.max(1, (positions + UPDATE_BAND_POSITIONS - 1) / UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns the first position index of an update seed band.
     *
     * @param total the number of changed positions
     * @param band  the band from {@code 0} to {@code updateBands(total) - 1}
     * @return the first index of the band
     */
    static int updateBandFrom(int total, int band) {
        return Math.min(total, band * UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns one past the last position index of an update seed band.
     *
     * @param total the number of changed positions
     * @param band  the band from {@code 0} to {@code updateBands(total) - 1}
     * @return one past the last index of the band
     */
    static int updateBandTo(int total, int band) {
        return Math.min(total, (band + 1) * UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns the seed bands an unload refresh of the height range needs.
     *
     * @param minHeight the lowest lit height
     * @param maxHeight one past the highest lit height
     * @return the band count, at least 1
     */
    static int unloadBands(int minHeight, int maxHeight) {
        return Math.max(1, (maxHeight - minHeight + UNLOAD_BAND_HEIGHTS - 1) / UNLOAD_BAND_HEIGHTS);
    }

    /**
     * Returns the lowest height of an unload seed band.
     *
     * @param minHeight the lowest lit height
     * @param band      the band from {@code 0} to {@code unloadBands - 1}
     * @return the lowest y of the band
     */
    static int unloadBandFromY(int minHeight, int band) {
        return minHeight + band * UNLOAD_BAND_HEIGHTS;
    }

    /**
     * Returns one past the highest height of an unload seed band.
     *
     * @param minHeight the lowest lit height
     * @param maxHeight one past the highest lit height
     * @param band      the band from {@code 0} to {@code unloadBands - 1}
     * @return one past the highest y of the band
     */
    static int unloadBandToY(int minHeight, int maxHeight, int band) {
        return Math.min(maxHeight, minHeight + (band + 1) * UNLOAD_BAND_HEIGHTS);
    }

    /**
     * Seeds in bands over one hold each, then locks, propagates and releases
     * until both channels drain. Scopes are re-read before every hold, so
     * unloaded chunks drop out.
     */
    private void runSlices(Seeder seeder, Supplier<? extends Collection<CloudChunk>> scope) {
        LightStateCache states = new LightStateCache(this.registry);
        LightPropagation block = new LightPropagation(LightChannel.BLOCK);
        LightPropagation sky = new LightPropagation(LightChannel.SKY);
        int bands = seeder.bands();
        for (int band = 0; band < bands; band++) {
            LockedChunk[] locked = lockSorted(seeder.seedScope(band));
            long start = System.nanoTime();
            try {
                seeder.seed(new Pass(states, this.minHeight, this.maxHeight, views(locked)), block, sky, band);
            } finally {
                releaseReversed(locked);
                notifyHold(System.nanoTime() - start);
                notifySteps(0);
            }
            notifyAfterSeedBand();
        }
        while (true) {
            LockedChunk[] locked = lockSorted(scope.get());
            long start = System.nanoTime();
            boolean blockDrained = false;
            boolean skyDrained = false;
            int steps = 0;
            try {
                Pass pass = new Pass(states, this.minHeight, this.maxHeight, views(locked));
                int[] budget = {this.sliceSteps};
                blockDrained = block.run(pass.blockAccess, budget);
                skyDrained = !this.skyLight || sky.run(pass.skyAccess, budget);
                steps = this.sliceSteps - budget[0];
            } finally {
                releaseReversed(locked);
                notifyHold(System.nanoTime() - start);
                notifySteps(steps);
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

    private void notifySteps(int steps) {
        if (this.holdSteps != null) {
            this.holdSteps.accept(steps);
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
        return neighbourhood(positions.toLongArray(), 0, positions.size());
    }

    private Set<CloudChunk> neighbourhood(long[] packed, int from, int to) {
        Set<Long> keys = new LongOpenHashSet(to - from);
        for (int i = from; i < to; i++) {
            keys.add(CloudChunk.key(LightPositions.x(packed[i]) >> 4, LightPositions.z(packed[i]) >> 4));
        }
        Set<CloudChunk> neighbourhood = new TreeSet<>();
        for (long key : keys) {
            neighbourhood.addAll(surrounding(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key), true));
        }
        return neighbourhood;
    }

    private interface Seeder {
        int bands();

        Collection<CloudChunk> seedScope(int band);

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
