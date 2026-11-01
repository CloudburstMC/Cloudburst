package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.IceBlockHandlers;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

import static org.junit.jupiter.api.Assertions.*;

class BlockLightEngineTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i SOURCE = Vector3i.from(0, 2, 0);
    private static final LightStateCache STATES = new LightStateCache(REGISTRY);

    @Test
    void complementarySlabFacesSealACorridorAndShapeChangesRelightIt() {
        Grid grid = new Grid();
        grid.minX = 0;
        grid.maxX = 4;
        grid.minZ = 0;
        grid.maxZ = 0;
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y < 5; y++) {
                grid.blocks.put(Vector3i.from(x, y, 0), BlockTypes.STONE.getDefaultState());
            }
        }
        BlockState bottom = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState top = bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP);
        Vector3i first = SOURCE.add(1, 0, 0);
        Vector3i second = SOURCE.add(2, 0, 0);
        Vector3i beyond = SOURCE.add(3, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(first, bottom);
        grid.blocks.put(second, top);
        grid.blocks.put(beyond, BlockStates.AIR);

        update(grid, List.of(SOURCE));

        assertEquals(14, grid.light(first));
        assertEquals(0, grid.light(second));
        assertEquals(0, grid.light(beyond));

        grid.blocks.put(second, bottom);
        update(grid, List.of(second));

        assertEquals(13, grid.light(second));
        assertEquals(12, grid.light(beyond));

        grid.blocks.put(second, top);
        update(grid, List.of(second));

        assertEquals(0, grid.light(second));
        assertEquals(0, grid.light(beyond));
    }

    @Test
    void initializationFindsBothLayerSourcesAndReplacesStaleLight() {
        Grid grid = new Grid();
        grid.minX = 0;
        grid.maxX = 15;
        grid.minZ = 0;
        grid.maxZ = 15;
        Vector3i primary = Vector3i.from(3, 2, 3);
        Vector3i secondary = Vector3i.from(10, 2, 10);
        Vector3i stale = Vector3i.from(8, 2, 5);
        grid.blocks.put(primary, BlockStates.LAVA);
        grid.secondary.put(secondary, BlockTypes.SEA_LANTERN.getDefaultState());
        grid.lights.put(stale, 15);

        initialize(grid, 0, 0);

        assertEquals(15, grid.light(primary));
        assertEquals(15, grid.light(secondary));
        assertEquals(14, grid.light(primary.add(1, 0, 0)));
        assertEquals(8, grid.light(stale));
    }

    @Test
    void newlyLoadedBordersReceiveLightAndUnloadedSourcesStopContributing() {
        Grid grid = new Grid();
        grid.minX = 0;
        grid.maxX = 15;
        grid.minZ = 0;
        grid.maxZ = 15;
        Vector3i source = Vector3i.from(15, 2, 8);
        Vector3i neighbor = source.add(1, 0, 0);
        grid.blocks.put(source, BlockStates.LAVA);
        initialize(grid, 0, 0);
        assertFalse(grid.lights.containsKey(neighbor));

        grid.maxX = 31;
        initialize(grid, 1, 0);

        assertEquals(14, grid.light(neighbor));
        assertEquals(10, grid.light(neighbor.add(4, 0, 0)));

        grid.minX = 16;
        update(grid, List.of(source));

        assertEquals(0, grid.light(neighbor));
        assertEquals(0, grid.light(neighbor.add(4, 0, 0)));
    }

    @Test
    void opaqueLavaEmitsLightAndLightsIceEnoughToMelt() {
        Grid grid = grid();
        Vector3i ice = SOURCE.add(1, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(ice, BlockTypes.ICE.getDefaultState());

        update(grid, List.of(SOURCE));

        int attenuation = Math.max(1, grid.blocks.get(ice).getLightDampening());
        assertEquals(15, grid.light(SOURCE));
        assertEquals(15 - attenuation, grid.light(ice));
        assertEquals(14, grid.light(SOURCE.add(-1, 0, 0)));

        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ice, grid.blocks.get(ice));
        IceBlockHandlers.randomTick(world.block(ice), grid.light(ice), false);
        assertSame(BlockStates.WATER, world.state(ice));
    }

    @Test
    void removingOneSourcePreservesLightFromAnotherAndRemovingBothClearsIt() {
        Grid grid = grid();
        Vector3i second = SOURCE.add(4, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(second, BlockTypes.TORCH.getDefaultState());
        update(grid, List.of(SOURCE, second));

        grid.blocks.remove(SOURCE);
        update(grid, List.of(SOURCE));
        assertEquals(14, grid.light(second));
        assertEquals(10, grid.light(SOURCE));

        grid.blocks.remove(second);
        update(grid, List.of(second));
        assertTrue(grid.lights.values().stream().allMatch(light -> light == 0));
    }

    @Test
    void opacityChangesRecalculateIncomingLightAndSecondaryLayersCanEmit() {
        Grid grid = grid();
        grid.secondary.put(SOURCE, BlockTypes.GLOWSTONE.getDefaultState());
        Vector3i target = SOURCE.add(1, 0, 0);
        update(grid, List.of(SOURCE));
        assertEquals(15, grid.light(SOURCE));
        assertEquals(14, grid.light(target));

        grid.blocks.put(target, BlockStates.STONE);
        update(grid, List.of(target));
        assertEquals(0, grid.light(target));

        grid.blocks.remove(target);
        update(grid, List.of(target));
        assertEquals(14, grid.light(target));
    }

    @Test
    void levelAdapterUsesAbsoluteNegativeCoordinatesAndOnlyLoadedChunks() {
        Level level = TestLevels.level(0, 16);
        CloudChunk chunk = new CloudChunk(-2, 1, level);
        Vector3i source = Vector3i.from(-17, 2, 17);
        try (LockedChunk locked = chunk.lockForWrite()) {
            locked.setBlockState(15, 2, 1, BlockLayer.PRIMARY, BlockStates.LAVA);
            LongFunction<LockedChunk> views = key -> key == CloudChunk.key(-2, 1) ? locked : null;
            ChunkLightAccess access = new ChunkLightAccess(LightChannel.BLOCK, 0, 16, STATES, views);
            LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
            propagation.invalidate(access, LightPositions.pack(source.getX(), source.getY(), source.getZ()));
            assertTrue(propagation.run(access, Integer.MAX_VALUE));

            assertEquals(15, access.light(source.getX(), source.getY(), source.getZ()));
            assertEquals(14, access.light(source.getX() - 1, source.getY(), source.getZ()));
            assertEquals(0, access.light(source.getX() + 1, source.getY(), source.getZ()));
            assertFalse(access.isLoaded(15, 1));
        }
    }

    private static void initialize(Grid grid, int chunkX, int chunkZ) {
        LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
        BlockLightEngine.initialize(grid, chunkX, chunkZ, propagation);
        assertTrue(propagation.run(grid, Integer.MAX_VALUE));
    }

    private static void update(Grid grid, Collection<Vector3i> changed) {
        LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
        for (Vector3i position : changed) {
            propagation.invalidate(grid,
                    LightPositions.pack(position.getX(), position.getY(), position.getZ()));
        }
        assertTrue(propagation.run(grid, Integer.MAX_VALUE));
    }

    private static Grid grid() {
        return new Grid();
    }

    private static class Grid implements LightAccess {
        private final Map<Vector3i, BlockState> blocks = new HashMap<>();
        private final Map<Vector3i, BlockState> secondary = new HashMap<>();
        private final Map<Vector3i, Integer> lights = new HashMap<>();
        private int minX = -8;
        private int maxX = 8;
        private int minZ = -8;
        private int maxZ = 8;

        @Override
        public int minHeight() {
            return 0;
        }

        @Override
        public int maxHeight() {
            return 5;
        }

        @Override
        public boolean isLoaded(int x, int z) {
            return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
        }

        @Override
        public BlockState primary(int x, int y, int z) {
            if (!this.isLoaded(x, z) || y < 0 || y > 4) {
                return BlockStates.AIR;
            }
            return this.blocks.getOrDefault(Vector3i.from(x, y, z), BlockStates.AIR);
        }

        @Override
        public BlockState secondary(int x, int y, int z) {
            if (!this.isLoaded(x, z) || y < 0 || y > 4) {
                return BlockStates.AIR;
            }
            return this.secondary.getOrDefault(Vector3i.from(x, y, z), BlockStates.AIR);
        }

        @Override
        public int light(int x, int y, int z) {
            return this.lights.getOrDefault(Vector3i.from(x, y, z), 0);
        }

        @Override
        public void setLight(int x, int y, int z, int light) {
            if (!this.isLoaded(x, z) || y < 0 || y > 4) {
                return;
            }
            this.lights.put(Vector3i.from(x, y, z), light);
        }

        @Override
        public LightStateCache states() {
            return STATES;
        }

        private int light(Vector3i position) {
            return this.light(position.getX(), position.getY(), position.getZ());
        }
    }
}
