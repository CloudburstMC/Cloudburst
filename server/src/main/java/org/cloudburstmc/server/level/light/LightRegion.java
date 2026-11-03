package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.chunk.ChunkLightView;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.*;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Seeds and propagates light in bounded slices over finished terrain.
 * Each slice reacquires its neighborhood in chunk order without waiting for locks.
 * Unloaded chunks are omitted when the next slice resolves its scope.
 * Advance operations serially. Creating an operation does not start it.
 */
public class LightRegion {

    /**
     * Column bands used to initialize one chunk.
     */
    public static final int SEED_BANDS = 16;

    /**
     * Column bands used to seed sky-light propagation.
     */
    public static final int SPREAD_BANDS = 8;

    /**
     * Maximum changed positions seeded per slice.
     */
    public static final int UPDATE_BAND_POSITIONS = 128;

    /**
     * Maximum vertical levels scanned per unload slice.
     */
    public static final int UNLOAD_BAND_HEIGHTS = 32;

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> loadedChunks;
    private final int sliceSteps;
    private final @Nullable HoldRecorder holdRecorder;

    /**
     * @param registry     the block registry
     * @param minHeight    the lowest lit y
     * @param maxHeight    one past the highest lit y
     * @param skyLight     whether to propagate skylight
     * @param loadedChunks thread-safe lookup of finished chunks by packed key, returning null when absent
     * @param sliceSteps   the combined propagation step budget per lock hold, which must be positive
     * @param holdRecorder receives each lock hold's duration with the steps it
     *                     ran (0 for seed holds), or null
     */
    public LightRegion(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight,
                       LongFunction<CloudChunk> loadedChunks, int sliceSteps, @Nullable HoldRecorder holdRecorder) {
        Preconditions.checkArgument(sliceSteps > 0, "sliceSteps must be positive");
        this.registry = registry;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.skyLight = skyLight;
        this.loadedChunks = loadedChunks;
        this.sliceSteps = sliceSteps;
        this.holdRecorder = holdRecorder;
    }

    /**
     * Creates an operation to initialize a finished chunk and propagate into its neighbors.
     *
     * @return an unstarted lighting operation
     */
    public Operation initialize(int chunkX, int chunkZ) {
        return new Operation(new Seeder() {
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
                        SkyLightEngine.initializeBand(pass.skyAccess, chunkX, chunkZ, band, SEED_BANDS);
                    }
                } else {
                    int spread = band - SEED_BANDS;
                    SkyLightEngine.spreadBand(pass.skyAccess, chunkX, chunkZ, spread, SPREAD_BANDS, sky);
                }
            }
        }, () -> surrounding(chunkX, chunkZ, true));
    }

    /**
     * Creates an operation to refresh neighbors after a chunk is removed.
     *
     * @return an unstarted boundary update
     */
    public Operation unloaded(int chunkX, int chunkZ) {
        int bands = unloadBands(this.minHeight, this.maxHeight);

        return new Operation(new Seeder() {
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
     * Creates an operation to update both channels after block changes.
     *
     * @param positions changed positions packed by {@link LightPositions#pack}
     * @return an unstarted light update
     */
    public Operation update(LongCollection positions) {
        long[] all = positions.toLongArray();
        LongSet centers = centerKeys(all, 0, all.length);
        int bands = updateBands(all.length);

        return new Operation(new Seeder() {
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
        }, () -> resolveCenters(centers));
    }

    /**
     * Returns the seed-band count for the given number of changed positions.
     *
     * @param positions the number of changed positions
     * @return the band count, at least 1
     */
    public static int updateBands(int positions) {
        return Math.max(1, (positions + UPDATE_BAND_POSITIONS - 1) / UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns the first position index of an update seed band.
     *
     * @param total the number of changed positions
     * @param band  the band from {@code 0} to {@code updateBands(total) - 1}
     * @return the first index of the band
     */
    public static int updateBandFrom(int total, int band) {
        return Math.min(total, band * UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns one past the last position index of an update seed band.
     *
     * @param total the number of changed positions
     * @param band  the band from {@code 0} to {@code updateBands(total) - 1}
     * @return one past the last index of the band
     */
    public static int updateBandTo(int total, int band) {
        return Math.min(total, (band + 1) * UPDATE_BAND_POSITIONS);
    }

    /**
     * Returns the seed-band count needed to refresh the height range.
     *
     * @param minHeight the lowest lit height
     * @param maxHeight one past the highest lit height
     * @return the band count, at least 1
     */
    public static int unloadBands(int minHeight, int maxHeight) {
        return Math.max(1, (maxHeight - minHeight + UNLOAD_BAND_HEIGHTS - 1) / UNLOAD_BAND_HEIGHTS);
    }

    /**
     * Returns the lowest height of an unload seed band.
     *
     * @param minHeight the lowest lit height
     * @param band      the band from {@code 0} to {@code unloadBands - 1}
     * @return the lowest y of the band
     */
    public static int unloadBandFromY(int minHeight, int band) {
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
    public static int unloadBandToY(int minHeight, int maxHeight, int band) {
        return Math.min(maxHeight, minHeight + (band + 1) * UNLOAD_BAND_HEIGHTS);
    }

    private Set<CloudChunk> neighbourhood(long[] packed, int from, int to) {
        return resolveCenters(centerKeys(packed, from, to));
    }

    private LongSet centerKeys(long[] packed, int from, int to) {
        LongSet keys = new LongOpenHashSet(to - from);

        for (int i = from; i < to; i++) {
            keys.add(CloudChunk.key(LightPositions.x(packed[i]) >> 4, LightPositions.z(packed[i]) >> 4));
        }

        return keys;
    }

    private Set<CloudChunk> resolveCenters(LongSet centers) {
        Set<CloudChunk> neighbourhood = new TreeSet<>();
        LongIterator keys = centers.iterator();

        while (keys.hasNext()) {
            long key = keys.nextLong();
            neighbourhood.addAll(surrounding(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key), true));
        }

        return neighbourhood;
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

    private static @Nullable LockedChunk[] tryLockSorted(Collection<CloudChunk> chunks) {
        List<CloudChunk> sorted = chunks.stream().sorted().toList();
        LockedChunk[] locked = new LockedChunk[sorted.size()];
        int taken = 0;
        boolean success = false;

        try {
            for (CloudChunk chunk : sorted) {
                LockedChunk view = chunk.tryLockForWrite();

                if (view == null) {
                    return null;
                }

                locked[taken++] = view;
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

    private static LongFunction<ChunkLightView> views(LockedChunk[] locked) {
        Long2ObjectOpenHashMap<ChunkLightView> byKey = new Long2ObjectOpenHashMap<>(locked.length);

        for (LockedChunk view : locked) {
            byKey.put(CloudChunk.key(view.getX(), view.getZ()), view);
        }

        return byKey::get;
    }

    /**
     * Re-queues a border cell whose light may have arrived horizontally through
     * the removed neighbor. Light 15 is either an emitter's own light or
     * direct sky from above, so it never depends on a horizontal neighbor
     * and needs no update.
     */
    private static void invalidateIfHorizontallyLit(ChunkLightAccess access, LightPropagation propagation, int x, int y, int z) {
        if (access.isLoaded(x, z)) {
            int light = access.light(x, y, z);
            if (light >= 1 && light <= 14) {
                propagation.invalidate(access, LightPositions.pack(x, y, z));
            }
        }
    }

    private void record(long nanos, int steps) {
        if (this.holdRecorder != null) {
            this.holdRecorder.record(nanos, steps);
        }
    }

    /**
     * Resumable lighting work. Each advance releases its locks before returning.
     */
    public class Operation {

        private final Seeder seeder;
        private final Supplier<? extends Collection<CloudChunk>> scope;
        private final LightStateCache states = new LightStateCache(LightRegion.this.registry);
        private final LightPropagation block = new LightPropagation(LightChannel.BLOCK);
        private final LightPropagation sky = new LightPropagation(LightChannel.SKY);
        private int band;
        private boolean complete;

        private Operation(Seeder seeder, Supplier<? extends Collection<CloudChunk>> scope) {
            this.seeder = seeder;
            this.scope = scope;
        }

        /**
         * Advances one seed band or propagation slice without waiting for chunk locks.
         *
         * @return whether the operation was busy, progressed or completed
         */
        public Step advance() {
            if (this.complete) {
                return Step.COMPLETE;
            }

            boolean seeding = this.band < this.seeder.bands();
            Collection<CloudChunk> chunks = seeding ? this.seeder.seedScope(this.band) : this.scope.get();
            long start = LightRegion.this.holdRecorder == null ? 0 : System.nanoTime();
            LockedChunk[] locked = tryLockSorted(chunks);

            if (locked == null) {
                return Step.BUSY;
            }

            int steps = 0;

            try {
                Pass pass = new Pass(this.states, LightRegion.this.minHeight, LightRegion.this.maxHeight, views(locked));

                if (seeding) {
                    this.seeder.seed(pass, this.block, this.sky, this.band++);
                } else {
                    int[] budget = {LightRegion.this.sliceSteps};
                    boolean blockDrained = this.block.run(pass.blockAccess, budget);
                    boolean skyDrained = !LightRegion.this.skyLight || this.sky.run(pass.skyAccess, budget);
                    steps = LightRegion.this.sliceSteps - budget[0];
                    this.complete = blockDrained && skyDrained;
                }
            } finally {
                releaseReversed(locked);

                if (LightRegion.this.holdRecorder != null) {
                    record(System.nanoTime() - start, steps);
                }
            }

            return this.complete ? Step.COMPLETE : Step.PROGRESSED;
        }
    }

    /**
     * Result of advancing a lighting operation.
     */
    public enum Step {
        /**
         * A chunk lock was unavailable. No work was performed.
         */
        BUSY,
        /**
         * A slice completed and more work remains.
         */
        PROGRESSED,
        /**
         * Both light channels have drained.
         */
        COMPLETE
    }

    /**
     * Receives timing and propagation counts for completed slices.
     */
    @FunctionalInterface
    public interface HoldRecorder {
        /**
         * Records a completed slice after releasing its locks.
         *
         * @param nanos elapsed time for successful lock acquisition, work and release
         * @param steps the propagation steps the hold ran, 0 for seed holds
         */
        void record(long nanos, int steps);
    }

    private interface Seeder {
        int bands();

        Collection<CloudChunk> seedScope(int band);

        void seed(Pass pass, LightPropagation block, LightPropagation sky, int band);
    }

    private record Pass(ChunkLightAccess blockAccess, ChunkLightAccess skyAccess) {
        private Pass(LightStateCache states, int minHeight, int maxHeight, LongFunction<ChunkLightView> views) {
            this(new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views),
                    new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views));
        }
    }
}
